package ratelimiter;

public interface RateLimiter {
    /**
     * Checks whether the request is allowed based on the rate limiting strategy.
     *
     * @return true if the request is allowed, false otherwise.
     */
    boolean allowRequest();

    /**
     * Tries to allow a request with retry logic.
     *
     * @param maxRetries Number of maximum retries before giving up.
     * @param maxBackoffMs Maximum backoff delay in milliseconds.
     * @return true if the request is eventually allowed, false if all retries fail.
     */
    boolean allowRequestWithRetries(int maxRetries, int maxBackoffMs);
}
