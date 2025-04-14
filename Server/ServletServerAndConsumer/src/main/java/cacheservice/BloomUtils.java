package cacheservice;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.Protocol;

public class BloomUtils {

  public static boolean mightContainSkier(int skierId) {
    try (Jedis jedis = RedisManager.getJedis()) {
      String result = (String)jedis.sendCommand(Protocol.Command.valueOf("BF.EXISTS"), "skier:bf", String.valueOf(skierId));
      return "1".equals(result);
    }
  }

  public static void addSkierToFilter(int skierId) {
    try (Jedis jedis = RedisManager.getJedis()) {
      jedis.sendCommand(Protocol.Command.valueOf("BF.ADD"), "skier:bf", String.valueOf(skierId));
    }
  }

  public static void initSkierFilterIfNeeded() {
    try (Jedis jedis = RedisManager.getJedis()) {
      // Reserve only if not created (BF.RESERVE fails if already exists)
      String result = (String)jedis.sendCommand(Protocol.Command.valueOf("BF.RESERVE"), "skier:bf", "0.01", "100000");
      System.out.println("Bloom filter reserved: " + result);
    } catch (Exception e) {
      System.out.println("Bloom filter likely already exists.");
    }
  }
}
