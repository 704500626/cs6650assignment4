package ratelimiter;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Token Bucket Rate Limiter Implementation.
 */
public class TokenBucketRateLimiter implements RateLimiter {
    private final int maxTokens; // Maximum number of tokens in the bucket
    private final int refillRatePerSecond; // Number of tokens replenished per second
    private final AtomicLong lastRefillTime; // Timestamp of the last token refill
    private final AtomicInteger availableTokens; // Current number of available tokens

    public TokenBucketRateLimiter(int maxTokens, int refillRatePerSecond) {
        this.maxTokens = maxTokens;
        this.refillRatePerSecond = refillRatePerSecond;
        this.lastRefillTime = new AtomicLong(System.currentTimeMillis());
        this.availableTokens = new AtomicInteger(maxTokens); // Start with a full bucket
    }

    /**
     * Refill tokens in the bucket based on the elapsed time.
     */
    private void refillTokens() {
        long now = System.currentTimeMillis();
        long elapsedTime = now - lastRefillTime.get();
        int refillTokens = (int) ((elapsedTime / 1000.0) * refillRatePerSecond);

        if (refillTokens > 0) {
            int newTokenCount = Math.min(maxTokens, availableTokens.get() + refillTokens);
            availableTokens.set(newTokenCount);
            lastRefillTime.set(now);
        }
    }

    @Override
    public boolean allowRequest() {
        refillTokens();
        if (availableTokens.get() > 0) {
            availableTokens.decrementAndGet();
            return true;
        }
        return false; // No tokens available, reject the request
    }

    @Override
    public boolean allowRequestWithRetries(int maxRetries, int maxBackoffMs) {
        for (int attempt = 0; attempt < maxRetries; attempt++) {
            if (allowRequest()) {
                return true;
            }
            try {
                // Apply exponential backoff: 100ms, 200ms, 400ms, up to maxBackoffMs
                int delayMs = Math.min(100 * (attempt + 1), maxBackoffMs);
                Thread.sleep(delayMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt(); // Restore the interrupt
                return false;
            }
        }
        return false; // All retries exhausted, request denied
    }
}