package edu.northeastern.common;

import edu.northeastern.model.Configuration;
import edu.northeastern.model.ConsumerContext;

import java.net.http.HttpRequest;
import java.util.concurrent.Phaser;

public class ConsumerThreadAsyncPhase2 extends ConsumerThreadAsync {

    public ConsumerThreadAsyncPhase2(Configuration configuration, ConsumerContext context) {
        super(configuration, context);
    }

    @Override
    public void run() {
        // Create a Phaser with 1 registered party (the thread itself), and new asynchronous tasks will register themselves.
        Phaser phaser = new Phaser(1);
        while (true) {
            try {
                RandomRequest request = context.requestBuffer.take();
                if (request.isPoisonPill()) break;
                phaser.register(); // Register a new party for this asynchronous task
                context.concurrentRequests.acquire(); // Ensure we do not exceed the concurrent request limit.
                long st = System.currentTimeMillis();
                HttpRequest httpRequest = RandomRequest.buildHttpRequestForRandomRequest(request, configuration.serverUrl, context.gson.toJson(request.getLiftRide()));
                sendRequestWithSyncRetryAsync(httpRequest, 0).whenComplete((statusCode, ex) -> {
                    long et = System.currentTimeMillis();
                    context.endRequestUpdate(statusCode, st, et - st, configuration);
                    phaser.arriveAndDeregister();
                });
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        // Arrive and await all registered tasks to complete.
        phaser.arriveAndAwaitAdvance();
        context.latch.countDown();
    }
}
