package ratelimiter;


import ratelimiter.algorithms.TokenBucketRateLimiter;

public class RateLimiterFactory {
    public static RateLimiter create(String mode, String host, String group_id, int port, int maxTokens, int refillRate) {
        switch (mode.toUpperCase()) {
            case "REMOTE":
                return new RemoteRateLimiter(
                        host,
                        port,
                        group_id
                );
            case "REDIS":
                return new RedisRateLimiter(host, port, maxTokens, refillRate); // You'll implement this next
            case "LOCAL":
            default:
                return new TokenBucketRateLimiter(maxTokens, refillRate);
        }
    }
}
