package edu.northeastern.common;

import edu.northeastern.model.Configuration;
import edu.northeastern.model.ConsumerContext;

import java.net.HttpURLConnection;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public abstract class ConsumerThreadAsync extends ConsumerThread{
    public ConsumerThreadAsync(Configuration configuration, ConsumerContext context) {
        super(configuration, context);
    }

    // Asynchronous request with async retry method (shared by both phases)
    protected CompletableFuture<Integer> sendRequestWithRetryAsync(HttpRequest httpRequest, int attempt) {
        return context.httpClient.sendAsync(httpRequest, HttpResponse.BodyHandlers.discarding()).thenCompose(response -> {
            if (response.statusCode() == HttpURLConnection.HTTP_CREATED) {
                return CompletableFuture.completedFuture(response.statusCode());
            } else {
                System.err.println("Received status code: " + response.statusCode() + " on attempt " + (attempt + 1));
                if (attempt < configuration.MAX_RETRIES - 1) {
                    return CompletableFuture.supplyAsync(() -> null, CompletableFuture.delayedExecutor(200L * (attempt + 1), TimeUnit.MILLISECONDS)).thenCompose(v -> sendRequestWithRetryAsync(httpRequest, attempt + 1));
                } else {
                    System.err.println("Request permanently failed after " + configuration.MAX_RETRIES + " attempts.");
                    return CompletableFuture.completedFuture(HttpURLConnection.HTTP_INTERNAL_ERROR);
                }
            }
        }).exceptionally(ex -> {
            System.err.println("Request failed: " + ex.getMessage() + " (attempt " + (attempt + 1) + ")");
            if (attempt < configuration.MAX_RETRIES - 1) {
                try {
                    Thread.sleep(200L * (attempt + 1));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                return sendRequestWithRetryAsync(httpRequest, attempt + 1).join();
            } else {
                System.err.println("Request permanently failed after " + configuration.MAX_RETRIES + " attempts.");
            }
            return HttpURLConnection.HTTP_INTERNAL_ERROR;
        });
    }

    // Asynchronous request with sync retry method (shared by both phases)
    protected CompletableFuture<Integer> sendRequestWithSyncRetryAsync(HttpRequest httpRequest, int attempt) {
        return context.httpClient.sendAsync(httpRequest, HttpResponse.BodyHandlers.discarding()).thenCompose(response -> {
            if (response.statusCode() == HttpURLConnection.HTTP_CREATED) {
                return CompletableFuture.completedFuture(response.statusCode());
            } else {
                System.err.println("Received status code: " + response.statusCode() + " on attempt " + (attempt + 1));
                if (attempt < configuration.MAX_RETRIES - 1) {
                    try {
                        Thread.sleep(200L * (attempt + 1)); // Synchronous sleep before retry
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    return sendRequestWithSyncRetryAsync(httpRequest, attempt + 1); // Retry synchronously
                } else {
                    System.err.println("Request permanently failed after " + configuration.MAX_RETRIES + " attempts.");
                    return CompletableFuture.completedFuture(HttpURLConnection.HTTP_INTERNAL_ERROR);
                }
            }
        }).exceptionally(ex -> {
            System.err.println("Request failed: " + ex.getMessage() + " (attempt " + (attempt + 1) + ")");
            if (attempt < configuration.MAX_RETRIES - 1) {
                try {
                    Thread.sleep(200L * (attempt + 1)); // Synchronous sleep before retry
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                return sendRequestWithSyncRetryAsync(httpRequest, attempt + 1).join(); // Retry synchronously
            } else {
                System.err.println("Request permanently failed after " + configuration.MAX_RETRIES + " attempts.");
            }
            return HttpURLConnection.HTTP_INTERNAL_ERROR;
        });
    }

    protected CompletableFuture<Integer> sendRequestWithSyncRetryAsync_(HttpRequest httpRequest, int attempt) {
        return context.httpClient.sendAsync(httpRequest, HttpResponse.BodyHandlers.discarding()).thenCompose(response -> {
            if (response.statusCode() == HttpURLConnection.HTTP_CREATED) {
                return CompletableFuture.completedFuture(response.statusCode());
            } else {
                System.err.println("Received status code: " + response.statusCode() + " on attempt " + (attempt + 1));
                if (attempt < configuration.MAX_RETRIES - 1) {
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
                    System.err.println("Request permanently failed after " + configuration.MAX_RETRIES + " attempts.");
                    return CompletableFuture.completedFuture(HttpURLConnection.HTTP_INTERNAL_ERROR);
                }
            }
        }).exceptionally(ex -> {
            System.err.println("Request failed: " + ex.getMessage() + " (attempt " + (attempt + 1) + ")");
            if (attempt < configuration.MAX_RETRIES - 1) {
                try {
                    Thread.sleep(200L * (attempt + 1)); // Synchronous sleep before retry
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                return sendRequestWithSyncRetryAsync_(httpRequest, attempt + 1).join(); // Retry synchronously
            } else {
                System.err.println("Request permanently failed after " + configuration.MAX_RETRIES + " attempts.");
            }
            return HttpURLConnection.HTTP_INTERNAL_ERROR;
        });
    }
}
