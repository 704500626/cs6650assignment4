package old.cacheservice;

import redis.clients.jedis.JedisPooled;

public class BloomUtils {
  private static final String FILTER_NAME = "skier:bf";

  public static boolean mightContainSkier(int skierId) {
    JedisPooled jedis = RedisManager.getPool();
    return jedis.bfExists(FILTER_NAME, String.valueOf(skierId));
  }

  public static void addSkierToFilter(int skierId) {
    JedisPooled jedis = RedisManager.getPool();
    if (!jedis.exists(FILTER_NAME)) {
      jedis.bfReserve(FILTER_NAME, 0.01, 100000);
    }
    jedis.bfAdd(FILTER_NAME, String.valueOf(skierId));
  }
}
