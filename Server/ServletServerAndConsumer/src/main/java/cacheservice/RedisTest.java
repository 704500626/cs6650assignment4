import cacheservice.BloomUtils;
import cacheservice.CacheReadService;
import model.Configuration;
import redis.clients.jedis.Jedis;
import cacheservice.RedisManager;
import skierread.SkierReadServiceOuterClass.SkierCountResponse;
import skierread.SkierReadServiceOuterClass.VerticalIntResponse;
import skierread.SkierReadServiceOuterClass.VerticalListResponse;
import skierread.SkierReadServiceOuterClass.VerticalRecord;
import utils.ConfigUtils;

public class RedisTest {

  public static void main(String[] args) {
//    Configuration config = ConfigUtils.getConfigurationForLiftRideService();
//
//    RedisManager.init(config);

    CacheReadService cacheReadService = new CacheReadService();

    BloomUtils.addSkierToFilter(7);

    int resortId = 42;
    int skierId = 7;
    String seasonId = "2023";

    VerticalListResponse verticalListResponse = CacheReadService.getTotalVerticalFromCache(resortId, seasonId, skierId);

    if (verticalListResponse != null) {
      System.out.println("VerticalListResponse is not null");
      System.out.println("Record count: " + verticalListResponse.getRecordsList().size());
      for (VerticalRecord record : verticalListResponse.getRecordsList()) {
        System.out.println("Season: " + record.getSeasonID() + ", Total Vertical: " + record.getTotalVertical());
      }
    } else {
      System.out.println("VerticalListResponse is null");
    }
  }
}

