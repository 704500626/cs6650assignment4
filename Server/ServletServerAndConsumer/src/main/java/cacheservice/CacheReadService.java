package cacheservice;

import java.util.Map;
import model.Configuration;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPooled;
import skierread.SkierReadServiceOuterClass.SkierCountResponse;
import skierread.SkierReadServiceOuterClass.VerticalIntResponse;
import skierread.SkierReadServiceOuterClass.VerticalListResponse;
import skierread.SkierReadServiceOuterClass.VerticalRecord;
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

    try (JedisPooled jedis = RedisManager.getPool()) {

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

    String key = config.REDIS_KEY_VERTICAL_WITH_SKIER
        .replace("{skier}", String.valueOf(skierId))
        .replace("{resort}", String.valueOf(resortId))
        .replace("{season}", seasonId)
        .replace("{day}", String.valueOf(dayId));

    try (JedisPooled jedis = RedisManager.getPool()) {
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
//    if (!BloomUtils.mightContainSkier(skierId)) {
//      return null;
//    }

    String key = config.REDIS_KEY_VERTICAL_COUNT
        .replace("{skier}", String.valueOf(skierId))
        .replace("{resort}", String.valueOf(resortId));

    System.out.println("Generated Redis Key: " + key);

    VerticalListResponse.Builder response = VerticalListResponse.newBuilder();
    try (JedisPooled jedis = RedisManager.getPool()) {
      if (seasonId == null || seasonId.isEmpty()) {
        // Return total verticals for all seasons (all fields in the hash)
        Map<String, String> allSeasonVerticals = jedis.hgetAll(key);
        for (Map.Entry<String, String> entry : allSeasonVerticals.entrySet()) {
          response.addRecords(
              VerticalRecord.newBuilder()
                  .setSeasonID(entry.getKey())
                  .setTotalVertical(Integer.parseInt(entry.getValue()))
                  .build()
          );
        }
      } else {
        // Return only the vertical for the specific season
        String verticalStr = jedis.hget(key, seasonId);
        if (verticalStr != null) {
          response.addRecords(
              VerticalRecord.newBuilder()
                  .setSeasonID(seasonId)
                  .setTotalVertical(Integer.parseInt(verticalStr))
                  .build()
          );
        }
      }
    }
    return response.build();
  }
}
