package old;

import batch.AggregationStrategy;
import cache.RedisCacheClient;
import model.Configuration;
import utils.ConfigUtils;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

// TODO set batch size
public class BloomOnlyAggregationRedis implements AggregationStrategy {
    private final Configuration config;
    private final RedisCacheClient cache;
    private final Connection conn;

    public BloomOnlyAggregationRedis(Configuration config, RedisCacheClient cache) throws SQLException {
        this.config = config;
        this.cache = cache;
        this.conn = DriverManager.getConnection(config.MYSQL_READ_URL, config.MYSQL_USERNAME, config.MYSQL_PASSWORD);
    }

    @Override
    public void run() throws SQLException {
        System.out.println("[BloomOnlyAggregation] Rebuilding Bloom filters...");

        // Temporary keys
        String bf1Temp = config.REDIS_BLOOM_FILTER_UNIQUE_SKIERS + ":bloom_only";
        String bf2Temp = config.REDIS_BLOOM_FILTER_DAILY_VERTICAL + ":bloom_only";
        String bf3Temp = config.REDIS_BLOOM_FILTER_SINGLE_SEASON_VERTICAL + ":bloom_only";
        String bf4Temp = config.REDIS_BLOOM_FILTER_ALL_SEASON_VERTICALS + ":bloom_only";

        cache.createFilter(bf1Temp, config.BLOOM_FILTER_CAPACITY, config.BLOOM_FILTER_ERROR_RATE);
        cache.createFilter(bf2Temp, config.BLOOM_FILTER_CAPACITY, config.BLOOM_FILTER_ERROR_RATE);
        cache.createFilter(bf3Temp, config.BLOOM_FILTER_CAPACITY, config.BLOOM_FILTER_ERROR_RATE);
        cache.createFilter(bf4Temp, config.BLOOM_FILTER_CAPACITY, config.BLOOM_FILTER_ERROR_RATE);

        List<String> buffer = new ArrayList<>(5000);
        // Unique Skiers
        try (PreparedStatement stmt = conn.prepareStatement(config.AGGREGATION_BLOOM_FILTER_KEY_UNIQUE_SKIERS_SQL); ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                String key = cache.getUniqueSkierCountKey(rs.getInt(1), rs.getString(2), rs.getInt(3));
                buffer.add(key);
            }
            if (!buffer.isEmpty()) {
                cache.addBatch(bf1Temp, buffer);
            }
        }

        // Daily Vertical
        try (PreparedStatement stmt = conn.prepareStatement(config.AGGREGATION_BLOOM_FILTER_KEY_DAILY_VERTICAL_SQL); ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                String key = cache.getSkierDayVerticalKey(rs.getInt(1), rs.getString(2), rs.getInt(3), rs.getInt(4));
                buffer.add(key);
            }
            if (!buffer.isEmpty()) {
                cache.addBatch(bf2Temp, buffer);
                buffer.clear();
            }
        }

        // Season Vertical
        try (PreparedStatement stmt = conn.prepareStatement(config.AGGREGATION_BLOOM_FILTER_KEY_SINGLE_SEASON_VERTICAL_SQL); ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                String key = cache.getSingleSeasonVerticalKey(rs.getInt(1), rs.getInt(2), rs.getString(3));
                buffer.add(key);
            }
            if (!buffer.isEmpty()) {
                cache.addBatch(bf3Temp, buffer);
                buffer.clear();
            }
        }

        // All Season Vertical
        try (PreparedStatement stmt = conn.prepareStatement(config.AGGREGATION_BLOOM_FILTER_KEY_ALL_SEASON_VERTICAL_SQL); ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                String key = cache.getAllSeasonsVerticalKey(rs.getInt(1), rs.getInt(2));
                buffer.add(key);
            }
            if (!buffer.isEmpty()) {
                cache.addBatch(bf4Temp, buffer);
                buffer.clear();
            }
        }

        // Hot-swap after success
        cache.rename(bf1Temp, config.REDIS_BLOOM_FILTER_UNIQUE_SKIERS);
        cache.rename(bf2Temp, config.REDIS_BLOOM_FILTER_DAILY_VERTICAL);
        cache.rename(bf3Temp, config.REDIS_BLOOM_FILTER_SINGLE_SEASON_VERTICAL);
        cache.rename(bf4Temp, config.REDIS_BLOOM_FILTER_ALL_SEASON_VERTICALS);
        System.out.println("[BloomOnlyAggregation] Bloom filters updated successfully.");
    }

    @Override
    public void close() {
        try {
            conn.close();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) throws Exception {
        Configuration config = ConfigUtils.getConfigurationForService();
        RedisCacheClient cacheClient = new RedisCacheClient(config);

        AggregationStrategy bloomOnly = new BloomOnlyAggregationRedis(config, cacheClient);

        long startTime = System.currentTimeMillis();
        bloomOnly.run();
        long endTime = System.currentTimeMillis();
        System.out.println("[BloomOnlyAggregation] update time: " + (endTime - startTime) + " ms");
    }
}