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
    if (!BloomUtils.mightContainSkier(resortId)) {
      return SkierCountResponse.newBuilder().setSkierCount(-1).build();
    }

    String key = config.REDIS_KEY_RESORT_DAY_SKIERS
        .replace("{resort}", String.valueOf(resortId))
        .replace("{season}", seasonId)
        .replace("{day}", String.valueOf(dayId));

    try (Jedis jedis = RedisManager.getJedis()) {
      long count = jedis.exists(key) ? jedis.scard(key) : -1;
      return SkierCountResponse.newBuilder().setSkierCount((int) count).build();
    }
  }

  /**
   * get the total vertical for the skier for the specified ski day
   * provided: resort_id, season_id, day_id, skier_id
   * return total vertical
   */
  public static VerticalIntResponse getUniqueSkierCount(int resortId, String seasonId, int dayId, int skierId) {

    if (!BloomUtils.mightContainSkier(resortId)) {
      return VerticalIntResponse.newBuilder().setTotalVertical(-1).build();
    }

    String key = config.REDIS_KEY_SKIER_DAY_SUMMARY
        .replace("{skier}", String.valueOf(skierId))
        .replace("{resort}", String.valueOf(resortId))
        .replace("{season}", seasonId)
        .replace("{day}", String.valueOf(dayId));

    try (Jedis jedis = RedisManager.getJedis()) {
      String verticalStr = jedis.hget(key, "vertical");
      int vertical = verticalStr == null ? -1 : Integer.parseInt(verticalStr);
      return VerticalIntResponse.newBuilder().setTotalVertical(vertical).build();
    }
  }

  /**
   * get the total vertical for the skier the specified resort. If no season is specified, return all seasons
   * provided: resort_id, season_id, skier_id
   */
  public static VerticalListResponse getTotalVertical(int resortId, String seasonId, int skierId) {

    // if specific season is provided, return total vertical for that season for the skier
    // if no season is provided, return total vertical for all seasons for the skier
    VerticalListResponse.Builder responseBuilder = VerticalListResponse.newBuilder();
    return null;
    //TODO
  }
}
