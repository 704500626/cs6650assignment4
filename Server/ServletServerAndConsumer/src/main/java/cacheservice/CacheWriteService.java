package cacheservice;

import java.util.List;
import model.Configuration;
import model.SkierVertical;
import redis.clients.jedis.Jedis;
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

  public static void writeUniqueSkierCountToCache(int resortId, String seasonId, int dayId, int vertical) {
    return;
    //TODO
  }

  /**
   * This method writes the skier's vertical to the cache.
   * Redis Key: skier:{skier}:{resort}:{season}:{day}
   * Support VerticalIntResponse read method from cache read service
   */
  public static void writeVerticalToCache(int resortId, String seasonId, int dayId, int skierId, SkierVertical vertical) {
    return;
    //TODO
  }

  /**
   * This method writes the skier's vertical list to the cache.
   * Redis Key: skier:{skier}:{resort}
   * Field: SeasonID
   * Value: verticals
   * Support VerticalListResponse read method from cache read service
   */

  public static void writeVerticalListToCache(int skierId, int resortId, String seasonId, List<SkierVertical> verticals) {
    return;
    //TODO
  }

  public static void writeVerticalListToCache(int skierId, int resortId, List<SkierVertical> verticals) {
    return;
    //TODO
  }
}
