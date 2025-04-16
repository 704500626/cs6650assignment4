package readservice;

import bloomfilter.LiftRideBloomFilter;
import cache.LocalLRUCache;
import cache.RedisCacheClient;
import dao.LiftRideReader;
import grpc.LiftRideReadProto;
import io.grpc.stub.StreamObserver;
import model.CacheHitLevel;
import model.CacheWrite;
import model.Configuration;

import java.sql.SQLException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

public class SkierDayRidesQuery {
    private final Configuration config;
    private final LiftRideReader dbReader;
    private final RedisCacheClient cache;
    private final BlockingQueue<CacheWrite> cacheQueue;
    private final LiftRideBloomFilter bloomFilter;
    private final LocalLRUCache<String, String> localLRU;
    private final AtomicInteger cacheWriteFailure = new AtomicInteger(0);

    public SkierDayRidesQuery(Configuration config, LiftRideReader dbReader, RedisCacheClient cache, BlockingQueue<CacheWrite> cacheQueue, LiftRideBloomFilter bloomFilter, LocalLRUCache<String, String> localLRU) {
        this.config = config;
        this.dbReader = dbReader;
        this.cache = cache;
        this.cacheQueue = cacheQueue;
        this.bloomFilter = bloomFilter;
        this.localLRU = localLRU;
    }

    public CacheHitLevel querySkierDayRides(StreamObserver<LiftRideReadProto.VerticalIntResponse> responseObserver, String itemKey, int resortId, String seasonId, int dayId, int skierId) throws SQLException {
        if (config.BLOOM_FILTER_SWITCH && bloomFilter != null && !bloomFilter.getDailyVerticalFilter().mightContain(itemKey)) {
            System.out.printf("[getSkierDayRides] Bloom filter negative: key=%s -> skipping DB%n", itemKey);
            responseObserver.onNext(LiftRideReadProto.VerticalIntResponse.newBuilder().setTotalVertical(0).build());
            return CacheHitLevel.BLOOM_NEGATIVE;
        }

        if (config.LIFTRIDE_READ_SERVICE_LRU_SWITCH) {
            String cached = localLRU.get(itemKey);
            if (cached != null) {
                System.out.printf("[getSkierDayRides] LRU Cache hit: key=%s, value=%s%n", itemKey, cached);
                responseObserver.onNext(LiftRideReadProto.VerticalIntResponse.newBuilder().setTotalVertical(Integer.parseInt(cached)).build());
                return CacheHitLevel.LRU_HIT;
            }
            System.out.printf("[getSkierDayRides] LRU Cache miss: key=%s%n", itemKey);
        }

        Integer cached = cache.getSkierDayVertical(itemKey);
        if (cached != null) {
            System.out.printf("[getSkierDayRides] Cache hit: key=%s, value=%d%n", itemKey, cached);
            responseObserver.onNext(LiftRideReadProto.VerticalIntResponse.newBuilder().setTotalVertical(cached).build());
            return CacheHitLevel.REDIS_HIT;
        }
        System.out.printf("[getSkierDayRides] Cache miss: key=%s%n", itemKey);

        int vertical = dbReader.getSkierDayVertical(resortId, seasonId, dayId, skierId);
        String value = String.valueOf(vertical);
        System.out.printf("[getSkierDayRides] Fetched from DB: key=%s, value=%d%n", itemKey, vertical);
        if (config.LIFTRIDE_READ_SERVICE_LRU_SWITCH) localLRU.put(itemKey, value);
        if (!cacheQueue.offer(new CacheWrite(itemKey, value))) {
            cacheWriteFailure.incrementAndGet();
            System.err.printf("[CacheWriteQueue] Dropped key=%s due to full queue, cache write is not fast enough%n", itemKey);
        }

        responseObserver.onNext(LiftRideReadProto.VerticalIntResponse.newBuilder().setTotalVertical(vertical).build());
        return CacheHitLevel.DB_HIT;
    }

    public AtomicInteger getCacheWriteFailure() {
        return cacheWriteFailure;
    }
}
