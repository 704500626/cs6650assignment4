package old;

import cache.RedisKeyFormatter;
import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnels;
import model.Configuration;

import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.concurrent.atomic.AtomicLong;

public class BloomFilterLocal {
    private final Configuration config;
    private final BloomFilter<String> uniqueSkiersFilter;
    private final BloomFilter<String> dailyVerticalFilter;
    private final BloomFilter<String> seasonVerticalFilter;
    private final BloomFilter<String> totalVerticalFilter;

    public BloomFilterLocal(Configuration config) throws SQLException {
        this.config = config;

        // Step 1: Count the expected size (or use config)
//        long expectedInsertions = estimateSizeFromDB();

        // Step 2: Initialize the Bloom filter
        uniqueSkiersFilter = BloomFilter.create(Funnels.stringFunnel(StandardCharsets.UTF_8), config.BLOOM_FILTER_CAPACITY, config.BLOOM_FILTER_ERROR_RATE);

        dailyVerticalFilter = BloomFilter.create(Funnels.stringFunnel(StandardCharsets.UTF_8), config.BLOOM_FILTER_CAPACITY, config.BLOOM_FILTER_ERROR_RATE);

        seasonVerticalFilter = BloomFilter.create(Funnels.stringFunnel(StandardCharsets.UTF_8), config.BLOOM_FILTER_CAPACITY, config.BLOOM_FILTER_ERROR_RATE);

        totalVerticalFilter = BloomFilter.create(Funnels.stringFunnel(StandardCharsets.UTF_8), config.BLOOM_FILTER_CAPACITY, config.BLOOM_FILTER_ERROR_RATE);

        Connection conn = DriverManager.getConnection(config.MYSQL_READ_URL, config.MYSQL_USERNAME, config.MYSQL_PASSWORD);

        try (PreparedStatement stmt = conn.prepareStatement(config.AGGREGATION_BLOOM_FILTER_KEY_UNIQUE_SKIERS_SQL); ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                String key = RedisKeyFormatter.format(config.REDIS_KEY_PATTERN_UNIQUE_SKIERS, rs.getInt(1), rs.getString(2), rs.getInt(3));
                uniqueSkiersFilter.put(key);
            }
        }

        // Daily Vertical
        try (PreparedStatement stmt = conn.prepareStatement(config.AGGREGATION_BLOOM_FILTER_KEY_DAILY_VERTICAL_SQL); ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                String key = RedisKeyFormatter.format(config.REDIS_KEY_PATTERN_DAILY_VERTICAL, rs.getInt(1), rs.getString(2), rs.getInt(3), rs.getInt(4));
                dailyVerticalFilter.put(key);
            }
        }

        // Season Vertical
        try (PreparedStatement stmt = conn.prepareStatement(config.AGGREGATION_BLOOM_FILTER_KEY_SINGLE_SEASON_VERTICAL_SQL); ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                String key = RedisKeyFormatter.format(config.REDIS_KEY_PATTERN_SINGLE_SEASON_VERTICAL, rs.getInt(1), rs.getInt(2), rs.getString(3));
                seasonVerticalFilter.put(key);
            }
        }

        // All Season Vertical
        try (PreparedStatement stmt = conn.prepareStatement(config.AGGREGATION_BLOOM_FILTER_KEY_ALL_SEASON_VERTICAL_SQL); ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                String key = RedisKeyFormatter.format(config.REDIS_KEY_PATTERN_ALL_SEASON_VERTICALS, rs.getInt(1), rs.getInt(2));
                totalVerticalFilter.put(key);
            }
        }

        try {
            conn.close();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        // Step 3: Load keys into filter
//        loadUniqueSkierKeysFromDB();
    }

    public BloomFilter<String> getDailyVerticalFilter() {
        return dailyVerticalFilter;
    }

    public BloomFilter<String> getSeasonVerticalFilter() {
        return seasonVerticalFilter;
    }

    public BloomFilter<String> getTotalVerticalFilter() {
        return totalVerticalFilter;
    }

    private void loadUniqueSkierKeysFromDB() throws SQLException {
        try (Connection conn = DriverManager.getConnection(config.MYSQL_READ_URL, config.MYSQL_USERNAME, config.MYSQL_PASSWORD); PreparedStatement stmt = conn.prepareStatement(config.AGGREGATION_BLOOM_FILTER_KEY_UNIQUE_SKIERS_SQL); ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                String key = RedisKeyFormatter.format(config.REDIS_KEY_PATTERN_UNIQUE_SKIERS, rs.getInt(1), rs.getString(2), rs.getInt(3));
                uniqueSkiersFilter.put(key);
            }
        }
    }

    public BloomFilter<String> getUniqueSkiersFilter() {
        return uniqueSkiersFilter;
    }

    private long estimateSizeFromDB() {
        // Default estimate
        long fallback = config.BLOOM_FILTER_CAPACITY;
        try (Connection conn = DriverManager.getConnection(config.MYSQL_READ_URL, config.MYSQL_USERNAME, config.MYSQL_PASSWORD); PreparedStatement stmt = conn.prepareStatement(config.AGGREGATION_BLOOM_FILTER_KEY_UNIQUE_SKIERS_SQL); ResultSet rs = stmt.executeQuery()) {
            AtomicLong count = new AtomicLong(0);
            while (rs.next()) {
                count.incrementAndGet();
            }
            return count.get() == 0 ? fallback : count.get();
        } catch (SQLException e) {
            e.printStackTrace();
            return fallback;
        }
    }
}
