package old;

import cache.RedisCacheClient;
import model.Configuration;
import utils.ConfigUtils;

public class BloomFilterSeeder {
    public static void main(String[] args) {
        Configuration config = ConfigUtils.getConfigurationForService();
        seeding(config);
    }

    public static void seeding(Configuration config) {
        RedisCacheClient cacheClient = new RedisCacheClient(config);
        cacheClient.createFilter(config.REDIS_BLOOM_FILTER_UNIQUE_SKIERS, config.BLOOM_FILTER_CAPACITY, config.BLOOM_FILTER_ERROR_RATE);
        cacheClient.createFilter(config.REDIS_BLOOM_FILTER_DAILY_VERTICAL, config.BLOOM_FILTER_CAPACITY, config.BLOOM_FILTER_ERROR_RATE);
        cacheClient.createFilter(config.REDIS_BLOOM_FILTER_SINGLE_SEASON_VERTICAL, config.BLOOM_FILTER_CAPACITY, config.BLOOM_FILTER_ERROR_RATE);
        cacheClient.createFilter(config.REDIS_BLOOM_FILTER_ALL_SEASON_VERTICALS, config.BLOOM_FILTER_CAPACITY, config.BLOOM_FILTER_ERROR_RATE);
        cacheClient.close();
    }
}
