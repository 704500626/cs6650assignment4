package cacheservice;

import model.Configuration;
import redis.clients.jedis.Jedis;
import utils.ConfigUtils;

public class CacheWriteService {
  private static final Configuration config = ConfigUtils.getConfigurationForLiftRideService();

  /*
   * This method retrieves the number of unique skiers for a given resort, season, and day.
   * provided: resort_id, season_id, day_id
   * return number of unique skiers
   */
  public static int getUniqueSkierCountFromCache(int resortId, String seasonId, int dayId) {
    return 0;
  }

  /*
   * get the total vertical for the skier for the specified ski day
   * provided: resort_id, season_id, day_id, skier_id
   * return total vertical
   */
  public static int getUniqueSkierCount(int resortId, String seasonId, int dayId, int skierId) {
    return 0;
  }

  /*
   * get the total vertical for the skier the specified resort. If no season is specified, return all seasons
   * provided: resort_id, season_id, skier_id
   * return total vertical
   */
  public static int getTotalVertical(int resortId, String seasonId, int skierId) {
    return 0;
  }

}
