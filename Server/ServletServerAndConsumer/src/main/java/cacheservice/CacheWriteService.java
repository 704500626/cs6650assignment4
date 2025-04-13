package cacheservice;

import model.Configuration;
import redis.clients.jedis.Jedis;
import utils.ConfigUtils;

public class CacheWriteService {
  private static final Configuration config = ConfigUtils.getConfigurationForLiftRideService();

  /**
    * This method writes the skier day summary to the cache.
    * Redis Key: summary:{skier}:{resort}:{season}:{day} (HASH)
    * Fields: vertical += liftId * 10, rides += 1
    * Supports:
    * CacheReadService.getUniqueSkierCount(int resortId, String seasonId, int dayId, int skierId)
    * CacheReadService.getTotalVertical(int resortId, String seasonId, int skierId)
   */

  public static void writeSkierDaySummary(int skierId, int resortId, String seasonId, int dayId, int liftId) {
    return;
    //TODO
  }
  /**
    * This method writes the skier to resort day to the cache.
    * Redis Key: resort:{resort}:{season}:{day}:skiers (SET)
    * This one support getUniqueSkierCountFromCache method from cache read service
   */
  public static void writeSkierToResortDay(int skierId, int resortId, String seasonId, int dayId) {
    return;
    //TODO
  }

  /**
   * This method writes the skier's season total vertical to the cache.
   * Redis Key: total:{skier}:{resort}:{season} (STRING or HASH)
   * Value: incremented by liftId * 10
   * Supports:
   * CacheReadService.getTotalVertical(...) if it is implemented to read pre-aggregated totals.
   */

  public static void writeSkierSeasonTotal(int skierId, int resortId, String seasonId, int liftId) {
    return;
    //TODO
  }
}
