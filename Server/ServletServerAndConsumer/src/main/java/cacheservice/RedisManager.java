package cacheservice;

import java.util.List;
import model.Configuration;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPooled;
import redis.clients.jedis.Protocol;

public class RedisManager {
  private static JedisPooled pool;

  public static void init(Configuration config) {
    pool = new JedisPooled(config.REDIS_HOST, config.REDIS_PORT);
  }

  public static JedisPooled getPool() {
    return pool;
  }
}
