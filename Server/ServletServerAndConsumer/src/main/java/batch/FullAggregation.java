package batch;

import cache.RedisCacheClient;
import com.google.gson.Gson;
import grpc.LiftRideReadProto;
import model.Configuration;
import utils.ConfigUtils;

import java.sql.*;
import java.util.*;

// TODO set batch size
public class FullAggregation implements AggregationStrategy {
    private final Configuration config;
    private final RedisCacheClient cache;
    private final Connection conn;
    private final Gson gson = new Gson();

    public FullAggregation(Configuration config, RedisCacheClient cache) throws SQLException {
        this.config = config;
        this.cache = cache;
        this.conn = DriverManager.getConnection(config.MYSQL_READ_URL, config.MYSQL_USERNAME, config.MYSQL_PASSWORD);
    }

    @Override
    public void run() throws SQLException {
        aggregateUniqueSkiers(conn);
        aggregateDailyVerticals(conn);
        aggregateSeasonVerticals(conn);
        aggregateAllSeasonVerticals(conn);
    }

    @Override
    public void close() {
        try {
            conn.close();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private void aggregateUniqueSkiers(Connection conn) throws SQLException {
        try (PreparedStatement stmt =
                     conn.prepareStatement(config.AGGREGATION_FULL_UNIQUE_SKIERS_SQL)) {
            String tmpBloom = config.REDIS_BLOOM_FILTER_UNIQUE_SKIERS + ":full";
            cache.createFilter(tmpBloom, config.REDIS_BLOOM_FILTER_CAPACITY, config.REDIS_BLOOM_FILTER_ERROR_RATE);

            ResultSet rs = stmt.executeQuery();
            Map<String, String> updateMap = new HashMap<>();
            List<String> bloomKeys = new ArrayList<>();

            while (rs.next()) {
                int resortId = rs.getInt("resort_id");
                String seasonId = rs.getString("season_id");
                int dayId = rs.getInt("day_id");
                int count = rs.getInt("count");
                String key = cache.getUniqueSkierCountKey(resortId, seasonId, dayId);
                // Accumulate for normal cache update.
                updateMap.put(key, String.valueOf(count));
                // Accumulate for Bloom filter batch add.
                bloomKeys.add(key);
            }

            // Batch update cache keys.
            if (!updateMap.isEmpty()) {
                cache.getSync().mset(updateMap);
            }
            // Batch add keys to the temporary Bloom filter.
            if (!bloomKeys.isEmpty()) {
                cache.addBatch(tmpBloom, bloomKeys);
            }

            cache.rename(tmpBloom, config.REDIS_BLOOM_FILTER_UNIQUE_SKIERS);
        }
    }

    private void aggregateDailyVerticals(Connection conn) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(config.AGGREGATION_FULL_DAILY_VERTICAL_SQL)) {
            String tmpBloom = config.REDIS_BLOOM_FILTER_DAILY_VERTICAL + ":full";
            cache.createFilter(tmpBloom, config.REDIS_BLOOM_FILTER_CAPACITY, config.REDIS_BLOOM_FILTER_ERROR_RATE);
            ResultSet rs = stmt.executeQuery();

            Map<String, String> updateMap = new HashMap<>();
            List<String> bloomKeys = new ArrayList<>();

            while (rs.next()) {
                int resortId = rs.getInt("resort_id");
                String seasonId = rs.getString("season_id");
                int dayId = rs.getInt("day_id");
                int skierId = rs.getInt("skier_id");
                int vertical = rs.getInt("total_vertical");
                String key = cache.getSkierDayVerticalKey(resortId, seasonId, dayId, skierId);
                updateMap.put(key, String.valueOf(vertical));
                bloomKeys.add(key);
            }

            if (!updateMap.isEmpty()) {
                cache.getSync().mset(updateMap);
            }
            if (!bloomKeys.isEmpty()) {
                cache.addBatch(tmpBloom, bloomKeys);
            }
            cache.rename(tmpBloom, config.REDIS_BLOOM_FILTER_DAILY_VERTICAL);
        }
    }

    // TODO combine this and aggregateAllSeasonVerticals
    private void aggregateSeasonVerticals(Connection conn) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(config.AGGREGATION_FULL_SEASON_VERTICAL_SQL)) {
            String tmpBloom = config.REDIS_BLOOM_FILTER_SINGLE_SEASON_VERTICAL + ":full";
            cache.createFilter(tmpBloom, config.REDIS_BLOOM_FILTER_CAPACITY, config.REDIS_BLOOM_FILTER_ERROR_RATE);
            ResultSet rs = stmt.executeQuery();
            Map<String, String> updateMap = new HashMap<>();
            List<String> bloomKeys = new ArrayList<>();

            while (rs.next()) {
                int skierId = rs.getInt("skier_id");
                int resortId = rs.getInt("resort_id");
                String seasonId = rs.getString("season_id");
                int vertical = rs.getInt("total_vertical");
                String key = cache.getSingleSeasonVerticalKey(skierId, resortId, seasonId);
                updateMap.put(key, String.valueOf(vertical));
                bloomKeys.add(key);
            }

            if (!updateMap.isEmpty()) {
                cache.getSync().mset(updateMap);
            }
            if (!bloomKeys.isEmpty()) {
                cache.addBatch(tmpBloom, bloomKeys);
            }
            cache.rename(tmpBloom, config.REDIS_BLOOM_FILTER_SINGLE_SEASON_VERTICAL);
        }
    }

    private void aggregateAllSeasonVerticals(Connection conn) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(config.AGGREGATION_FULL_SEASON_VERTICAL_SQL)) {
            ResultSet rs = stmt.executeQuery();
            String tmpBloom = config.REDIS_BLOOM_FILTER_ALL_SEASON_VERTICALS + ":full";
            cache.createFilter(tmpBloom, config.REDIS_BLOOM_FILTER_CAPACITY, config.REDIS_BLOOM_FILTER_ERROR_RATE);
            // Map to accumulate values per key.
            Map<String, List<LiftRideReadProto.VerticalRecord>> aggregateMap = new HashMap<>();
            // Use a list to collect bloom filter keys; ensure each key is added only once.
            List<String> bloomKeys = new ArrayList<>();

            while (rs.next()) {
                int skierId = rs.getInt("skier_id");
                int resortId = rs.getInt("resort_id");
                String seasonId = rs.getString("season_id");
                int vertical = rs.getInt("total_vertical");
                String key = cache.getAllSeasonsVerticalKey(skierId, resortId);
                if (!aggregateMap.containsKey(key)) {
                    bloomKeys.add(key);
                    aggregateMap.put(key, new ArrayList<>());
                }
                aggregateMap.get(key).add(
                        LiftRideReadProto.VerticalRecord.newBuilder()
                                .setSeasonID(seasonId)
                                .setTotalVertical(vertical)
                                .build()
                );
            }

            // Create a map of key to JSON representation.
            if (!aggregateMap.isEmpty()) {
                Map<String, String> updateMap = new HashMap<>();
                for (Map.Entry<String, List<LiftRideReadProto.VerticalRecord>> entry : aggregateMap.entrySet()) {
                    String json = gson.toJson(entry.getValue());
                    updateMap.put(entry.getKey(), json);
                }
                cache.getSync().mset(updateMap);
            }
            if (!bloomKeys.isEmpty()) {
                cache.addBatch(tmpBloom, bloomKeys);
            }
            cache.rename(tmpBloom, config.REDIS_BLOOM_FILTER_ALL_SEASON_VERTICALS);
        }
    }

    public static void main(String[] args) throws Exception {
        Configuration config = ConfigUtils.getConfigurationForService();
        RedisCacheClient cacheClient = new RedisCacheClient(config);

        AggregationStrategy full = new FullAggregation(config, cacheClient);

        long startTime = System.currentTimeMillis();
        full.run();
        long endTime = System.currentTimeMillis();
        System.out.println("[FullAggregation] update time: " + (endTime - startTime) + " ms");
    }
}
