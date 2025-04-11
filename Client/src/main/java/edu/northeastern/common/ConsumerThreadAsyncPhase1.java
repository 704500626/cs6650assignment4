package edu.northeastern.common;

import edu.northeastern.model.Configuration;
import edu.northeastern.model.ConsumerContext;

import java.net.http.HttpRequest;
import java.util.concurrent.CountDownLatch;

public class ConsumerThreadAsyncPhase1 extends ConsumerThreadAsync {

    public ConsumerThreadAsyncPhase1(Configuration configuration, ConsumerContext context) {
        super(configuration, context);
    }

    @Override
    public void run() {
        // A local CountDownLatch for the known number of requests, because we are using async requests
        CountDownLatch localLatch = new CountDownLatch(configuration.PHASE1_PER_THREAD_REQUEST_COUNT);
        for (int i = 0; i < configuration.PHASE1_PER_THREAD_REQUEST_COUNT; i++) {
            try {
                RandomRequest request = context.requestBuffer.take();
                if (request.isPoisonPill()) break; // An unlikely edge case where the poison pill is encountered before finishing sending PHASE1_PER_THREAD_REQUEST_COUNT number of requests
                context.concurrentRequests.acquire(); // Ensure we do not exceed the concurrent request limit.
                long st = System.currentTimeMillis();
                HttpRequest httpRequest = RandomRequest.buildHttpRequestForRandomRequest(request, configuration.serverUrl, context.gson.toJson(request.getLiftRide()));
                sendRequestWithSyncRetryAsync(httpRequest, 0).whenComplete((statusCode, ex) -> {
                    long et = System.currentTimeMillis();
                    context.endRequestUpdate(statusCode, st, et - st, configuration);
                    localLatch.countDown();
                });
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        // Wait until all asynchronous requests from this thread are complete
        try {
            localLatch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        // Signal phase 1 completion
        context.lock.lock();
        try {
            if (!context.phase1Completed) {
                context.phase1Completed = true;
                context.phase1Completion.signal();
            }
        } finally {
            context.lock.unlock();
        }
        context.latch.countDown();
    }
}
