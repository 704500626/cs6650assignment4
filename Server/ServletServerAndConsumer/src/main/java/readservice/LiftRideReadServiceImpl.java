package readservice;

import bloomfilter.BloomFilterUtils;
import bloomfilter.LiftRideBloomFilter;
import cache.CacheWriterWorker;
import cache.LocalLRUCache;
import cache.RedisCacheClient;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import dao.LiftRideReader;
import grpc.BatchAggregationServiceGrpc;
import grpc.BatchAggregationServiceProto.BloomFilterSnapshot;
import grpc.BatchAggregationServiceProto.Empty;
import grpc.LiftRideReadProto.*;
import grpc.SkierReadServiceGrpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import model.CacheHitLevel;
import model.CacheWrite;
import model.Configuration;

import java.lang.reflect.Type;
import java.sql.SQLException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class LiftRideReadServiceImpl extends SkierReadServiceGrpc.SkierReadServiceImplBase {
    private final LiftRideReader dbReader;
    private final RedisCacheClient cache;
    private final Configuration config;
    private final BlockingQueue<CacheWrite> cacheQueue;
    private final Thread cacheWriterThread;

    private final SkierDayRidesQuery skierDayRidesQuery;
    private final ResortDaySkiersQuery resortDaySkiersQuery;
    private final TotalVerticalQuery totalVerticalQuery;

    private LocalLRUCache<String, String> localLRU; // optional
    private ScheduledExecutorService lruRefresher;
    private volatile LiftRideBloomFilter bloomFilter;  // optional
    private ScheduledExecutorService bloomRefresher;
    private ManagedChannel batchServiceChannel;
    private Map<CacheHitLevel, AtomicInteger> cacheStats; // optional
    private ScheduledExecutorService metricsCollector;
    private final AtomicInteger requestCounter = new AtomicInteger(0);

    public LiftRideReadServiceImpl(Configuration config) {
        this.config = config;
        this.dbReader = new LiftRideReader(config);
        this.cache = new RedisCacheClient(config);
        this.cacheQueue = new LinkedBlockingQueue<>(config.LIFTRIDE_READ_SERVICE_CACHE_QUEUE_SIZE);
        this.cacheWriterThread = new Thread(new CacheWriterWorker(cache, cacheQueue,
                config.LIFTRIDE_READ_SERVICE_CACHE_BATCH_SIZE,
                config.LIFTRIDE_READ_SERVICE_CACHE_FLUSH_INTERVAL_MS));
        this.cacheWriterThread.start();

        if (config.LIFTRIDE_READ_SERVICE_LRU_SWITCH) {
            setupLocalLRU(config);
        }

        if (config.LIFTRIDE_READ_SERVICE_COLLECT_METRICS) {
            setupMetricsCollector();
        }

        if (config.BLOOM_FILTER_SWITCH) {
            setupBloomFilterScheduledUpdate(config);
        }

        skierDayRidesQuery = new SkierDayRidesQuery(config, dbReader, cache, cacheQueue, bloomFilter, localLRU);
        resortDaySkiersQuery = new ResortDaySkiersQuery(config, dbReader, cache, cacheQueue, bloomFilter, localLRU);
        totalVerticalQuery = new TotalVerticalQuery(config, dbReader, cache, cacheQueue, bloomFilter, localLRU);
    }

    private void setupMetricsCollector() {
        cacheStats = new ConcurrentHashMap<>();
        for (CacheHitLevel level : CacheHitLevel.values()) {
            cacheStats.put(level, new AtomicInteger());
        }
        metricsCollector = Executors.newSingleThreadScheduledExecutor();
        metricsCollector.scheduleAtFixedRate(() -> {
            try {
                System.out.println("==== Cache Stats ====");
                System.out.println("NUM_REQUESTS: " + requestCounter.get());
                for (var entry : cacheStats.entrySet()) {
                    System.out.printf("%s: %d%n", entry.getKey(), entry.getValue().get());
                }
                System.out.println("CACHE_WRITE_FAILURE: " + (skierDayRidesQuery.getCacheWriteFailure().get() + resortDaySkiersQuery.getCacheWriteFailure().get() + totalVerticalQuery.getCacheWriteFailure().get()));
                System.out.println("=====================");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, config.LIFTRIDE_READ_SERVICE_METRICS_OUTPUT_INTERVAL_SEC, config.LIFTRIDE_READ_SERVICE_METRICS_OUTPUT_INTERVAL_SEC, TimeUnit.SECONDS);
    }

    private void setupLocalLRU(Configuration config) {
        lruRefresher = Executors.newSingleThreadScheduledExecutor();
        localLRU = new LocalLRUCache<>(config.LIFTRIDE_READ_SERVICE_LRU_CAPACITY);

        List<String> hotKeyPatterns = List.of(
                config.AGGREGATION_HOT_KEY_UNIQUE_SKIERS,
                config.AGGREGATION_HOT_KEY_DAILY_VERTICAL,
                config.AGGREGATION_HOT_KEY_SINGLE_SEASON_VERTICAL,
                config.AGGREGATION_HOT_KEY_ALL_SEASON_VERTICAL
        );

        int perPatternLimit = config.LIFTRIDE_READ_SERVICE_LRU_CAPACITY / hotKeyPatterns.size();

        for (String pattern : hotKeyPatterns) {
            try {
                Collection<String> keys = cache.scanKeys(pattern, perPatternLimit);
                for (String key : keys) {
                    String value = cache.getSync().get(key);
                    if (value != null) {
                        localLRU.put(key, value);
                    }
                }
                System.out.printf("[LRU Prewarm] Loaded %d keys for pattern: %s%n", keys.size(), pattern);
            } catch (Exception e) {
                System.err.printf("[LRU Prewarm] Failed for pattern: %s - %s%n", pattern, e.getMessage());
            }
        }

        lruRefresher.scheduleAtFixedRate(() -> {
            try {
                for (String key : localLRU.keySetSnapshot()) {
                    String value = cache.getSync().get(key);
                    if (value != null) {
                        localLRU.put(key, value);
                    } else {
                        localLRU.remove(key); // evict dead Redis keys
                    }
                }
                System.out.println("[LRU Refresh] Successfully updated from Redis.");
            } catch (Exception e) {
                System.err.println("[LRU Refresh] Failed: " + e.getMessage());
                e.printStackTrace();
            }
        }, 0, config.LIFTRIDE_READ_SERVICE_LRU_REFRESH_INTERVAL_SEC, TimeUnit.SECONDS);
    }

    private void setupBloomFilterScheduledUpdate(Configuration config) {
        try {
            bloomFilter = new LiftRideBloomFilter(config);
            bloomRefresher = Executors.newSingleThreadScheduledExecutor();

            batchServiceChannel = ManagedChannelBuilder.forAddress(
                    config.AGGREGATION_SERVICE_HOST, config.AGGREGATION_SERVICE_PORT
            ).usePlaintext().build();

            BatchAggregationServiceGrpc.BatchAggregationServiceBlockingStub stub =
                    BatchAggregationServiceGrpc.newBlockingStub(batchServiceChannel);

            bloomRefresher.scheduleAtFixedRate(() -> {
                try {
                    BloomFilterSnapshot snapshot = stub.getBloomFilterSnapshot(Empty.newBuilder().build());
                    bloomFilter.setUniqueSkiersFilter(BloomFilterUtils.decompressAndDeserialize(snapshot.getUniqueSkiersFilter().toByteArray()));
                    bloomFilter.setDailyVerticalFilter(BloomFilterUtils.decompressAndDeserialize(snapshot.getDailyVerticalFilter().toByteArray()));
                    bloomFilter.setSeasonVerticalFilter(BloomFilterUtils.decompressAndDeserialize(snapshot.getSeasonVerticalFilter().toByteArray()));
                    bloomFilter.setTotalVerticalFilter(BloomFilterUtils.decompressAndDeserialize(snapshot.getTotalVerticalFilter().toByteArray()));
                    System.out.println("[BloomFilter Refresh] Successfully updated from BatchAggregationService.");
                } catch (Exception e) {
                    System.err.println("[BloomFilter Refresh] Failed: " + e.getMessage());
                    e.printStackTrace();
                }
            }, 0, config.AGGREGATION_BLOOM_ONLY_INTERVAL_SEC, TimeUnit.SECONDS);
        } catch (Exception e) {
            e.printStackTrace();
            if (batchServiceChannel != null) {
                try {
                    batchServiceChannel.shutdown();
                    batchServiceChannel.awaitTermination(30, TimeUnit.SECONDS);
                } catch (InterruptedException ie) {
                    throw new RuntimeException(ie);
                }
            }
        }
    }

    @Override
    public void getResortDaySkiers(ResortDayRequest request, StreamObserver<SkierCountResponse> responseObserver) {
        int resortId = request.getResortID();
        String seasonId = request.getSeasonID();
        int dayId = request.getDayID();
        String itemKey = cache.getUniqueSkierCountKey(resortId, seasonId, dayId);

        try {
            CacheHitLevel cacheHitLevel = resortDaySkiersQuery.queryResortDaySkiers(responseObserver, itemKey, resortId, seasonId, dayId);
            if (config.LIFTRIDE_READ_SERVICE_COLLECT_METRICS) {
                requestCounter.incrementAndGet();
                cacheStats.get(cacheHitLevel).incrementAndGet();
            }
            responseObserver.onCompleted();
        } catch (Exception e) {
            e.printStackTrace();
            responseObserver.onError(e);
        }
    }

    @Override
    public void getSkierDayRides(SkierDayRequest request, StreamObserver<VerticalIntResponse> responseObserver) {
        int resortId = request.getResortID();
        String seasonId = request.getSeasonID();
        int dayId = request.getDayID();
        int skierId = request.getSkierID();
        String itemKey = cache.getSkierDayVerticalKey(resortId, seasonId, dayId, skierId);

        try {
            CacheHitLevel cacheHitLevel = skierDayRidesQuery.querySkierDayRides(responseObserver, itemKey, resortId, seasonId, dayId, skierId);
            if (config.LIFTRIDE_READ_SERVICE_COLLECT_METRICS) {
                requestCounter.incrementAndGet();
                cacheStats.get(cacheHitLevel).incrementAndGet();
            }
            responseObserver.onCompleted();
        } catch (Exception e) {
            e.printStackTrace();
            responseObserver.onError(e);
        }
    }

    @Override
    public void getTotalVertical(VerticalRequest request, StreamObserver<VerticalListResponse> responseObserver) {
        int skierId = request.getSkierID();
        int resortId = request.getResortID();
        String seasonId = request.getSeasonID();  // could be empty
        boolean withSeason = seasonId != null && !seasonId.isEmpty();
        try {
            CacheHitLevel cacheHitLevel;
            if (withSeason) {
                cacheHitLevel = totalVerticalQuery.queryTotalVerticalBySeason(responseObserver, skierId, resortId, seasonId);
            } else {
                cacheHitLevel = totalVerticalQuery.queryTotalVerticalAllSeason(responseObserver, skierId, resortId);
            }
            if (config.LIFTRIDE_READ_SERVICE_COLLECT_METRICS) {
                requestCounter.incrementAndGet();
                cacheStats.get(cacheHitLevel).incrementAndGet();
            }
            responseObserver.onCompleted();
        } catch (Exception e) {
            e.printStackTrace();
            responseObserver.onError(e);
        }
    }

    public void close() {
        dbReader.close();
        cache.close();
        if (config.LIFTRIDE_READ_SERVICE_LRU_SWITCH) {
            try {
                lruRefresher.shutdown();
                lruRefresher.awaitTermination(10, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        if (config.LIFTRIDE_READ_SERVICE_COLLECT_METRICS) {
            try {
                metricsCollector.shutdown();
                metricsCollector.awaitTermination(10, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        if (config.BLOOM_FILTER_SWITCH) {
            try {
                batchServiceChannel.shutdown();
                batchServiceChannel.awaitTermination(10, TimeUnit.SECONDS);
                bloomRefresher.shutdown();
                bloomRefresher.awaitTermination(10, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        if (cacheWriterThread != null && cacheWriterThread.isAlive()) {
            cacheWriterThread.interrupt();
            try {
                cacheWriterThread.join(2000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }
}