package cacheservice;

import java.util.List;
import model.Configuration;
import model.SkierVertical;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPooled;
import skierread.SkierReadServiceOuterClass.VerticalRecord;
import utils.ConfigUtils;

public class CacheWriteService {

  /**
   * This method writes the unique skier count to the cache.
   * Redis Key: {resort}:{season}:{day}
   * Support SkierCountResponse read method from cache read service
   */

  public static void writeUniqueSkierCountToCache(int resortId, String seasonId, int dayId, int count) {
    String key = RedisManager.config.REDIS_KEY_UNIQUE_SKIER_COUNT
        .replace("{resort}", String.valueOf(resortId))
        .replace("{season}", seasonId)
        .replace("{day}", String.valueOf(dayId));

    JedisPooled jedis = RedisManager.getPool();
    String status = jedis.set(key, String.valueOf(count));
    System.out.println("Redis SET status: " + status);
  }

  /**
   * This method writes the skier's vertical to the cache.
   * Redis Key: skier:{skier}:{resort}:{season}:{day}
   * Support VerticalIntResponse read method from cache read service
   */
  public static void writeVerticalToCache(int resortId, String seasonId, int dayId, int skierId, int vertical) {
    String key = RedisManager.config.REDIS_KEY_VERTICAL_WITH_SKIER
        .replace("{skier}", String.valueOf(skierId))
        .replace("{resort}", String.valueOf(resortId))
        .replace("{season}", seasonId)
        .replace("{day}", String.valueOf(dayId));

    JedisPooled jedis = RedisManager.getPool();
    jedis.set(key, String.valueOf(vertical));
  }

  /**
   * This method writes the skier's vertical list to the cache.
   * Field: SeasonID
   * Value: verticals
   * Support VerticalListResponse read method from cache read service
   */

  public static void writeVerticalListToCache(int skierId, int resortId, List<VerticalRecord> verticals) {
    String key = RedisManager.config.REDIS_KEY_VERTICAL_COUNT
        .replace("{skier}", String.valueOf(skierId))
        .replace("{resort}", String.valueOf(resortId));

    JedisPooled jedis = RedisManager.getPool();
    for (VerticalRecord vertical : verticals) {
      jedis.hset(key, vertical.getSeasonID(), String.valueOf(vertical.getTotalVertical()));
    }
  }
}
