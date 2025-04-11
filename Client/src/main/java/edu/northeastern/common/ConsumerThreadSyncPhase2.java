package edu.northeastern.common;

import edu.northeastern.model.Configuration;
import edu.northeastern.model.ConsumerContext;

public class ConsumerThreadSyncPhase2 extends ConsumerThreadSync {

    public ConsumerThreadSyncPhase2(Configuration configuration, ConsumerContext context) {
        super(configuration, context);
    }

    @Override
    public void run() {
        while (true) {
            try {
                RandomRequest request = context.requestBuffer.take();
                if (request.isPoisonPill()) break;
                sendRequestWithRetry(request);
            } catch (InterruptedException e) {
                System.err.println("Exception when calling SkierApi, error: " + e.getMessage());
                Thread.currentThread().interrupt();
            }
        }
        context.latch.countDown();
    }
}
