package cache;

import model.CacheWrite;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

public class CacheWriterWorker implements Runnable {
    private final RedisCacheClient cache;
    private final BlockingQueue<CacheWrite> queue;
    private final int batchSize;
    private final int flushIntervalMs;

    public CacheWriterWorker(RedisCacheClient cache, BlockingQueue<CacheWrite> queue, int batchSize, int flushIntervalMs) {
        this.cache = cache;
        this.queue = queue;
        this.batchSize = batchSize;
        this.flushIntervalMs = flushIntervalMs;
    }

    @Override
    public void run() {
        Map<String, String> buffer = new HashMap<>(batchSize);
        long lastFlushTime = System.currentTimeMillis();

        while (!Thread.currentThread().isInterrupted()) {
            try {
                CacheWrite write = queue.poll(flushIntervalMs, TimeUnit.MILLISECONDS);
                if (write != null) buffer.put(write.key, write.value);

                boolean shouldFlush = buffer.size() >= batchSize ||
                        (System.currentTimeMillis() - lastFlushTime >= flushIntervalMs && !buffer.isEmpty());

                if (shouldFlush) {
                    cache.getSync().mset(buffer);
                    buffer.clear();
                    lastFlushTime = System.currentTimeMillis();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt(); // graceful shutdown
            } catch (Exception e) {
                System.err.println("[CacheWriterWorker] Redis write failed: " + e.getMessage());
                e.printStackTrace();
            }
        }

        // Final flush
        if (!buffer.isEmpty()) {
            try {
                cache.getSync().mset(buffer);
            } catch (Exception e) {
                System.err.println("[CacheWriterWorker] Final flush failed: " + e.getMessage());
            }
        }
    }
}