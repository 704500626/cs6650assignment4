package batch;

import bloomfilter.LiftRideBloomFilter;
import cache.RedisCacheClient;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import model.Configuration;
import utils.ConfigUtils;

import java.util.concurrent.*;

public class BatchAggregationServer {
    public static void main(String[] args) throws Exception {
        Configuration config = ConfigUtils.getConfigurationForService();

        ExecutorService executor = new ThreadPoolExecutor(
                config.AGGREGATION_SERVICE_MIN_THREAD,          // core pool size
                config.AGGREGATION_SERVICE_MAX_THREAD,                     // max pool size
                60, TimeUnit.SECONDS,    // idle timeout
                new LinkedBlockingQueue<>(config.AGGREGATION_SERVICE_REQUEST_QUEUE_SIZE)  // request queue size (like acceptCount)
        );

        LiftRideBloomFilter bloomFilter = new LiftRideBloomFilter(config);
        RedisCacheClient cacheClient = new RedisCacheClient(config);
        AggregationStrategy full = new FullAggregationGuava(config, cacheClient, bloomFilter);
        AggregationStrategy bloomOnly = new BloomOnlyAggregationGuava(config, bloomFilter);
        AggregationStrategy refreshHotKeys = new RefreshExistingCache(config, cacheClient);

        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(3);

        // FULL strategy: default to every 1 minute, only if DB record count is small enough and it's enabled
        if (config.AGGREGATION_FULL_SWITCH) {
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
        }

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
            if (config.AGGREGATION_FULL_SWITCH) full.close();
            bloomOnly.close();
            refreshHotKeys.close();
            cacheClient.close();
            System.out.println("Batch aggregator shut down cleanly.");
        }));

        BatchAggregationServiceImpl batchAggregationService = new BatchAggregationServiceImpl(bloomFilter);
        Server server = ServerBuilder.forPort(config.AGGREGATION_SERVICE_PORT)
                .executor(executor)
                .addService(batchAggregationService)
                .build()
                .start();
        System.out.println("Aggregation Service gRPC server started on port " + config.AGGREGATION_SERVICE_PORT);
        server.awaitTermination();
    }
}