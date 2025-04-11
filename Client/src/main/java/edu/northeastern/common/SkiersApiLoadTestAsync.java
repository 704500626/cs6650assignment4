package edu.northeastern.common;

import edu.northeastern.model.Configuration;
import edu.northeastern.model.ConsumerContext;

import java.util.concurrent.*;

public class SkiersApiLoadTestAsync {
    public static void main(String[] args) throws InterruptedException {
        Configuration config = new Configuration(false, false);
        // An executor with a fixed thread pool for the async client
        ExecutorService executor = Executors.newFixedThreadPool(config.MAX_CONCURRENT_REQUESTS);
        ConsumerContext context = new ConsumerContext(executor, config);
        postLoadTest(config, context);
        executor.shutdown();
        executor.awaitTermination(1, TimeUnit.MINUTES);
    }

    public static void postLoadTest(Configuration config, ConsumerContext context) throws InterruptedException {
        int totalConsumerThreads = config.PHASE1_THREAD_COUNT + config.PHASE2_THREAD_COUNT;
        long startTime = System.currentTimeMillis();
        // Create Producer thread for generating request content
        ExecutorService producerExecutor = Executors.newSingleThreadExecutor();
        producerExecutor.submit(new ProducerThread(context.requestBuffer, config.TOTAL_REQUEST_COUNT, totalConsumerThreads));
        producerExecutor.shutdown();

        // Phase 1: Create and start PHASE1_THREAD_COUNT ConsumerThread
        for (int i = 0; i < config.PHASE1_THREAD_COUNT; i++) {
            new Thread(new ConsumerThreadAsyncPhase1(config, context)).start();
        }

        // Wait for phase 1 to signal completion (only one thread needs to do this)
        context.lock.lock();
        try {
            while (!context.phase1Completed) {
                context.phase1Completion.await();
            }
        } finally {
            context.lock.unlock();
        }

        long phase1ResponseTime = context.totalResponseTime.get();
        int phase1SuccessfulRequestCount = context.successRequests.get();
        int phase1FailedRequestCount = context.failedRequests.get();
        long phase1EndTime = System.currentTimeMillis();
        System.out.println("Total successful request for Phase 1: " + phase1SuccessfulRequestCount);
        System.out.println("Total unsuccessful request for Phase 1: " + phase1FailedRequestCount);
        System.out.println("Phase 1 completed with " + (phase1EndTime - startTime) + " ms");
        System.out.println("Phase 1 Avg Response time: " + (double) phase1ResponseTime / (phase1SuccessfulRequestCount + phase1FailedRequestCount) + " ms");
        System.out.println("Phase 1 Throughput: " + (phase1SuccessfulRequestCount + phase1FailedRequestCount) / ((double) (phase1EndTime - startTime) / 1000)  + " req/s");

        long phase2StartTime = System.currentTimeMillis();
        // Phase 2: Create and start PHASE2_THREAD_COUNT ConsumerThread
        for (int i = 0; i < config.PHASE2_THREAD_COUNT; i++) {
            new Thread(new ConsumerThreadAsyncPhase2(config, context)).start();
        }
        // Wait for all consumer tasks to finish
        context.latch.await();

        long endTime = System.currentTimeMillis();
        System.out.println("Total successful request: " + context.successRequests.get());
        System.out.println("Total unsuccessful request: " + context.failedRequests.get());
        System.out.println("Total time to send " + config.TOTAL_REQUEST_COUNT + " requests: " + (endTime - startTime) + " ms");
        System.out.println("Total Avg Response time: " + (double) context.totalResponseTime.get() / config.TOTAL_REQUEST_COUNT + " ms");
        System.out.println("Phase 2 Avg Response time: " + (double) (context.totalResponseTime.get() - phase1ResponseTime)/ (config.TOTAL_REQUEST_COUNT - phase1SuccessfulRequestCount - phase1FailedRequestCount)+ " ms");
        System.out.println("Phase 2 Throughput " + (config.TOTAL_REQUEST_COUNT - phase1SuccessfulRequestCount - phase1FailedRequestCount) / ((double) (endTime - phase2StartTime) / 1000)  + " req/s");
        System.out.println("Total Throughput " + config.TOTAL_REQUEST_COUNT / ((double) (endTime - startTime) / 1000)  + " req/s");
        System.out.println("Number of threads in phase 1 responsible for sending requests: " + config.PHASE1_THREAD_COUNT);
        System.out.println("Number of threads in phase 2 responsible for sending requests: " + config.PHASE2_THREAD_COUNT);
        System.out.println("Number of threads in thread pool responsible for receiving requests: " + config.MAX_CONCURRENT_REQUESTS);
    }
}
