package cacheservice;

import java.util.List;
import skierread.SkierReadServiceOuterClass.VerticalRecord;

public class RedisWriteTest {

  public static void main(String[] args) {
    //  writeUniqueSkierCountToCache(int resortId, String seasonId, int dayId, int count)

    CacheWriteService cacheWriteService = new CacheWriteService();
    int resortId = 50;
    String seasonId = "2025";
    int dayId = 10;
    int count = 500;
    CacheWriteService.writeUniqueSkierCountToCache(resortId, seasonId, dayId, count);

    //  writeVerticalToCache(int resortId, String seasonId, int dayId, int skierId, int vertical)
    int skierId = 888;
    int vertical = 8888;
    String seasonId1 = "2026";
    int dayId1 = 11;
    int resortId1 = 88;
    CacheWriteService.writeVerticalToCache(resortId1, seasonId1, dayId1, skierId, vertical);

    //    writeVerticalListToCache(int skierId, int resortId, List<VerticalRecord > verticals)

    int skierId2 = 3;
    int resortId2 = 3;
    String seasonId2 = "2027";
    VerticalRecord.Builder verticalRecordBuilder = VerticalRecord.newBuilder();
    verticalRecordBuilder.setSeasonID(seasonId2);
    verticalRecordBuilder.setTotalVertical(3333);

    VerticalRecord.Builder verticalRecordBuilder1 = VerticalRecord.newBuilder();
    verticalRecordBuilder1.setSeasonID("2028");
    verticalRecordBuilder1.setTotalVertical(4444);


    List<VerticalRecord> verticalRecords = List.of(verticalRecordBuilder.build(), verticalRecordBuilder1.build());
    CacheWriteService.writeVerticalListToCache(skierId2, resortId2, verticalRecords);
  }
}
