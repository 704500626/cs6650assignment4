package bloomfilter;

import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnels;
import model.Configuration;

import java.nio.charset.StandardCharsets;

public class LiftRideBloomFilter {
    private volatile BloomFilter<String> uniqueSkiersFilter;
    private volatile BloomFilter<String> dailyVerticalFilter;
    private volatile BloomFilter<String> seasonVerticalFilter;
    private volatile BloomFilter<String> totalVerticalFilter;

    public LiftRideBloomFilter(Configuration config) {
        uniqueSkiersFilter = BloomFilter.create(Funnels.stringFunnel(StandardCharsets.UTF_8), config.BLOOM_FILTER_CAPACITY, config.BLOOM_FILTER_ERROR_RATE);
        dailyVerticalFilter = BloomFilter.create(Funnels.stringFunnel(StandardCharsets.UTF_8), config.BLOOM_FILTER_CAPACITY, config.BLOOM_FILTER_ERROR_RATE);
        seasonVerticalFilter = BloomFilter.create(Funnels.stringFunnel(StandardCharsets.UTF_8), config.BLOOM_FILTER_CAPACITY, config.BLOOM_FILTER_ERROR_RATE);
        totalVerticalFilter = BloomFilter.create(Funnels.stringFunnel(StandardCharsets.UTF_8), config.BLOOM_FILTER_CAPACITY, config.BLOOM_FILTER_ERROR_RATE);
    }

    public LiftRideBloomFilter(BloomFilter<String> uniqueSkiersFilter, BloomFilter<String> dailyVerticalFilter, BloomFilter<String> seasonVerticalFilter, BloomFilter<String> totalVerticalFilter) {
        this.uniqueSkiersFilter = uniqueSkiersFilter;
        this.dailyVerticalFilter = dailyVerticalFilter;
        this.seasonVerticalFilter = seasonVerticalFilter;
        this.totalVerticalFilter = totalVerticalFilter;
    }

    public BloomFilter<String> getUniqueSkiersFilter() {
        return uniqueSkiersFilter;
    }

    public void setUniqueSkiersFilter(BloomFilter<String> uniqueSkiersFilter) {
        this.uniqueSkiersFilter = uniqueSkiersFilter;
    }

    public BloomFilter<String> getDailyVerticalFilter() {
        return dailyVerticalFilter;
    }

    public void setDailyVerticalFilter(BloomFilter<String> dailyVerticalFilter) {
        this.dailyVerticalFilter = dailyVerticalFilter;
    }

    public BloomFilter<String> getSeasonVerticalFilter() {
        return seasonVerticalFilter;
    }

    public void setSeasonVerticalFilter(BloomFilter<String> seasonVerticalFilter) {
        this.seasonVerticalFilter = seasonVerticalFilter;
    }

    public BloomFilter<String> getTotalVerticalFilter() {
        return totalVerticalFilter;
    }

    public void setTotalVerticalFilter(BloomFilter<String> totalVerticalFilter) {
        this.totalVerticalFilter = totalVerticalFilter;
    }
}
