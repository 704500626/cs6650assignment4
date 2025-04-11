package edu.northeastern.model;

import com.google.gson.Gson;
import edu.northeastern.common.RandomRequest;

import java.net.HttpURLConnection;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class ConsumerContext {
    // Notice that phase 1 is completed when any one of the phase 1 threads finishes send and receiving all PHASE1_PER_THREAD_REQUEST_COUNT requests, not when all of them finish
    public final Lock lock = new ReentrantLock(); // Lock used to acquire phase1Completion
    public final Condition phase1Completion = lock.newCondition(); // Condition to wait for the completion of phase 1
    public boolean phase1Completed = false; // Boolean value signaling the completion phase 1

    // Counters for important metrics
    public final AtomicInteger successRequests = new AtomicInteger(0);
    public final AtomicInteger failedRequests = new AtomicInteger(0);
    public final AtomicLong totalResponseTime = new AtomicLong(0);

    // Shared HTTP client and related objects
    public HttpClient httpClient;
    public final Gson gson = new Gson();

    public Semaphore concurrentRequests; // A semaphore to allow at most MAX_CONCURRENT_REQUESTS concurrent asynchronous requests
    public BlockingQueue<RandomRequest> requestBuffer;
    // Global latch counts the total number of consumer threads (phase1 + phase2)
    public CountDownLatch latch;
    // Thread-safe collection to store request metrics
    public BlockingQueue<String[]> metricsBuffer;

    public ConsumerContext(ExecutorService executor, Configuration configuration) {
        latch = new CountDownLatch(configuration.PHASE1_THREAD_COUNT + configuration.PHASE2_THREAD_COUNT);
        requestBuffer = new LinkedBlockingQueue<>(configuration.TOTAL_REQUEST_COUNT);
        if (configuration.sync) {
            httpClient = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).connectTimeout(Duration.ofSeconds(10)).build();
        } else {
            httpClient = HttpClient.newBuilder().executor(executor).version(HttpClient.Version.HTTP_1_1).connectTimeout(Duration.ofSeconds(10)).build();
            concurrentRequests = new Semaphore(configuration.MAX_CONCURRENT_REQUESTS);
        }
        if (configuration.storeMetrics) {
            metricsBuffer = new LinkedBlockingQueue<>(configuration.METRICS_BUFFER_SIZE);
        }
    }

    public void endRequestUpdate(int statusCode, long startTime, long timeDelta, Configuration configuration) {
        if (configuration.sync) {
            endRequestUpdateSync(statusCode, startTime, timeDelta, configuration);
        } else {
            endRequestUpdateAsync(statusCode, startTime, timeDelta, configuration);
        }
    }

    private void endRequestUpdateSync(int statusCode, long startTime, long timeDelta, Configuration configuration) {
        totalResponseTime.getAndAdd(timeDelta);
        if (statusCode == HttpURLConnection.HTTP_CREATED) {
            successRequests.incrementAndGet();
        } else {
            failedRequests.incrementAndGet();
        }
        if (configuration.storeMetrics) {
            String[] record = {String.valueOf(startTime), "POST", String.valueOf(timeDelta), String.valueOf(statusCode)};
            try {
                metricsBuffer.put(record);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void endRequestUpdateAsync(int statusCode, long startTime, long timeDelta, Configuration configuration) {
        if (statusCode == HttpURLConnection.HTTP_CREATED) {
            successRequests.incrementAndGet();
        } else {
            failedRequests.incrementAndGet();
        }
        totalResponseTime.getAndAdd(timeDelta);
        concurrentRequests.release();
        if (configuration.storeMetrics) {
            String[] record = {String.valueOf(startTime), "POST", String.valueOf(timeDelta), String.valueOf(statusCode)};
            try {
                metricsBuffer.put(record);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
