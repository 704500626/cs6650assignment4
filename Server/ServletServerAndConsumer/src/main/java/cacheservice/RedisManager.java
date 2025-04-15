package cacheservice;

import model.Configuration;
import redis.clients.jedis.JedisPooled;
import utils.ConfigUtils;

public class RedisManager {
  public static final Configuration config = ConfigUtils.getConfigurationForService();

  private static JedisPooled pool = new JedisPooled(config.REDIS_HOST, config.REDIS_PORT);

  public static JedisPooled getPool() {
    return pool;
  }
}
