package cacheservice;

import model.Configuration;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

public class RedisManager {
  private static JedisPool pool;

  public static void init(Configuration config) {
    if (pool == null) {
      pool = new JedisPool(config.REDIS_HOST, config.getREDIS_PORT());
    }
  }

  public static Jedis getJedis() {
    if (pool == null) {
      throw new IllegalStateException("RedisManager not initialized. Call init() first.");
    }
    return pool.getResource();
  }
}
