package ratelimiter;

import io.lettuce.core.ClientOptions;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.ScriptOutputType;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.resource.DefaultClientResources;
import model.Configuration;

public class RedisRateLimiter implements RateLimiter {
    private final RedisClient redisClient;
    private final StatefulRedisConnection<String, String> connection;
    private final RedisCommands<String, String> sync;

    private final String key;
    private final int maxTokens;
    private final int refillRatePerSecond;

    private final String tokenBucketLua = ""
            + "local key = KEYS[1] "
            + "local now = tonumber(ARGV[1]) "
            + "local refill_rate = tonumber(ARGV[2]) "
            + "local max_tokens = tonumber(ARGV[3]) "
            + "local requested = tonumber(ARGV[4]) "
            + "local ttl = tonumber(ARGV[5]) "
            + "local data = redis.call('HMGET', key, 'tokens', 'timestamp') "
            + "local tokens = tonumber(data[1]) "
            + "local last_refill = tonumber(data[2]) "
            + "if tokens == nil then "
            + "  tokens = max_tokens "
            + "  last_refill = now "
            + "end "
            + "local elapsed = now - last_refill "
            + "local refill = math.floor(elapsed * refill_rate / 1000) "
            + "tokens = math.min(tokens + refill, max_tokens) "
            + "if tokens < requested then "
            + "  redis.call('HMSET', key, 'tokens', tokens, 'timestamp', now) "
            + "  redis.call('PEXPIRE', key, ttl) "
            + "  return 0 "
            + "else "
            + "  tokens = tokens - requested "
            + "  redis.call('HMSET', key, 'tokens', tokens, 'timestamp', now) "
            + "  redis.call('PEXPIRE', key, ttl) "
            + "  return 1 "
            + "end";

    public RedisRateLimiter(String host, int port, int maxTokens, int refillRate) {
        RedisURI redisUri = RedisURI.Builder.redis(host, port).build();
        redisClient = RedisClient.create(DefaultClientResources.create(), redisUri);
        redisClient.setOptions(ClientOptions.builder().autoReconnect(true).build());
        this.connection = redisClient.connect();
        this.sync = connection.sync();

        this.key = "rate_limit:write_servlet"; // could be configurable
        this.maxTokens = maxTokens;
        this.refillRatePerSecond = refillRate;
    }

    @Override
    public boolean allowRequest() {
        long now = System.currentTimeMillis();
        String[] keys = { key };
        String[] args = {
                String.valueOf(now),
                String.valueOf(refillRatePerSecond),
                String.valueOf(maxTokens),
                "1",                       // 1 token per request
                "30000"                    // expire in 30s if unused
        };

        Long allowed = sync.eval(tokenBucketLua, ScriptOutputType.INTEGER, keys, args);
        return allowed != null && allowed == 1;
    }

    @Override
    public boolean allowRequestWithRetries(int maxRetries, int maxBackoffMs) {
        for (int attempt = 0; attempt < maxRetries; attempt++) {
            if (allowRequest()) {
                return true;
            }
            try {
                int delayMs = Math.min(100 * (attempt + 1), maxBackoffMs);
                Thread.sleep(delayMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        return false;
    }

    public void close() {
        connection.close();
        redisClient.shutdown();
    }
}
