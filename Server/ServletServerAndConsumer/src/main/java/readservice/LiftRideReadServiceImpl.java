package readservice;

import bloomfilter.BloomFilterUtils;
import bloomfilter.LiftRideBloomFilter;
import cache.CacheWriterWorker;
import cache.RedisCacheClient;
import com.google.gson.Gson;
import dao.LiftRideReader;
import grpc.BatchAggregationServiceGrpc;
import grpc.BatchAggregationServiceProto.*;
import grpc.LiftRideReadProto.*;
import grpc.SkierReadServiceGrpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import model.CacheWrite;
import model.Configuration;

import java.util.List;
import java.util.concurrent.*;

public class LiftRideReadServiceImpl extends SkierReadServiceGrpc.SkierReadServiceImplBase {
    private final LiftRideReader dbReader;
    private final RedisCacheClient cache;
    private final Configuration config;
    private final BlockingQueue<CacheWrite> cacheQueue;
    private final Thread cacheWriterThread;
    private final Gson gson = new Gson();
    private volatile LiftRideBloomFilter bloomFilter;
    private final ScheduledExecutorService refresher = Executors.newSingleThreadScheduledExecutor();
    private ManagedChannel batchServiceChannel;

    public LiftRideReadServiceImpl(Configuration config) {
        this.config = config;
        this.dbReader = new LiftRideReader(config);
        this.cache = new RedisCacheClient(config);
        this.cacheQueue = new LinkedBlockingQueue<>(config.LIFTRIDE_READ_SERVICE_CACHE_QUEUE_SIZE);
        this.cacheWriterThread = new Thread(new CacheWriterWorker(cache, cacheQueue,
                config.LIFTRIDE_READ_SERVICE_CACHE_BATCH_SIZE,
                config.LIFTRIDE_READ_SERVICE_CACHE_FLUSH_INTERVAL_MS));
        this.cacheWriterThread.start();
        if (config.BLOOM_FILTER_SWITCH) {
            setupBloomFilterScheduledUpdate(config);
        }
    }

    private void setupBloomFilterScheduledUpdate(Configuration config) {
        try {
            batchServiceChannel = ManagedChannelBuilder.forAddress(
                    config.AGGREGATION_SERVICE_HOST, config.AGGREGATION_SERVICE_PORT
            ).usePlaintext().build();

            BatchAggregationServiceGrpc.BatchAggregationServiceBlockingStub stub =
                    BatchAggregationServiceGrpc.newBlockingStub(batchServiceChannel);

            refresher.scheduleAtFixedRate(() -> {
                try {
                    BloomFilterSnapshot snapshot = stub.getBloomFilterSnapshot(Empty.newBuilder().build());
                    bloomFilter = new LiftRideBloomFilter(
                            BloomFilterUtils.decompressAndDeserialize(snapshot.getUniqueSkiersFilter().toByteArray()),
                            BloomFilterUtils.decompressAndDeserialize(snapshot.getDailyVerticalFilter().toByteArray()),
                            BloomFilterUtils.decompressAndDeserialize(snapshot.getSeasonVerticalFilter().toByteArray()),
                            BloomFilterUtils.decompressAndDeserialize(snapshot.getTotalVerticalFilter().toByteArray()));
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
            if (config.BLOOM_FILTER_SWITCH && bloomFilter != null && !bloomFilter.getUniqueSkiersFilter().mightContain(itemKey)) {
//                System.out.printf("[getUniqueSkiers] Bloom filter negative: key=%s -> skipping DB%n", itemKey);
                responseObserver.onNext(SkierCountResponse.newBuilder().setSkierCount(0).build());
                responseObserver.onCompleted();
                return;
            }

            Integer cached = cache.getUniqueSkierCount(itemKey);
            if (cached != null) {
//                System.out.printf("[getUniqueSkiers] Cache hit: key=%s, value=%d%n", itemKey, cached);
                responseObserver.onNext(SkierCountResponse.newBuilder().setSkierCount(cached).build());
                responseObserver.onCompleted();
                return;
            }
//            System.out.printf("[getUniqueSkiers] Cache miss: key=%s%n", itemKey);

            int count = dbReader.getResortUniqueSkiers(resortId, seasonId, dayId);
//            System.out.printf("[getUniqueSkiers] Fetched from DB: key=%s, value=%d%n", itemKey, count);
            cacheQueue.offer(new CacheWrite(itemKey, String.valueOf(count)));
            responseObserver.onNext(SkierCountResponse.newBuilder().setSkierCount(count).build());
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
            if (config.BLOOM_FILTER_SWITCH && bloomFilter != null && !bloomFilter.getDailyVerticalFilter().mightContain(itemKey)) {
//                System.out.printf("[getDailyVertical] Bloom filter negative: key=%s -> skipping DB%n", itemKey);
                responseObserver.onNext(VerticalIntResponse.newBuilder().setTotalVertical(0).build());
                responseObserver.onCompleted();
                return;
            }

            Integer cached = cache.getSkierDayVertical(itemKey);
            if (cached != null) {
//                System.out.printf("[getDailyVertical] Cache hit: key=%s, value=%d%n", itemKey, cached);
                responseObserver.onNext(VerticalIntResponse.newBuilder().setTotalVertical(cached).build());
                responseObserver.onCompleted();
                return;
            }
//            System.out.printf("[getDailyVertical] Cache miss: key=%s%n", itemKey);

            int vertical = dbReader.getSkierDayVertical(resortId, seasonId, dayId, skierId);
//            System.out.printf("[getDailyVertical] Fetched from DB: key=%s, value=%d%n", itemKey, vertical);
            cacheQueue.offer(new CacheWrite(itemKey, String.valueOf(vertical)));
            responseObserver.onNext(VerticalIntResponse.newBuilder().setTotalVertical(vertical).build());
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
            if (withSeason) {
                String itemKey = cache.getSingleSeasonVerticalKey(skierId, resortId, seasonId);
                if (config.BLOOM_FILTER_SWITCH && bloomFilter != null && !bloomFilter.getSeasonVerticalFilter().mightContain(itemKey)) {
//                    System.out.printf("[getTotalVertical-season] Bloom filter negative: key=%s -> skipping DB%n", itemKey);
                    responseObserver.onNext(VerticalListResponse.newBuilder().build()); // empty list
                    responseObserver.onCompleted();
                    return;
                }

                Integer cached = cache.getSingleSeasonVertical(itemKey);
                if (cached != null) {
//                    System.out.printf("[getTotalVertical-season] Cache hit: key=%s, value=%d%n", itemKey, cached);
                    responseObserver.onNext(VerticalListResponse.newBuilder().addRecords(VerticalRecord.newBuilder().setSeasonID(seasonId).setTotalVertical(cached).build()).build());
                    responseObserver.onCompleted();
                    return;
                }
//                System.out.printf("[getTotalVertical-season] Cache miss: key=%s%n", itemKey);

                List<VerticalRecord> results = dbReader.getSkierResortTotals(skierId, resortId, seasonId);
//                System.out.printf("[getTotalVertical-season] Fetched from DB: key=%s, results size=%d%n", itemKey, results.size());
                if (!results.isEmpty()) {
                    cacheQueue.offer(new CacheWrite(itemKey, String.valueOf(results.get(0).getTotalVertical())));
                } else {
                    cacheQueue.offer(new CacheWrite(itemKey, String.valueOf(0)));
                }
                responseObserver.onNext(VerticalListResponse.newBuilder().addAllRecords(results).build());
                responseObserver.onCompleted();
            } else {
                String itemKey = cache.getAllSeasonsVerticalKey(skierId, resortId);
                if (config.BLOOM_FILTER_SWITCH && bloomFilter != null && !bloomFilter.getTotalVerticalFilter().mightContain(itemKey)) {
//                    System.out.printf("[getTotalVertical-all] Bloom filter negative: key=%s -> skipping DB%n", itemKey);
                    responseObserver.onNext(VerticalListResponse.newBuilder().build());
                    responseObserver.onCompleted();
                    return;
                }

                List<VerticalRecord> cached = cache.getAllSeasonVerticals(itemKey);
                if (cached != null) {
//                    System.out.printf("[getTotalVertical-all] Cache hit: key=%s, size=%d%n", itemKey, cached.size());
                    responseObserver.onNext(VerticalListResponse.newBuilder().addAllRecords(cached).build());
                    responseObserver.onCompleted();
                    return;
                }
//                System.out.printf("[getTotalVertical-all] Cache miss: key=%s%n", itemKey);

                List<VerticalRecord> results = dbReader.getSkierResortTotals(skierId, resortId, "");
//                System.out.printf("[getTotalVertical-all] Fetched from DB: key=%s, results size=%d%n", itemKey, results.size());
                cacheQueue.offer(new CacheWrite(itemKey, gson.toJson(results)));
                responseObserver.onNext(VerticalListResponse.newBuilder().addAllRecords(results).build());
                responseObserver.onCompleted();
            }
        } catch (Exception e) {
            e.printStackTrace();
            responseObserver.onError(e);
        }
    }

    public void close() {
        dbReader.close();
        cache.close();
        if (batchServiceChannel != null) {
            try {
                batchServiceChannel.shutdown();
                batchServiceChannel.awaitTermination(30, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        refresher.shutdown();
        try {
            refresher.awaitTermination(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
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