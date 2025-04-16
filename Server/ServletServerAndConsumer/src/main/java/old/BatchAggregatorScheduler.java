package old;

import batch.AggregationStrategy;
import batch.BatchUtils;
import batch.RefreshExistingCache;
import cache.RedisCacheClient;
import model.Configuration;
import utils.ConfigUtils;

import java.sql.SQLException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * This is the long-running scheduler that triggers each aggregation strategy
 * based on its configured interval (in seconds).
 */
public class BatchAggregatorScheduler {
    public static void main(String[] args) throws SQLException {
        Configuration config = ConfigUtils.getConfigurationForService();
        RedisCacheClient cacheClient = new RedisCacheClient(config);

        AggregationStrategy full = new FullAggregationRedis(config, cacheClient);
        AggregationStrategy bloomOnly = new BloomOnlyAggregationRedis(config, cacheClient);
        AggregationStrategy refreshHotKeys = new RefreshExistingCache(config, cacheClient);

        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(3);

        // FULL strategy: default to every 1 hour, only if DB record count is small enough
        scheduler.scheduleAtFixedRate(() -> {
            try {
                long totalCount = BatchUtils.getLiftRideRowCount(config);
                if (totalCount <= config.AGGREGATION_FULL_MAX_ROWS) {
                    long startTime = System.currentTimeMillis();
                    full.run();
                    long endTime = System.currentTimeMillis();
                    System.out.println("[FullAggregation] update time: " + (endTime - startTime) + " ms");
                } else {
                    System.out.println("[FullAggregation] Skipped due to large DB size: " + totalCount);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, config.AGGREGATION_FULL_INTERVAL_SEC, config.AGGREGATION_FULL_INTERVAL_SEC, TimeUnit.SECONDS);

        // BLOOM_ONLY strategy
        scheduler.scheduleAtFixedRate(() -> {
            try {
                long startTime = System.currentTimeMillis();
                bloomOnly.run();
                long endTime = System.currentTimeMillis();
                System.out.println("[BloomOnlyAggregation] update time: " + (endTime - startTime) + " ms");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, config.AGGREGATION_BLOOM_ONLY_INTERVAL_SEC, config.AGGREGATION_BLOOM_ONLY_INTERVAL_SEC, TimeUnit.SECONDS);

        // REFRESH_EXISTING_CACHE strategy
        scheduler.scheduleAtFixedRate(() -> {
            try {
                long startTime = System.currentTimeMillis();
                refreshHotKeys.run();
                long endTime = System.currentTimeMillis();
                System.out.println("[RefreshExistingCache] update time: " + (endTime - startTime) + " ms");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, config.AGGREGATION_REFRESH_CACHE_INTERVAL_SEC, config.AGGREGATION_REFRESH_CACHE_INTERVAL_SEC, TimeUnit.SECONDS);

        // Hook for graceful shutdown
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            scheduler.shutdown();
            full.close();
            bloomOnly.close();
            refreshHotKeys.close();
            cacheClient.close();
            System.out.println("Batch aggregator shut down cleanly.");
        }));
    }
}
