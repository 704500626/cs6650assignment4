package edu.northeastern.common;

import edu.northeastern.model.Configuration;
import edu.northeastern.model.ConsumerContext;

public class ConsumerThreadSyncPhase1 extends ConsumerThreadSync{

    public ConsumerThreadSyncPhase1(Configuration configuration, ConsumerContext context) {
        super(configuration, context);
    }

    @Override
    public void run() {
        for (int i = 0; i < configuration.PHASE1_PER_THREAD_REQUEST_COUNT; i++) {
            try {
                RandomRequest request = context.requestBuffer.take();
                if (request.isPoisonPill()) break; // An unlikely edge case where the poison pill is encountered before finishing sending PHASE1_PER_THREAD_REQUEST_COUNT number of requests
                sendRequestWithRetry(request);
            } catch (InterruptedException e) {
                System.err.println("Exception when calling SkierApi, error: " + e.getMessage());
                Thread.currentThread().interrupt();
            }
        }
        // Signal phase 1 completion
        if (!context.phase1Completed) {
            context.lock.lock();
            try {
                context.phase1Completed = true;
                context.phase1Completion.signal();
            } finally {
                context.lock.unlock();
            }
        }
        context.latch.countDown();
    }
}
