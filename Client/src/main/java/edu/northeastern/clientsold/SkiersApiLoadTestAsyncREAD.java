package edu.northeastern.clientsold;

import com.google.gson.Gson;
import edu.northeastern.common.CsvWriterThread;
import edu.northeastern.common.RandomRequest;
import edu.northeastern.utils.ConfigUtils;

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

public class SkiersApiLoadTestAsyncREAD {
    // Global constants for the load test
    static final int TOTAL_REQUEST_COUNT = 200_000; // Total number of POST requests to send to the server
    static final int PHASE1_THREAD_COUNT = 32; // The number of consumer threads sending requests for phase 1
    static final int PHASE1_PER_THREAD_REQUEST_COUNT = 1000; // The number of POST requests to be sent by consumer threads of phase 1
    static final int PHASE2_THREAD_COUNT = 1; // The number of consumer threads sending requests for phase 2
    static final int MAX_RETRIES = 5; // The maximum number of retries of each request
    static final int REQUEST_BUFFER_SIZE = 3000; // The buffer size of the http request content, kept as small as possible
    static final int MAX_CONCURRENT_REQUESTS = 100; // The maximum number of concurrent requests
    static final int METRICS_BUFFER_SIZE = 5000; // The buffer size of metrics queued

    // Notice that phase 1 is completed when any one of the phase 1 threads finishes send and receiving all PHASE1_PER_THREAD_REQUEST_COUNT requests, not when all of them finish
    static final Lock lock = new ReentrantLock(); // Lock used to acquire phase1Completion
    static final Condition phase1Completion = lock.newCondition(); // Condition to wait for the completion of phase 1
    static boolean phase1Completed = false; // Boolean value signaling the completion phase 1

    // Counters for important metrics
    static final AtomicInteger successRequests = new AtomicInteger(0);
    static final AtomicInteger failedRequests = new AtomicInteger(0);
    static final AtomicLong totalResponseTime = new AtomicLong(0);

    // Shared asynchronous HTTP client and related objects
    static HttpClient asyncHttpClient;
    static final Semaphore concurrentRequests = new Semaphore(MAX_CONCURRENT_REQUESTS); // A semaphore to allow at most MAX_CONCURRENT_REQUESTS concurrent asynchronous requests
    static final String serverUrl = ConfigUtils.getServerUrl();
    static final Gson gson = new Gson();

    // Thread-safe collection to store request metrics
    static final BlockingQueue<String[]> metricsBuffer = new LinkedBlockingQueue<>(METRICS_BUFFER_SIZE);
    static final String CSV_FILE = "request_metrics.csv";

    public static void main(String[] args) throws InterruptedException, IOException {
        postLoadTest();
        CsvWriterThread.calculateMetrics(CSV_FILE);
    }

    private static void postLoadTest() throws InterruptedException {
        // An executor with a fixed thread pool for the async client
        ExecutorService executor = Executors.newFixedThreadPool(MAX_CONCURRENT_REQUESTS);
        asyncHttpClient = HttpClient.newBuilder().executor(executor).version(HttpClient.Version.HTTP_1_1).connectTimeout(Duration.ofSeconds(10)).build();

        long startTime = System.currentTimeMillis();
        // Global latch counts the total number of consumer threads (phase1 + phase2)
        CountDownLatch latch = new CountDownLatch(PHASE1_THREAD_COUNT + PHASE2_THREAD_COUNT);
        BlockingQueue<RandomRequest> requestBuffer = new LinkedBlockingQueue<>(REQUEST_BUFFER_SIZE);

        // Create Producer thread for generating request content
        Thread producerThread = new Thread(new ProducerThread(requestBuffer));
        producerThread.start();

        // CSV Writer thread
        Thread csvWriterThread = new Thread(new CsvWriterThread(metricsBuffer, CSV_FILE));
        csvWriterThread.start();

        // Phase 1: Create and start PHASE1_THREAD_COUNT ConsumerThread
        for (int i = 0; i < PHASE1_THREAD_COUNT; i++) {
            new Thread(new ConsumerPhase1Thread(requestBuffer, latch)).start();
        }

        // Wait for phase 1 to signal completion (only one thread needs to do this)
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
        // Phase 2: Create and start PHASE2_THREAD_COUNT ConsumerThread
        for (int i = 0; i < PHASE2_THREAD_COUNT; i++) {
            new Thread(new ConsumerPhase2Thread(requestBuffer, latch)).start();
        }
        // Wait for all consumer tasks to finish
        latch.await();

        // Signal the CSV writer to finish
        metricsBuffer.put(new String[]{"EOF"});
        csvWriterThread.join();

        long endTime = System.currentTimeMillis();
        System.out.println("Total successful request: " + successRequests.get());
        System.out.println("Total unsuccessful request: " + failedRequests.get());
        System.out.println("Total time to send " + TOTAL_REQUEST_COUNT + " requests: " + (endTime - startTime) + " ms");
        System.out.println("Total Avg Response time: " + (double) totalResponseTime.get() / TOTAL_REQUEST_COUNT + " ms");
        System.out.println("Phase 2 Avg Response time: " + (double) (totalResponseTime.get() - phase1ResponseTime)/ (TOTAL_REQUEST_COUNT - phase1SuccessfulRequestCount - phase1FailedRequestCount)+ " ms");
        System.out.println("Phase 2 Throughput " + (TOTAL_REQUEST_COUNT - phase1SuccessfulRequestCount - phase1FailedRequestCount) / ((double) (endTime - phase2StartTime) / 1000)  + " req/s");
        System.out.println("Total Throughput " + TOTAL_REQUEST_COUNT / ((double) (endTime - startTime) / 1000)  + " req/s");
        System.out.println("Number of threads in phase 1 responsible for sending requests: " + PHASE1_THREAD_COUNT);
        System.out.println("Number of threads in phase 2 responsible for sending requests: " + PHASE2_THREAD_COUNT);
        System.out.println("Number of threads in thread pool responsible for receiving requests: " + MAX_CONCURRENT_REQUESTS);

        executor.shutdown();
        executor.awaitTermination(1, TimeUnit.MINUTES);
    }

    // Asynchronous request with retry method (shared by both phases)
    private static CompletableFuture<Integer> sendRequestWithRetryAsync(HttpRequest httpRequest, int attempt) {
        return asyncHttpClient.sendAsync(httpRequest, HttpResponse.BodyHandlers.discarding()).thenCompose(response -> {
            if (response.statusCode() == HttpURLConnection.HTTP_OK) {
                return CompletableFuture.completedFuture(response.statusCode());
            } else {
//                System.err.println("Received status code: " + response.statusCode() + " on attempt " + (attempt + 1));
                if (attempt < MAX_RETRIES - 1) {
                    return CompletableFuture.supplyAsync(() -> null, CompletableFuture.delayedExecutor(200L * (attempt + 1), TimeUnit.MILLISECONDS)).thenCompose(v -> sendRequestWithRetryAsync(httpRequest, attempt + 1));
                } else {
                    System.err.println("Request permanently failed after " + MAX_RETRIES + " attempts.");
                    return CompletableFuture.completedFuture(HttpURLConnection.HTTP_INTERNAL_ERROR);
                }
            }
        }).exceptionally(ex -> {
            System.err.println("Request failed: " + ex.getMessage() + " (attempt " + (attempt + 1) + ")");
            if (attempt < MAX_RETRIES - 1) {
                try {
                    Thread.sleep(200L * (attempt + 1));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                return sendRequestWithRetryAsync(httpRequest, attempt + 1).join();
            } else {
                System.err.println("Request permanently failed after " + MAX_RETRIES + " attempts.");
            }
            return HttpURLConnection.HTTP_INTERNAL_ERROR;
        });
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
            // A local CountDownLatch for the known number of requests, because we are using async requests
            CountDownLatch localLatch = new CountDownLatch(PHASE1_PER_THREAD_REQUEST_COUNT);
            for (int i = 0; i < PHASE1_PER_THREAD_REQUEST_COUNT; i++) {
                try {
                    RandomRequest request = buffer.take();
                    if (request.isPoisonPill()) break; // An unlikely edge case where the poison pill is encountered before finishing sending PHASE1_PER_THREAD_REQUEST_COUNT number of requests
                    concurrentRequests.acquire(); // Ensure we do not exceed the concurrent request limit.
                    long st = System.currentTimeMillis();
                    HttpRequest httpRequest = RandomRequest.buildHttpRequestForGetRandomRequest(request, serverUrl);
                    sendRequestWithSyncRetryAsync(httpRequest, 0).whenComplete((statusCode, ex) -> {
                        if (statusCode == HttpURLConnection.HTTP_OK) {
                            successRequests.incrementAndGet();
                        } else {
                            failedRequests.incrementAndGet();
                        }
                        long et = System.currentTimeMillis();
                        totalResponseTime.getAndAdd(et - st);
                        concurrentRequests.release();
                        localLatch.countDown();
                        String[] record = {String.valueOf(st), "POST", String.valueOf(et - st), String.valueOf(statusCode)};
                        try {
                            metricsBuffer.put(record);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
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
            lock.lock();
            try {
                if (!phase1Completed) {
                    phase1Completed = true;
                    phase1Completion.signal();
                }
            } finally {
                lock.unlock();
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
            // Create a Phaser with 1 registered party (the thread itself), and new asynchronous tasks will register themselves.
            Phaser phaser = new Phaser(1);
            while (true) {
                try {
                    RandomRequest request = buffer.take();
                    if (request.isPoisonPill()) break;
                    phaser.register(); // Register a new party for this asynchronous task
                    concurrentRequests.acquire(); // Ensure we do not exceed the concurrent request limit.
                    long st = System.currentTimeMillis();
                    HttpRequest httpRequest = RandomRequest.buildHttpRequestForGetRandomRequest(request, serverUrl);
                    sendRequestWithSyncRetryAsync(httpRequest, 0).whenComplete((statusCode, ex) -> {
                        if (statusCode == HttpURLConnection.HTTP_OK) {
                            successRequests.incrementAndGet();
                        } else {
                            failedRequests.incrementAndGet();
                        }
                        long et = System.currentTimeMillis();
                        totalResponseTime.getAndAdd(et - st);
                        concurrentRequests.release();
                        phaser.arriveAndDeregister();
                        String[] record = {String.valueOf(st), "POST", String.valueOf(et - st), String.valueOf(statusCode)};
                        try {
                            metricsBuffer.put(record);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                    });
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            // Arrive and await all registered tasks to complete.
            phaser.arriveAndAwaitAdvance();
            latch.countDown();
        }
    }

    private static CompletableFuture<Integer> sendRequestWithSyncRetryAsync(HttpRequest httpRequest, int attempt) {
        return asyncHttpClient.sendAsync(httpRequest, HttpResponse.BodyHandlers.discarding()).thenCompose(response -> {
            if (response.statusCode() == HttpURLConnection.HTTP_OK) {
                return CompletableFuture.completedFuture(response.statusCode());
            } else {
//                System.err.println("Received status code: " + response.statusCode() + " on attempt " + (attempt + 1));
                if (attempt < MAX_RETRIES - 1) {
                    try {
                        Thread.sleep(200L * (attempt + 1)); // Synchronous sleep before retry
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    return sendRequestWithSyncRetryAsync(httpRequest, attempt + 1); // Retry synchronously
                } else {
                    System.err.println("Request permanently failed after " + MAX_RETRIES + " attempts.");
                    return CompletableFuture.completedFuture(HttpURLConnection.HTTP_INTERNAL_ERROR);
                }
            }
        }).exceptionally(ex -> {
            System.err.println("Request failed: " + ex.getMessage() + " (attempt " + (attempt + 1) + ")");
            if (attempt < MAX_RETRIES - 1) {
                try {
                    Thread.sleep(200L * (attempt + 1)); // Synchronous sleep before retry
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                return sendRequestWithSyncRetryAsync(httpRequest, attempt + 1).join(); // Retry synchronously
            } else {
                System.err.println("Request permanently failed after " + MAX_RETRIES + " attempts.");
            }
            return HttpURLConnection.HTTP_INTERNAL_ERROR;
        });
    }

    private static CompletableFuture<Integer> sendRequestWithSyncRetryAsync_(HttpRequest httpRequest, int attempt) {
        return asyncHttpClient.sendAsync(httpRequest, HttpResponse.BodyHandlers.discarding()).thenCompose(response -> {
            if (response.statusCode() == HttpURLConnection.HTTP_OK) {
                return CompletableFuture.completedFuture(response.statusCode());
            } else {
                System.err.println("Received status code: " + response.statusCode() + " on attempt " + (attempt + 1));
                if (attempt < MAX_RETRIES - 1) {
                    return CompletableFuture.completedFuture(null)
                            .thenCompose(v -> {
                                try {
                                    Thread.sleep(200L * (attempt + 1)); // Block the current thread for the backoff period
                                } catch (InterruptedException e) {
                                    Thread.currentThread().interrupt();
                                }
                                return sendRequestWithRetryAsync(httpRequest, attempt + 1);
                            });
                } else {
                    System.err.println("Request permanently failed after " + MAX_RETRIES + " attempts.");
                    return CompletableFuture.completedFuture(HttpURLConnection.HTTP_INTERNAL_ERROR);
                }
            }
        }).exceptionally(ex -> {
            System.err.println("Request failed: " + ex.getMessage() + " (attempt " + (attempt + 1) + ")");
            if (attempt < MAX_RETRIES - 1) {
                try {
                    Thread.sleep(200L * (attempt + 1)); // Synchronous sleep before retry
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                return sendRequestWithSyncRetryAsync_(httpRequest, attempt + 1).join(); // Retry synchronously
            } else {
                System.err.println("Request permanently failed after " + MAX_RETRIES + " attempts.");
            }
            return HttpURLConnection.HTTP_INTERNAL_ERROR;
        });
    }
}
