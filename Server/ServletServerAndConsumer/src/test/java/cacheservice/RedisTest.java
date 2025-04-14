package cacheservice;

import skierread.SkierReadServiceOuterClass.SkierCountResponse;
import skierread.SkierReadServiceOuterClass.VerticalIntResponse;
import skierread.SkierReadServiceOuterClass.VerticalListResponse;

public class RedisTest {

  public static void main(String[] args) {

      //read service: getUniqueSkierCountFromCache(int resortId, String seasonId, int dayId)
      int resortId = 42;
      String seasonId = "2025";
      int dayId = 10;

      CacheReadService cacheReadService = new CacheReadService();
      CacheReadService.getUniqueSkierCountFromCache(resortId, seasonId, dayId);
      System.out.println("Value in Redis:" + CacheReadService.getUniqueSkierCountFromCache(resortId, seasonId, dayId).getSkierCount());
      if (CacheReadService.getUniqueSkierCountFromCache(resortId, seasonId, dayId).getSkierCount() != 500) {
          System.out.println("Error: Value in Redis is not 500");
      } else {
          System.out.println("Pass test!");
      }

      //read service: getUniqueSkierCountFromCache(int resortId, String seasonId, int dayId), test null
      int resortId1 = 41;
      SkierCountResponse skierCountResponse = CacheReadService.getUniqueSkierCountFromCache(resortId1, seasonId, dayId);
      if (skierCountResponse == null) {
          System.out.println("Pass test!");
      } else {
          System.out.println("Error: Value in Redis is not null");
      }


      //read service: getTotalVerticalOfSkierFromCache(int resortId, String seasonId, int dayId, int skierId)
      int skierId = 1000;
      VerticalIntResponse skierCountResponse1 = CacheReadService.getTotalVerticalOfSkierFromCache(resortId, seasonId, dayId, skierId);
      System.out.println("Value in Redis:" + skierCountResponse1.getTotalVertical());

      if (skierCountResponse1.getTotalVertical() != 1000) {
          System.out.println("Error: Value in Redis is not 1000");
      } else {
          System.out.println("Pass test!");
      }

      //read service: getTotalVerticalOfSkierFromCache(int resortId, String seasonId, int dayId, int skierId), null test
      int skierId1 = 900;
      VerticalIntResponse skierCountResponse2 = CacheReadService.getTotalVerticalOfSkierFromCache(resortId1, seasonId, dayId, skierId);
      System.out.println("Value in Redis:" + skierCountResponse1.getTotalVertical());
      if (skierCountResponse2 == null) {
          System.out.println("Pass test!");
      } else {
          System.out.println("Error: Value in Redis is not null");
      }

      //getTotalVerticalFromCache(int resortId, String seasonId, int skierId), season
      int skierId2 = 7;
      VerticalListResponse skierCountResponse3 = CacheReadService.getTotalVerticalFromCache(resortId, seasonId, skierId2);
      for (int i = 0; i < skierCountResponse3.getRecordsCount(); i++) {
          System.out.println("Value in Redis:" + skierCountResponse3.getRecords(i).getTotalVertical());
      }

      String seasonId1 = "2023";
      VerticalListResponse skierCountResponse4 = CacheReadService.getTotalVerticalFromCache(resortId, seasonId1, skierId2);
      for (int i = 0; i < skierCountResponse4.getRecordsCount(); i++) {
          System.out.println("Value in Redis:" + skierCountResponse4.getRecords(i).getTotalVertical());
      }

      int skierId3 = 8;
      VerticalListResponse skierCountResponse5 = CacheReadService.getTotalVerticalFromCache(resortId, seasonId, skierId3);
      if (skierCountResponse5 == null) {
          System.out.println("Pass test!");
      } else {
          System.out.println("Error: Value in Redis is not null");
      }

      //getTotalVerticalFromCache(int resortId, String seasonId, int skierId), season not provided

      VerticalListResponse skierCountResponse6 = CacheReadService.getTotalVerticalFromCache(resortId, "", skierId2);
      for (int i = 0; i < skierCountResponse6.getRecordsCount(); i++) {
          System.out.println("Value in Redis:" + skierCountResponse6.getRecords(i).getTotalVertical());
      }
      if (skierCountResponse6.getRecordsCount() != 2) {
          System.out.println("Error: Value in Redis is not 2");
      } else {
          System.out.println("Pass test!");
      }
  }
}
