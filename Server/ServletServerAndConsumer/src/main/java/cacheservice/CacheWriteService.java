package cacheservice;

import java.util.List;
import model.Configuration;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPooled;
import skierread.SkierReadServiceOuterClass.VerticalRecord;
import utils.ConfigUtils;

public class CacheWriteService {
  private static final Configuration config = ConfigUtils.getConfigurationForLiftRideService();

  public CacheWriteService() {
    // Initialize Redis connection pool
    RedisManager.init(config);
  }

  /**
   * This method writes the unique skier count to the cache.
   * Redis Key: {resort}:{season}:{day}
   * Support SkierCountResponse read method from cache read service
   */

  public static void writeUniqueSkierCountToCache(int resortId, String seasonId, int dayId, int count) {
    String key = config.REDIS_KEY_UNIQUE_SKIER_COUNT
        .replace("{resort}", String.valueOf(resortId))
        .replace("{season}", seasonId)
        .replace("{day}", String.valueOf(dayId));

    try (JedisPooled jedis = RedisManager.getPool()) {
      jedis.set(key, String.valueOf(count));
    }
  }

  /**
   * This method writes the skier's vertical to the cache.
   * Redis Key: skier:{skier}:{resort}:{season}:{day}
   * Support VerticalIntResponse read method from cache read service
   */
  public static void writeVerticalToCache(int resortId, String seasonId, int dayId, int skierId, int vertical) {
    if (!BloomUtils.mightContainSkier(skierId)) {
      BloomUtils.addSkierToFilter(skierId);
    }

    String key = config.REDIS_KEY_VERTICAL_WITH_SKIER
        .replace("{skier}", String.valueOf(skierId))
        .replace("{resort}", String.valueOf(resortId))
        .replace("{season}", seasonId)
        .replace("{day}", String.valueOf(dayId));

    try (JedisPooled jedis = RedisManager.getPool()) {
      jedis.set(key, String.valueOf(vertical));
    }
  }

  /**
   * This method writes the skier's vertical list to the cache.
   * Redis Key: total:{resort}:{season}:{skier}
   * Field: SeasonID
   * Value: verticals
   * Support VerticalListResponse read method from cache read service
   */

  public static void writeVerticalListToCache(int skierId, int resortId, List<VerticalRecord> verticals) {
    String key = config.REDIS_KEY_VERTICAL_COUNT
        .replace("{skier}", String.valueOf(skierId))
        .replace("{resort}", String.valueOf(resortId));

    try (JedisPooled jedis = RedisManager.getPool()) {
      for (VerticalRecord vertical : verticals) {
        jedis.hset(key, vertical.getSeasonID(), String.valueOf(vertical.getTotalVertical()));
      }
    }
  }
}
