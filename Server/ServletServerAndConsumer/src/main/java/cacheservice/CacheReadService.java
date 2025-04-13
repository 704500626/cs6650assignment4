package cacheservice;

import java.util.List;
import model.Configuration;
import redis.clients.jedis.Jedis;
import skierread.SkierReadServiceOuterClass.SkierCountResponse;
import skierread.SkierReadServiceOuterClass.VerticalIntResponse;
import skierread.SkierReadServiceOuterClass.VerticalListResponse;
import software.amazon.awssdk.services.dynamodb.endpoints.internal.Value.None;
import utils.ConfigUtils;

public class CacheReadService {
  private static final Configuration config = ConfigUtils.getConfigurationForLiftRideService();

  /**
   * This method retrieves the number of unique skiers for a given resort, season, and day.
   * provided: resort_id, season_id, day_id
   *
   */
  public static SkierCountResponse getUniqueSkierCountFromCache(int resortId, String seasonId, int dayId) {
    return null;
    // TODO
  }

  /**
   * get the total vertical for the skier for the specified ski day
   * provided: resort_id, season_id, day_id, skier_id
   * return total vertical
   */
  public static VerticalIntResponse getUniqueSkierCount(int resortId, String seasonId, int dayId, int skierId) {
    return null;
    // TODO
  }

  /**
   * get the total vertical for the skier the specified resort. If no season is specified, return all seasons
   * provided: resort_id, season_id, skier_id
   */
  public static VerticalListResponse getTotalVertical(int resortId, String seasonId, int skierId) {
    return null;
    // TODO
  }
}
