package ratelimiter.service;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import model.Configuration;
import utils.ConfigUtils;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class RateLimiterServer {
    public static void main(String[] args) {
        try {
            Configuration config = ConfigUtils.getConfigurationForService();

            ExecutorService executor = new ThreadPoolExecutor(
                    config.RATE_LIMITER_SERVICE_MIN_THREAD,          // core pool size
                    config.RATE_LIMITER_SERVICE_MAX_THREAD,                     // max pool size
                    60, TimeUnit.SECONDS,    // idle timeout
                    new LinkedBlockingQueue<>(config.RATE_LIMITER_SERVICE_QUEUE_SIZE)  // request queue size (like acceptCount)
            );

            Server server = ServerBuilder.forPort(config.RATE_LIMITER_SERVICE_PORT)
                    .executor(executor)
                    .addService(new RateLimiterServiceImpl(config.RATE_LIMITER_WRITE_MAX_TOKENS, config.RATE_LIMITER_WRITE_REFILL_RATE))
                    .build()
                    .start();

            System.out.println("RateLimiter gRPC server started on port " + config.RATE_LIMITER_SERVICE_PORT);
            server.awaitTermination();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}