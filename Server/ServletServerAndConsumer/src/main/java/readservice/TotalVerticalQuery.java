package readservice;

import bloomfilter.LiftRideBloomFilter;
import cache.LocalLRUCache;
import cache.RedisCacheClient;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import dao.LiftRideReader;
import grpc.LiftRideReadProto;
import io.grpc.stub.StreamObserver;
import model.CacheHitLevel;
import model.CacheWrite;
import model.Configuration;

import java.lang.reflect.Type;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

public class TotalVerticalQuery {
    private final Configuration config;
    private final LiftRideReader dbReader;
    private final RedisCacheClient cache;
    private final BlockingQueue<CacheWrite> cacheQueue;
    private final LiftRideBloomFilter bloomFilter;
    private final LocalLRUCache<String, String> localLRU;
    private final Gson gson = new Gson();
    private final AtomicInteger cacheWriteFailure = new AtomicInteger(0);

    public TotalVerticalQuery(Configuration config, LiftRideReader dbReader, RedisCacheClient cache, BlockingQueue<CacheWrite> cacheQueue, LiftRideBloomFilter bloomFilter, LocalLRUCache<String, String> localLRU) {
        this.config = config;
        this.dbReader = dbReader;
        this.cache = cache;
        this.cacheQueue = cacheQueue;
        this.bloomFilter = bloomFilter;
        this.localLRU = localLRU;
    }

    public CacheHitLevel queryTotalVerticalBySeason(StreamObserver<LiftRideReadProto.VerticalListResponse> responseObserver, int skierId, int resortId, String seasonId) throws SQLException {
        String itemKey = cache.getSingleSeasonVerticalKey(skierId, resortId, seasonId);

        if (config.BLOOM_FILTER_SWITCH && bloomFilter != null && !bloomFilter.getSeasonVerticalFilter().mightContain(itemKey)) {
            System.out.printf("[getTotalVertical-season] Bloom filter negative: key=%s -> skipping DB%n", itemKey);
            responseObserver.onNext(LiftRideReadProto.VerticalListResponse.newBuilder().build()); // empty list
            return CacheHitLevel.BLOOM_NEGATIVE;
        }

        if (config.LIFTRIDE_READ_SERVICE_LRU_SWITCH) {
            String cached = localLRU.get(itemKey);
            if (cached != null) {
                System.out.printf("[getTotalVertical-season] LRU Cache hit: key=%s, value=%s%n", itemKey, cached);
                responseObserver.onNext(LiftRideReadProto.VerticalListResponse.newBuilder().addRecords(LiftRideReadProto.VerticalRecord.newBuilder().setSeasonID(seasonId).setTotalVertical(Integer.parseInt(cached)).build()).build());
                return CacheHitLevel.LRU_HIT;
            }
            System.out.printf("[getTotalVertical-season] LRU Cache miss: key=%s%n", itemKey);
        }

        Integer cached = cache.getSingleSeasonVertical(itemKey);
        if (cached != null) {
            System.out.printf("[getTotalVertical-season] Cache hit: key=%s, value=%d%n", itemKey, cached);
            responseObserver.onNext(LiftRideReadProto.VerticalListResponse.newBuilder().addRecords(LiftRideReadProto.VerticalRecord.newBuilder().setSeasonID(seasonId).setTotalVertical(cached).build()).build());
            return CacheHitLevel.REDIS_HIT;
        }
        System.out.printf("[getTotalVertical-season] Cache miss: key=%s%n", itemKey);

        List<LiftRideReadProto.VerticalRecord> results = dbReader.getSkierResortTotals(skierId, resortId, seasonId);
        System.out.printf("[getTotalVertical-season] Fetched from DB: key=%s, results size=%d%n", itemKey, results.size());
        String value;
        if (!results.isEmpty()) {
            value = String.valueOf(results.get(0).getTotalVertical());
        } else {
            value = String.valueOf(0);
        }
        if (config.LIFTRIDE_READ_SERVICE_LRU_SWITCH) localLRU.put(itemKey, value);
        if (!cacheQueue.offer(new CacheWrite(itemKey, value))) {
            cacheWriteFailure.incrementAndGet();
            System.err.printf("[CacheWriteQueue] Dropped key=%s due to full queue, cache write is not fast enough%n", itemKey);
        }

        responseObserver.onNext(LiftRideReadProto.VerticalListResponse.newBuilder().addAllRecords(results).build());
        return CacheHitLevel.DB_HIT;
    }

    public CacheHitLevel queryTotalVerticalAllSeason(StreamObserver<LiftRideReadProto.VerticalListResponse> responseObserver, int skierId, int resortId) throws SQLException {
        String itemKey = cache.getAllSeasonsVerticalKey(skierId, resortId);
        if (config.BLOOM_FILTER_SWITCH && bloomFilter != null && !bloomFilter.getTotalVerticalFilter().mightContain(itemKey)) {
            System.out.printf("[getTotalVertical-all] Bloom filter negative: key=%s -> skipping DB%n", itemKey);
            responseObserver.onNext(LiftRideReadProto.VerticalListResponse.newBuilder().build());
            return CacheHitLevel.BLOOM_NEGATIVE;
        }

        if (config.LIFTRIDE_READ_SERVICE_LRU_SWITCH) {
            String cached = localLRU.get(itemKey);
            if (cached != null) {
                Type listType = new TypeToken<List<LiftRideReadProto.VerticalRecord>>() {
                }.getType();
                List<LiftRideReadProto.VerticalRecord> value = gson.fromJson(cached, listType);
                System.out.printf("[getTotalVertical-all] LRU Cache hit: key=%s, value=%s%n", itemKey, cached);
                responseObserver.onNext(LiftRideReadProto.VerticalListResponse.newBuilder().addAllRecords(value).build());
                return CacheHitLevel.LRU_HIT;
            }
            System.out.printf("[getTotalVertical-all] LRU Cache miss: key=%s%n", itemKey);
        }

        List<LiftRideReadProto.VerticalRecord> cached = cache.getAllSeasonVerticals(itemKey);
        if (cached != null) {
            System.out.printf("[getTotalVertical-all] Cache hit: key=%s, size=%d%n", itemKey, cached.size());
            responseObserver.onNext(LiftRideReadProto.VerticalListResponse.newBuilder().addAllRecords(cached).build());
            return CacheHitLevel.REDIS_HIT;
        }
        System.out.printf("[getTotalVertical-all] Cache miss: key=%s%n", itemKey);

        List<LiftRideReadProto.VerticalRecord> results = dbReader.getSkierResortTotals(skierId, resortId, "");
        String value = gson.toJson(results);
        System.out.printf("[getTotalVertical-all] Fetched from DB: key=%s, results size=%d%n", itemKey, results.size());
        if (config.LIFTRIDE_READ_SERVICE_LRU_SWITCH) localLRU.put(itemKey, value);
        if (!cacheQueue.offer(new CacheWrite(itemKey, value))) {
            cacheWriteFailure.incrementAndGet();
            System.err.printf("[CacheWriteQueue] Dropped key=%s due to full queue, cache write is not fast enough%n", itemKey);
        }

        responseObserver.onNext(LiftRideReadProto.VerticalListResponse.newBuilder().addAllRecords(results).build());
        return CacheHitLevel.DB_HIT;
    }

    public AtomicInteger getCacheWriteFailure() {
        return cacheWriteFailure;
    }
}
