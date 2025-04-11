package edu.northeastern.common;

import edu.northeastern.model.Configuration;
import edu.northeastern.model.ConsumerContext;

import java.util.concurrent.*;

public class SkiersApiLoadTestSync {
    public static void main(String[] args) throws InterruptedException {
        Configuration config = new Configuration(true, false);
        ConsumerContext context = new ConsumerContext(null, config);
        postLoadTestThreadPool(config, context);
    }

    // POST Load testing with ExecutorService
    public static void postLoadTestThreadPool(Configuration config, ConsumerContext context) throws InterruptedException {
        int totalConsumerThreads = config.PHASE1_THREAD_COUNT + config.PHASE2_THREAD_COUNT;
        long startTime = System.currentTimeMillis();

        // Submit the Producer task to an executor (or start it in its own thread)
        ExecutorService producerExecutor = Executors.newSingleThreadExecutor();
        producerExecutor.submit(new ProducerThread(context.requestBuffer, config.TOTAL_REQUEST_COUNT, totalConsumerThreads));
        producerExecutor.shutdown();

        // Create an ExecutorService for consumer tasks
        ExecutorService consumerExecutor = Executors.newFixedThreadPool(totalConsumerThreads);

        // Phase 1: Submit PHASE1_THREAD_COUNT ConsumerThread tasks
        for (int i = 0; i < config.PHASE1_THREAD_COUNT; i++) {
            consumerExecutor.submit(new ConsumerThreadSyncPhase1(config, context));
        }

        // Wait for phase 1 to signal completion, only one thread signals this
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
        // Phase 2: Submit PHASE2_THREAD_COUNT ConsumerThread tasks
        for (int i = 0; i < config.PHASE2_THREAD_COUNT; i++) {
            consumerExecutor.submit(new ConsumerThreadSyncPhase2(config, context));
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
        System.out.println("Number of threads in phase 1 responsible for sending and receiving requests: " + config.PHASE1_THREAD_COUNT);
        System.out.println("Number of threads in phase 2 responsible for sending and receiving requests: " + config.PHASE2_THREAD_COUNT);

        // Shut down the consumer executor
        consumerExecutor.shutdown();
    }
}
