package edu.northeastern.clientpart1;

import com.google.gson.Gson;
import edu.northeastern.common.CsvWriterThread;
import edu.northeastern.common.RandomRequest;
import edu.northeastern.utils.ServerUtils;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class SkiersApiLoadTestSync {
    // Global constants for the load test
    static final int TOTAL_REQUEST_COUNT = 200_000; // Total number of POST requests to send to the server
    static final int PHASE1_THREAD_COUNT = 32; // The number of consumer threads sending requests for phase 1
    static final int PHASE1_PER_THREAD_REQUEST_COUNT = 1000; // The number of POST requests to be sent by consumer threads of phase 1
    static final int PHASE2_THREAD_COUNT = 1000; // The number of consumer threads sending requests for phase 2
    static final int MAX_RETRIES = 5; // The maximum number of retries of each request
    static final int REQUEST_BUFFER_SIZE = 1000; // The buffer size of the http request content, kept as small as possible

    // Notice that phase 1 is completed when any one of the phase 1 threads finishes send and receiving all PHASE1_PER_THREAD_REQUEST_COUNT requests, not when all of them finish
    static final Lock lock = new ReentrantLock(); // Lock used to acquire phase1Completion
    static final Condition phase1Completion = lock.newCondition(); // Condition to wait for the completion of phase 1
    static boolean phase1Completed = false; // Boolean value signaling the completion phase 1

    // Counters for important metrics
    static final AtomicInteger successRequests = new AtomicInteger(0);
    static final AtomicInteger failedRequests = new AtomicInteger(0);
    static final AtomicLong totalResponseTime = new AtomicLong(0);

    // Shared synchronous HTTP client and related objects
    static HttpClient syncHttpClient = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).connectTimeout(Duration.ofSeconds(10)).build();
    static final Gson gson = new Gson();
    static final String serverUrl = ServerUtils.getServerUrl();

    public static void main(String[] args) throws InterruptedException {
        postLoadTestThreadPool();
    }

    // POST Load testing with ExecutorService
    private static void postLoadTestThreadPool() throws InterruptedException {
        long startTime = System.currentTimeMillis();
        // Global latch counts the total number of consumer threads (phase1 + phase2)
        CountDownLatch latch = new CountDownLatch(PHASE1_THREAD_COUNT + PHASE2_THREAD_COUNT);
        BlockingQueue<RandomRequest> requestBuffer = new LinkedBlockingQueue<>(REQUEST_BUFFER_SIZE);

        // Submit the Producer task to an executor (or start it in its own thread)
        ExecutorService producerExecutor = Executors.newSingleThreadExecutor();
        producerExecutor.submit(new ProducerThread(requestBuffer));
        producerExecutor.shutdown();

        // Create an ExecutorService for consumer tasks
        int totalConsumerThreads = PHASE1_THREAD_COUNT + PHASE2_THREAD_COUNT;
        ExecutorService consumerExecutor = Executors.newFixedThreadPool(totalConsumerThreads);

        // Phase 1: Submit PHASE1_THREAD_COUNT ConsumerThread tasks
        for (int i = 0; i < PHASE1_THREAD_COUNT; i++) {
            consumerExecutor.submit(new ConsumerPhase1Thread(requestBuffer, latch));
        }

        // Wait for phase 1 to signal completion, only one thread signals this
        lock.lock();
        try {
            while (!phase1Completed) {
                phase1Completion.await();
            }
        } finally {
            lock.unlock();
        }

        long phase1ResponseTime = totalResponseTime.get();
        int phase1SuccessfulRequestCount = successRequests.get();
        int phase1FailedRequestCount = failedRequests.get();
        long phase1EndTime = System.currentTimeMillis();
        System.out.println("Total successful request for Phase 1: " + phase1SuccessfulRequestCount);
        System.out.println("Total unsuccessful request for Phase 1: " + phase1FailedRequestCount);
        System.out.println("Phase 1 completed with " + (phase1EndTime - startTime) + " ms");
        System.out.println("Phase 1 Avg Response time: " + (double) phase1ResponseTime / (phase1SuccessfulRequestCount + phase1FailedRequestCount) + " ms");
        System.out.println("Phase 1 Throughput: " + (phase1SuccessfulRequestCount + phase1FailedRequestCount) / ((double) (phase1EndTime - startTime) / 1000)  + " req/s");

        long phase2StartTime = System.currentTimeMillis();
        // Phase 2: Submit PHASE2_THREAD_COUNT ConsumerThread tasks
        for (int i = 0; i < PHASE2_THREAD_COUNT; i++) {
            consumerExecutor.submit(new ConsumerPhase2Thread(requestBuffer, latch));
        }
        // Wait for all consumer tasks to finish
        latch.await();

        long endTime = System.currentTimeMillis();
        System.out.println("Total successful request: " + successRequests.get());
        System.out.println("Total unsuccessful request: " + failedRequests.get());
        System.out.println("Total time to send " + TOTAL_REQUEST_COUNT + " requests: " + (endTime - startTime) + " ms");
        System.out.println("Total Avg Response time: " + (double) totalResponseTime.get() / TOTAL_REQUEST_COUNT + " ms");
        System.out.println("Phase 2 Avg Response time: " + (double) (totalResponseTime.get() - phase1ResponseTime)/ (TOTAL_REQUEST_COUNT - phase1SuccessfulRequestCount - phase1FailedRequestCount)+ " ms");
        System.out.println("Phase 2 Throughput " + (TOTAL_REQUEST_COUNT - phase1SuccessfulRequestCount - phase1FailedRequestCount) / ((double) (endTime - phase2StartTime) / 1000)  + " req/s");
        System.out.println("Total Throughput " + TOTAL_REQUEST_COUNT / ((double) (endTime - startTime) / 1000)  + " req/s");
        System.out.println("Number of threads in phase 1 responsible for sending and receiving requests: " + PHASE1_THREAD_COUNT);
        System.out.println("Number of threads in phase 2 responsible for sending and receiving requests: " + PHASE2_THREAD_COUNT);

        // Shut down the consumer executor
        consumerExecutor.shutdown();
    }

    // Synchronous request with retry method (shared by both phases)
    private static void sendRequestWithRetry(RandomRequest request) {
        HttpRequest httpRequest = RandomRequest.buildHttpRequestForRandomRequest(request, serverUrl, gson.toJson(request.getLiftRide()));
        int attempts = 0;
        long st = System.currentTimeMillis();
        int statusCode = HttpURLConnection.HTTP_INTERNAL_ERROR;
        try {
            while (attempts < MAX_RETRIES) {
                try {
                    HttpResponse<String> response = syncHttpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
                    statusCode = response.statusCode();
                    if (statusCode == HttpURLConnection.HTTP_CREATED) {
                        break;
                    }
                    System.err.println("Received status code: " + statusCode + " on attempt " + (attempts + 1));
                } catch (IOException e) {
                    statusCode = HttpURLConnection.HTTP_INTERNAL_ERROR;
                    System.err.println("Request failed: " + e.getMessage() + " (attempt " + (attempts + 1) + ")");
                }
                attempts++;
                Thread.sleep(200L * attempts);
            }
            if (attempts == MAX_RETRIES) {
                System.err.println("Request failed after " + MAX_RETRIES + " attempts.");
            }
            long et = System.currentTimeMillis();
            totalResponseTime.getAndAdd(et - st);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            if (statusCode == HttpURLConnection.HTTP_CREATED) {
                successRequests.incrementAndGet();
            } else {
                failedRequests.incrementAndGet();
            }
        }
    }

    static class ProducerThread implements Runnable {
        private final BlockingQueue<RandomRequest> buffer;

        public ProducerThread(BlockingQueue<RandomRequest> data) {
            this.buffer = data;
        }

        @Override
        public void run() {
            try {
                for (int i = 0; i < TOTAL_REQUEST_COUNT; i++)
                    this.buffer.put(new RandomRequest());
                for (int i = 0; i < PHASE2_THREAD_COUNT + PHASE1_THREAD_COUNT; i++)
                    this.buffer.put(RandomRequest.POISON_PILL);
            } catch (InterruptedException e) {
                System.err.println("Producer interrupted.");
                Thread.currentThread().interrupt();
            }
        }
    }

    static class ConsumerPhase1Thread implements Runnable {
        private final CountDownLatch latch;
        private final BlockingQueue<RandomRequest> buffer;

        public ConsumerPhase1Thread(BlockingQueue<RandomRequest> buffer, CountDownLatch latch) {
            this.buffer = buffer;
            this.latch = latch;
        }

        @Override
        public void run() {
            for (int i = 0; i < PHASE1_PER_THREAD_REQUEST_COUNT; i++) {
                try {
                    RandomRequest request = buffer.take();
                    if (request.isPoisonPill()) break; // An unlikely edge case where the poison pill is encountered before finishing sending PHASE1_PER_THREAD_REQUEST_COUNT number of requests
                    sendRequestWithRetry(request);
                } catch (InterruptedException e) {
                    System.err.println("Exception when calling SkierApi, error: " + e.getMessage());
                    Thread.currentThread().interrupt();
                }
            }
            // Signal phase 1 completion
            if (!phase1Completed) {
                lock.lock();
                try {
                    phase1Completed = true;
                    phase1Completion.signal();
                } finally {
                    lock.unlock();
                }
            }
            latch.countDown();
        }
    }

    static class ConsumerPhase2Thread implements Runnable {
        private final CountDownLatch latch;
        private final BlockingQueue<RandomRequest> buffer;

        public ConsumerPhase2Thread(BlockingQueue<RandomRequest> buffer, CountDownLatch latch) {
            this.buffer = buffer;
            this.latch = latch;
        }

        @Override
        public void run() {
            while (true) {
                try {
                    RandomRequest request = buffer.take();
                    if (request.isPoisonPill()) break;
                    sendRequestWithRetry(request);
                } catch (InterruptedException e) {
                    System.err.println("Exception when calling SkierApi, error: " + e.getMessage());
                    Thread.currentThread().interrupt();
                }
            }
            latch.countDown();
        }
    }
}
