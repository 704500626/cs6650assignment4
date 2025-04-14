package cacheservice;

import java.util.List;
import model.Configuration;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPooled;
import redis.clients.jedis.Protocol;
import utils.ConfigUtils;

public class RedisManager {
  public static final Configuration config = ConfigUtils.getConfigurationForLiftRideService();

  private static JedisPooled pool = new JedisPooled(config.REDIS_HOST, config.REDIS_PORT);

  public static JedisPooled getPool() {
    return pool;
  }
}
