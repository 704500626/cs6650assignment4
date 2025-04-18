package batch;

import bloomfilter.LiftRideBloomFilter;
import cache.RedisKeyFormatter;
import model.Configuration;
import utils.ConfigUtils;

import java.sql.*;

public class BloomOnlyAggregation implements AggregationStrategy {
    private final Configuration config;
    private final Connection conn;
    private final LiftRideBloomFilter liftRideBloomFilter;

    public BloomOnlyAggregation(Configuration config, LiftRideBloomFilter liftRideBloomFilter) throws SQLException {
        this.config = config;
        this.liftRideBloomFilter = liftRideBloomFilter;
        this.conn = DriverManager.getConnection(config.MYSQL_READ_URL, config.MYSQL_USERNAME, config.MYSQL_PASSWORD);
    }

    @Override
    public void run() throws SQLException {
        System.out.println("[BloomOnlyAggregation] Rebuilding Bloom filters...");
        LiftRideBloomFilter tempFilter = new LiftRideBloomFilter(config);

        try (PreparedStatement stmt = conn.prepareStatement(config.AGGREGATION_BLOOM_FILTER_KEY_UNIQUE_SKIERS_SQL); ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                String key = RedisKeyFormatter.format(config.REDIS_KEY_PATTERN_UNIQUE_SKIERS, rs.getInt(1), rs.getString(2), rs.getInt(3));
                tempFilter.getUniqueSkiersFilter().put(key);
            }
        }

        // Daily Vertical
        try (PreparedStatement stmt = conn.prepareStatement(config.AGGREGATION_BLOOM_FILTER_KEY_DAILY_VERTICAL_SQL); ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                String key = RedisKeyFormatter.format(config.REDIS_KEY_PATTERN_DAILY_VERTICAL, rs.getInt(1), rs.getString(2), rs.getInt(3), rs.getInt(4));
                tempFilter.getDailyVerticalFilter().put(key);
            }
        }

        // Season Vertical
        try (PreparedStatement stmt = conn.prepareStatement(config.AGGREGATION_BLOOM_FILTER_KEY_SINGLE_SEASON_VERTICAL_SQL); ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                String key = RedisKeyFormatter.format(config.REDIS_KEY_PATTERN_SINGLE_SEASON_VERTICAL, rs.getInt(1), rs.getInt(2), rs.getString(3));
                tempFilter.getSeasonVerticalFilter().put(key);
            }
        }

        // All Season Vertical
        try (PreparedStatement stmt = conn.prepareStatement(config.AGGREGATION_BLOOM_FILTER_KEY_ALL_SEASON_VERTICAL_SQL); ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                String key = RedisKeyFormatter.format(config.REDIS_KEY_PATTERN_ALL_SEASON_VERTICALS, rs.getInt(1), rs.getInt(2));
                tempFilter.getTotalVerticalFilter().put(key);
            }
        }

        liftRideBloomFilter.setUniqueSkiersFilter(tempFilter.getUniqueSkiersFilter());
        liftRideBloomFilter.setDailyVerticalFilter(tempFilter.getDailyVerticalFilter());
        liftRideBloomFilter.setSeasonVerticalFilter(tempFilter.getSeasonVerticalFilter());
        liftRideBloomFilter.setTotalVerticalFilter(tempFilter.getTotalVerticalFilter());
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
        LiftRideBloomFilter liftRideBloomFilter = new LiftRideBloomFilter(config);
        AggregationStrategy bloomOnly = new BloomOnlyAggregation(config, liftRideBloomFilter);
        long startTime = System.currentTimeMillis();
        bloomOnly.run();
        long endTime = System.currentTimeMillis();
        System.out.println("[BloomOnlyAggregation] update time: " + (endTime - startTime) + " ms");
    }
}