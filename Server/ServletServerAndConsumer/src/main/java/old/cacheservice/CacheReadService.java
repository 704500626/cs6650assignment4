package old.cacheservice;

import java.util.Map;

import grpc.*;
import grpc.LiftRideReadProto.*;
import redis.clients.jedis.JedisPooled;

public class CacheReadService {

  /**
   * This method retrieves the number of unique skiers for a given resort, season, and day.
   * provided: resort_id, season_id, day_id
   *
   */
  public static SkierCountResponse getUniqueSkierCountFromCache(int resortId, String seasonId, int dayId) {
    String key = RedisManager.config.REDIS_KEY_UNIQUE_SKIER_COUNT
        .replace("{resort}", String.valueOf(resortId))
        .replace("{season}", seasonId)
        .replace("{day}", String.valueOf(dayId));

    JedisPooled jedis = RedisManager.getPool();

    if (!jedis.exists(key)) {
      return null;
    }
    long count = Long.parseLong(jedis.get(key));
    return LiftRideReadProto.SkierCountResponse.newBuilder().setSkierCount((int) count).build();
  }

  /**
   * get the total vertical for the skier for the specified ski day
   * provided: resort_id, season_id, day_id, skier_id
   * return total vertical
   */
  public static VerticalIntResponse getTotalVerticalOfSkierFromCache(int resortId, String seasonId, int dayId, int skierId) {

    String key = RedisManager.config.REDIS_KEY_VERTICAL_WITH_SKIER
        .replace("{skier}", String.valueOf(skierId))
        .replace("{resort}", String.valueOf(resortId))
        .replace("{season}", seasonId)
        .replace("{day}", String.valueOf(dayId));

    JedisPooled jedis = RedisManager.getPool();
    if (!jedis.exists(key)) {
      return null;
    }
    String verticalStr = jedis.get(key);
    long  vertical = Long.parseLong(verticalStr);
    return VerticalIntResponse.newBuilder().setTotalVertical((int)vertical).build();
  }

  /**
   * get the total vertical for the skier the specified resort. If no season is specified, return all seasons
   * provided: resort_id, season_id, skier_id
   */
  public static VerticalListResponse getTotalVerticalFromCache(int resortId, String seasonId, int skierId) {

    // if specific season is provided, return total vertical for that season for the skier
    // if no season is provided, return total vertical for all seasons for the skier


    String key = RedisManager.config.REDIS_KEY_VERTICAL_COUNT
        .replace("{skier}", String.valueOf(skierId))
        .replace("{resort}", String.valueOf(resortId))
        .replace("{season}", seasonId);

    VerticalListResponse.Builder response = VerticalListResponse.newBuilder();
    JedisPooled jedis = RedisManager.getPool();

    if (!jedis.exists(key)) {
      return null;
    }

    Map<String, String> verticals = jedis.hgetAll(key);
    for (Map.Entry<String, String> entry : verticals.entrySet()) {
      String seasonIdKey = entry.getKey();
      String verticalStr = entry.getValue();
      long vertical = Long.parseLong(verticalStr);
      VerticalRecord record = VerticalRecord.newBuilder()
          .setSeasonID(seasonIdKey)
          .setTotalVertical((int) vertical)
          .build();
      response.addRecords(record);
    }
  return response.build();
  }
}
