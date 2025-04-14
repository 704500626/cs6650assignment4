package cacheservice;

import java.util.List;
import model.Configuration;
import redis.clients.jedis.Jedis;
import skierread.SkierReadServiceOuterClass.SkierCountResponse;
import skierread.SkierReadServiceOuterClass.VerticalIntResponse;
import skierread.SkierReadServiceOuterClass.VerticalListResponse;
import utils.ConfigUtils;

public class CacheReadService {
  private static final Configuration config = ConfigUtils.getConfigurationForLiftRideService();

  public CacheReadService() {
    // Initialize Redis connection pool
    RedisManager.init(config);
  }

  /**
   * This method retrieves the number of unique skiers for a given resort, season, and day.
   * provided: resort_id, season_id, day_id
   *
   */
  public static SkierCountResponse getUniqueSkierCountFromCache(int resortId, String seasonId, int dayId) {
    String key = config.REDIS_KEY_UNIQUE_SKIER_COUNT
        .replace("{resort}", String.valueOf(resortId))
        .replace("{season}", seasonId)
        .replace("{day}", String.valueOf(dayId));

    try (Jedis jedis = RedisManager.getJedis()) {

      if (!jedis.exists(key)) {
        return null;
      }
      long count = jedis.scard(key);
      return SkierCountResponse.newBuilder().setSkierCount((int) count).build();
    }
  }

  /**
   * get the total vertical for the skier for the specified ski day
   * provided: resort_id, season_id, day_id, skier_id
   * return total vertical
   */
  public static VerticalIntResponse getTotalVerticalOfSkierFromCache(int resortId, String seasonId, int dayId, int skierId) {
    if (!BloomUtils.mightContainSkier(skierId)) {
      return VerticalIntResponse.newBuilder().setTotalVertical(-1).build();
    }

    String key = config.REDIS_KEY_VERTICAL_WITH_SKIER
        .replace("{skier}", String.valueOf(skierId))
        .replace("{resort}", String.valueOf(resortId))
        .replace("{season}", seasonId)
        .replace("{day}", String.valueOf(dayId));

    try (Jedis jedis = RedisManager.getJedis()) {
      String verticalStr = jedis.hget(key, "vertical");

      if (verticalStr == null) {
        return null;
      }
      int vertical = Integer.parseInt(verticalStr);
      return VerticalIntResponse.newBuilder().setTotalVertical(vertical).build();
    }
  }

  /**
   * get the total vertical for the skier the specified resort. If no season is specified, return all seasons
   * provided: resort_id, season_id, skier_id
   */
  public static VerticalListResponse getTotalVerticalFromCache(int resortId, String seasonId, int skierId) {

    // if specific season is provided, return total vertical for that season for the skier
    // if no season is provided, return total vertical for all seasons for the skier
    VerticalListResponse.Builder responseBuilder = VerticalListResponse.newBuilder();
    return null;
    //TODO
  }

  /**
   * get the total vertical for the skier the specified resort. If no season is specified, return all seasons
   * provided: resort_id, season_id, skier_id
   */
  public static VerticalListResponse getTotalVerticalFromCache(int resortId, int skierId) {

    // if specific season is provided, return total vertical for that season for the skier
    // if no season is provided, return total vertical for all seasons for the skier
    VerticalListResponse.Builder responseBuilder = VerticalListResponse.newBuilder();
    return null;
    //TODO
  }
}
