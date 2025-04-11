package edu.northeastern.common;

import java.util.concurrent.BlockingQueue;

public class ProducerThread implements Runnable {
    private final BlockingQueue<RandomRequest> buffer;
    private final int requestCount;
    private final int poisonCount;

    public ProducerThread(BlockingQueue<RandomRequest> data, int requestCount, int poisonCount) {
        this.buffer = data;
        this.requestCount = requestCount;
        this.poisonCount = poisonCount;
    }

    @Override
    public void run() {
        try {
            for (int i = 0; i < requestCount; i++)
                this.buffer.put(new RandomRequest());
            for (int i = 0; i < poisonCount; i++)
                this.buffer.put(RandomRequest.POISON_PILL);
        } catch (InterruptedException e) {
            System.err.println("Producer interrupted.");
            Thread.currentThread().interrupt();
        }
    }
}