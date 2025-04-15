package readservice;

import cache.RedisCacheClient;
import dao.LiftRideReader;
import grpc.LiftRideReadProto.*;
import grpc.SkierReadServiceGrpc;
import io.grpc.stub.StreamObserver;
import model.Configuration;

import java.util.List;

public class LiftRideReadServiceImpl extends SkierReadServiceGrpc.SkierReadServiceImplBase {
    private final LiftRideReader dbReader;
    private final RedisCacheClient cache;
    private final Configuration config;

    public LiftRideReadServiceImpl(Configuration config) {
        this.config = config;
        this.dbReader = new LiftRideReader(config);
        this.cache = new RedisCacheClient(config);
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
                if (config.REDIS_BLOOM_FILTER_SWITCH && !cache.exists(config.REDIS_BLOOM_FILTER_SINGLE_SEASON_VERTICAL, itemKey)) {
                    System.out.printf("[getTotalVertical-season] Bloom filter negative: key=%s -> skipping DB%n", itemKey);
                    responseObserver.onNext(VerticalListResponse.newBuilder().build()); // empty list
                    responseObserver.onCompleted();
                    return;
                }

                Integer cached = cache.getSingleSeasonVertical(itemKey);
                if (cached != null) {
                    System.out.printf("[getTotalVertical-season] Cache hit: key=%s, value=%d%n", itemKey, cached);
                    responseObserver.onNext(VerticalListResponse.newBuilder().addRecords(VerticalRecord.newBuilder().setSeasonID(seasonId).setTotalVertical(cached).build()).build());
                    responseObserver.onCompleted();
                    return;
                }
                System.out.printf("[getTotalVertical-season] Cache miss: key=%s%n", itemKey);

                List<VerticalRecord> results = dbReader.getSkierResortTotals(skierId, resortId, seasonId);
                System.out.printf("[getTotalVertical-season] Fetched from DB: key=%s, results size=%d%n", itemKey, results.size());
                if (!results.isEmpty()) {
                    cache.setSingleSeasonVertical(itemKey, results.get(0).getTotalVertical());
                } else {
                    cache.setSingleSeasonVertical(itemKey, 0);
                }
                responseObserver.onNext(VerticalListResponse.newBuilder().addAllRecords(results).build());
                responseObserver.onCompleted();
            } else {
                String itemKey = cache.getAllSeasonsVerticalKey(skierId, resortId);
                if (config.REDIS_BLOOM_FILTER_SWITCH && !cache.exists(config.REDIS_BLOOM_FILTER_ALL_SEASON_VERTICALS, itemKey)) {
                    System.out.printf("[getTotalVertical-all] Bloom filter negative: key=%s -> skipping DB%n", itemKey);
                    responseObserver.onNext(VerticalListResponse.newBuilder().build());
                    responseObserver.onCompleted();
                    return;
                }

                List<VerticalRecord> cached = cache.getAllSeasonVerticals(itemKey);
                if (cached != null) {
                    System.out.printf("[getTotalVertical-all] Cache hit: key=%s, size=%d%n", itemKey, cached.size());
                    responseObserver.onNext(VerticalListResponse.newBuilder().addAllRecords(cached).build());
                    responseObserver.onCompleted();
                    return;
                }
                System.out.printf("[getTotalVertical-all] Cache miss: key=%s%n", itemKey);

                List<VerticalRecord> results = dbReader.getSkierResortTotals(skierId, resortId, "");
                System.out.printf("[getTotalVertical-all] Fetched from DB: key=%s, results size=%d%n", itemKey, results.size());
                cache.setAllSeasonVerticals(itemKey, results);
                responseObserver.onNext(VerticalListResponse.newBuilder().addAllRecords(results).build());
                responseObserver.onCompleted();
            }
        } catch (Exception e) {
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
            if (config.REDIS_BLOOM_FILTER_SWITCH && !cache.exists(config.REDIS_BLOOM_FILTER_DAILY_VERTICAL, itemKey)) {
                System.out.printf("[getDailyVertical] Bloom filter negative: key=%s -> skipping DB%n", itemKey);
                responseObserver.onNext(VerticalIntResponse.newBuilder().setTotalVertical(0).build());
                responseObserver.onCompleted();
                return;
            }

            Integer cached = cache.getSkierDayVertical(itemKey);
            if (cached != null) {
                System.out.printf("[getDailyVertical] Cache hit: key=%s, value=%d%n", itemKey, cached);
                responseObserver.onNext(VerticalIntResponse.newBuilder().setTotalVertical(cached).build());
                responseObserver.onCompleted();
                return;
            }
            System.out.printf("[getDailyVertical] Cache miss: key=%s%n", itemKey);

            int vertical = dbReader.getSkierDayVertical(resortId, seasonId, dayId, skierId);
            System.out.printf("[getDailyVertical] Fetched from DB: key=%s, value=%d%n", itemKey, vertical);
            cache.setSkierDayVertical(itemKey, vertical);
            responseObserver.onNext(VerticalIntResponse.newBuilder().setTotalVertical(vertical).build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(e);
        }
    }

    @Override
    public void getResortDaySkiers(ResortDayRequest request, StreamObserver<SkierCountResponse> responseObserver) {
        int resortId = request.getResortID();
        String seasonId = request.getSeasonID();
        int dayId = request.getDayID();
        String itemKey = cache.getUniqueSkierCountKey(resortId, seasonId, dayId);

        try {
            if (config.REDIS_BLOOM_FILTER_SWITCH && !cache.exists(config.REDIS_BLOOM_FILTER_UNIQUE_SKIERS, itemKey)) {
                System.out.printf("[getUniqueSkiers] Bloom filter negative: key=%s -> skipping DB%n", itemKey);
                responseObserver.onNext(SkierCountResponse.newBuilder().setSkierCount(0).build());
                responseObserver.onCompleted();
                return;
            }

            Integer cached = cache.getUniqueSkierCount(itemKey);
            if (cached != null) {
                System.out.printf("[getUniqueSkiers] Cache hit: key=%s, value=%d%n", itemKey, cached);
                responseObserver.onNext(SkierCountResponse.newBuilder().setSkierCount(cached).build());
                responseObserver.onCompleted();
                return;
            }
            System.out.printf("[getUniqueSkiers] Cache miss: key=%s%n", itemKey);

            int count = dbReader.getResortUniqueSkiers(resortId, seasonId, dayId);
            System.out.printf("[getUniqueSkiers] Fetched from DB: key=%s, value=%d%n", itemKey, count);
            cache.setUniqueSkierCount(itemKey, count);
            responseObserver.onNext(SkierCountResponse.newBuilder().setSkierCount(count).build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(e);
        }
    }

    public void close() {
        dbReader.close();
        cache.close();
    }
}