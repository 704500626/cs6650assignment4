package batch;

import cache.RedisCacheClient;
import com.google.gson.Gson;
import dao.LiftRideReader;
import grpc.LiftRideReadProto;
import model.Configuration;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

// TODO I think we can optimize this one, instead of separate key reading and updating
public class RefreshExistingCache implements AggregationStrategy {
    private final Configuration config;
    private final RedisCacheClient cache;
    private final LiftRideReader dbReader;
    private final Gson gson = new Gson();

    public RefreshExistingCache(Configuration config, RedisCacheClient cache) {
        this.config = config;
        this.cache = cache;
        this.dbReader = new LiftRideReader(config);
    }

    @Override
    public void run() throws SQLException {
        System.out.println("[RefreshExistingCache] Refreshing hot Redis cache entries...");
        refreshUniqueSkiers();
        refreshDailyVerticals();
        refreshSingleSeasonVerticals();
        refreshAllSeasonVerticals();
        System.out.println("[RefreshExistingCache] Hot keys refreshed.");
    }

    @Override
    public void close() {
        dbReader.close();
    }

    private void refreshUniqueSkiers() throws SQLException {
        Set<String> keys = cache.scanKeys(config.AGGREGATION_HOT_KEY_UNIQUE_SKIERS);
        Map<String, String> keyValues = new HashMap<>();
        for (String key : keys) {
            String[] parts = key.split(":");
            int resortId = Integer.parseInt(parts[1]);
            String seasonId = parts[3];
            int dayId = Integer.parseInt(parts[5]);
            int count = dbReader.getResortUniqueSkiers(resortId, seasonId, dayId);
            keyValues.put(key, String.valueOf(count));
        }
        if (!keyValues.isEmpty()) {
            cache.getSync().mset(keyValues);
        }
    }

    private void refreshDailyVerticals() throws SQLException {
        Set<String> keys = cache.scanKeys(config.AGGREGATION_HOT_KEY_DAILY_VERTICAL);
        Map<String, String> keyValues = new HashMap<>();
        for (String key : keys) {
            String[] parts = key.split(":");
            int resortId = Integer.parseInt(parts[1]);
            String seasonId = parts[3];
            int dayId = Integer.parseInt(parts[5]);
            int skierId = Integer.parseInt(parts[7]);
            int vertical = dbReader.getSkierDayVertical(resortId, seasonId, dayId, skierId);
            keyValues.put(key, String.valueOf(vertical));
        }
        if (!keyValues.isEmpty()) {
            cache.getSync().mset(keyValues);
        }
    }

    private void refreshSingleSeasonVerticals() throws SQLException {
        Set<String> keys = cache.scanKeys(config.AGGREGATION_HOT_KEY_SINGLE_SEASON_VERTICAL);
        Map<String, String> keyValues = new HashMap<>();
        for (String key : keys) {
            String[] parts = key.split(":");
            int skierId = Integer.parseInt(parts[1]);
            int resortId = Integer.parseInt(parts[3]);
            String seasonId = parts[5];
            int vertical = dbReader.getSkierResortTotals(skierId, resortId, seasonId)
                    .stream().findFirst().map(LiftRideReadProto.VerticalRecord::getTotalVertical).orElse(0);
            keyValues.put(key, String.valueOf(vertical));
        }
        if (!keyValues.isEmpty()) {
            cache.getSync().mset(keyValues);
        }
    }

    private void refreshAllSeasonVerticals() throws SQLException {
        Set<String> keys = cache.scanKeys(config.AGGREGATION_HOT_KEY_ALL_SEASON_VERTICAL);
        Map<String, String> keyValues = new HashMap<>();
        for (String key : keys) {
            String[] parts = key.split(":");
            int skierId = Integer.parseInt(parts[1]);
            int resortId = Integer.parseInt(parts[3]);
            List<LiftRideReadProto.VerticalRecord> verticals = dbReader.getSkierResortTotals(skierId, resortId, null);
            String json = gson.toJson(verticals);
            keyValues.put(key, json);
        }
        if (!keyValues.isEmpty()) {
            cache.getSync().mset(keyValues);
        }
    }
}