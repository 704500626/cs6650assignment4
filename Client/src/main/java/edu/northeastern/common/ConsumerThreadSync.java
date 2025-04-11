package edu.northeastern.common;

import edu.northeastern.model.Configuration;
import edu.northeastern.model.ConsumerContext;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public abstract class ConsumerThreadSync extends ConsumerThread{
    public ConsumerThreadSync(Configuration configuration, ConsumerContext context) {
        super(configuration, context);
    }

    // Synchronous request with retry method (shared by both phases)
    protected void sendRequestWithRetry(RandomRequest request) {
        HttpRequest httpRequest = RandomRequest.buildHttpRequestForRandomRequest(request, configuration.serverUrl, context.gson.toJson(request.getLiftRide()));
        int attempts = 0;
        long st = System.currentTimeMillis();
        int statusCode = HttpURLConnection.HTTP_INTERNAL_ERROR;
        try {
            while (attempts < configuration.MAX_RETRIES) {
                try {
                    HttpResponse<String> response = context.httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
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
            if (attempts == configuration.MAX_RETRIES) {
                System.err.println("Request failed after " + configuration.MAX_RETRIES + " attempts.");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            long et = System.currentTimeMillis();
            context.endRequestUpdate(statusCode, st, et - st, configuration);
        }
    }
}
