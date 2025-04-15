package cache;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import grpc.LiftRideReadProto;
import io.lettuce.core.*;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.output.BooleanOutput;
import io.lettuce.core.output.StatusOutput;
import io.lettuce.core.output.VoidOutput;
import io.lettuce.core.protocol.CommandArgs;
import io.lettuce.core.resource.DefaultClientResources;
import model.Configuration;

import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

// Edge Case:
// resort_id=1, skier_id=1, season_id=1, vertical=100
// resort_id=1, skier_id=1, season_id=2, vertical=200
// GET resort_id=1, skier_id=1, season_id=1
// [1,100] -> resort_id:1:skier_id:1, value=hashmap{season_id=1, vertical=100}
// GET resort_id=1, skier_id=1
// resort_id:1:skier_id:1 -> hashmap{season_id=1, vertical=100} -> [1,100]
public class RedisCacheClient {
    private final Configuration config;
    private final RedisClient redisClient;
    private final StatefulRedisConnection<String, String> connection;
    private final RedisCommands<String, String> sync;
    private final Gson gson = new Gson();

    public RedisCacheClient(Configuration config) {
        this.config = config;
        RedisURI redisUri = RedisURI.Builder.redis(config.REDIS_HOST, config.REDIS_PORT).build();
        redisClient = RedisClient.create(DefaultClientResources.create(), redisUri);
        redisClient.setOptions(ClientOptions.builder().autoReconnect(true).build());
//        this.redisClient = RedisClient.create(config.REDIS_URL);
        this.connection = redisClient.connect();
        this.sync = connection.sync(); // synchronous commands; you can also expose async if needed
    }

    public RedisCommands<String, String> getSync() {
        return sync;
    }

    // GET API 1
    public String getUniqueSkierCountKey(int resortId, String seasonId, int dayId) {
        return RedisKeyFormatter.format(config.REDIS_KEY_PATTERN_UNIQUE_SKIERS, resortId, seasonId, dayId);
    }

    public Integer getUniqueSkierCount(String key) {
        String val = sync.get(key);
        return val == null ? null : Integer.parseInt(val);
    }

    public Integer getUniqueSkierCount(int resortId, String seasonId, int dayId) {
        String key = getUniqueSkierCountKey(resortId, seasonId, dayId);
        String val = sync.get(key);
        return val == null ? null : Integer.parseInt(val);
    }

    public void setUniqueSkierCount(int resortId, String seasonId, int dayId, int count) {
        sync.set(getUniqueSkierCountKey(resortId, seasonId, dayId), String.valueOf(count));
    }

    public void setUniqueSkierCount(String key, int count) {
        sync.set(key, String.valueOf(count));
    }

    // GET API 2
    public String getSkierDayVerticalKey(int resortId, String seasonId, int dayId, int skierId) {
        return RedisKeyFormatter.format(config.REDIS_KEY_PATTERN_DAILY_VERTICAL, resortId, seasonId, dayId, skierId);
    }

    public Integer getSkierDayVertical(String key) {
        String val = sync.get(key);
        return val == null ? null : Integer.parseInt(val);
    }

    public Integer getSkierDayVertical(int resortId, String seasonId, int dayId, int skierId) {
        String key = getSkierDayVerticalKey(resortId, seasonId, dayId, skierId);
        String val = sync.get(key);
        return val == null ? null : Integer.parseInt(val);
    }

    public void setSkierDayVertical(int resortId, String seasonId, int dayId, int skierId, int vertical) {
        sync.set(getSkierDayVerticalKey(resortId, seasonId, dayId, skierId), String.valueOf(vertical));
    }

    public void setSkierDayVertical(String key, int vertical) {
        sync.set(key, String.valueOf(vertical));
    }

    // GET API 3b
    public String getAllSeasonsVerticalKey(int skierId, int resortId) {
        return RedisKeyFormatter.format(config.REDIS_KEY_PATTERN_ALL_SEASON_VERTICALS, skierId, resortId);
    }

    public List<LiftRideReadProto.VerticalRecord> getAllSeasonVerticals(int skierId, int resortId) {
        return getAllSeasonVerticals(getAllSeasonsVerticalKey(skierId, resortId));
    }

    public List<LiftRideReadProto.VerticalRecord> getAllSeasonVerticals(String key) {
        String json = sync.get(key);
        if (json == null) return null;
        Type listType = new TypeToken<List<LiftRideReadProto.VerticalRecord>>() {
        }.getType();
        return gson.fromJson(json, listType);
    }

    public void setAllSeasonVerticals(String key, List<LiftRideReadProto.VerticalRecord> verticals) {
        sync.set(key, gson.toJson(verticals));
    }

    public void setAllSeasonVerticals(int skierId, int resortId, List<LiftRideReadProto.VerticalRecord> verticals) {
        String key = getAllSeasonsVerticalKey(skierId, resortId);
        String json = gson.toJson(verticals);
        sync.set(key, json);
    }

    // GET API 3a
    public String getSingleSeasonVerticalKey(int skierId, int resortId, String seasonId) {
        return RedisKeyFormatter.format(config.REDIS_KEY_PATTERN_SINGLE_SEASON_VERTICAL, skierId, resortId, seasonId);
    }

    public Integer getSingleSeasonVertical(String key) {
        String val = sync.get(key);
        return val == null ? null : Integer.parseInt(val);
    }

    public Integer getSingleSeasonVertical(int skierId, int resortId, String seasonId) {
        String key = getSingleSeasonVerticalKey(skierId, resortId, seasonId);
        String val = sync.get(key);
        return val == null ? null : Integer.parseInt(val);
    }

    public void setSingleSeasonVertical(String key, int vertical) {
        sync.set(key, String.valueOf(vertical));
    }

    public void setSingleSeasonVertical(int skierId, int resortId, String seasonId, int vertical) {
        String key = getSingleSeasonVerticalKey(skierId, resortId, seasonId);
        sync.set(key, String.valueOf(vertical));
    }

    // TODO move Bloom Filter code out
    // Create a new Bloom Filter
    public void createFilter(String key, long capacity, double errorRate) {
        sync.del(key);
        sync.dispatch(RedisBloomCommand.BF_RESERVE, new StatusOutput<>(StringCodec.UTF8), new CommandArgs<>(StringCodec.UTF8).add(key).add(errorRate).add(capacity));
    }

    // Add a value to the filter
    public boolean add(String key, String item) {
        return Boolean.TRUE.equals(sync.dispatch(RedisBloomCommand.BF_ADD, new BooleanOutput<>(StringCodec.UTF8), new CommandArgs<>(StringCodec.UTF8).add(key).add(item)));
    }

//    public void addBatch(String filterName, List<String> items) {
//        if (items == null || items.isEmpty()) return;
//
//        CommandArgs<String, String> args = new CommandArgs<>(StringCodec.UTF8).add(filterName);
//        for (String item : items) {
//            args.add(item);
//        }
//
//        // Expecting a list of integers as response from BF.MADD
//        sync.dispatch(
//                RedisBloomCommand.BF_MADD,
//                new IntegerListOutput<>(StringCodec.UTF8),  // ✅ Correct output type
//                args
//        );
//    }

    public void addBatch(String filterKey, List<String> items) {
        if (items == null || items.isEmpty()) return;
        sync.dispatch(
                RedisBloomCommand.BF_MADD,
                new VoidOutput<>(StringCodec.UTF8),
                new CommandArgs<>(StringCodec.UTF8).add(filterKey).addValues(items)
        );
    }

    // Check if a value may exist
    public boolean exists(String key, String item) {
        return Boolean.TRUE.equals(sync.dispatch(RedisBloomCommand.BF_EXISTS, new BooleanOutput<>(StringCodec.UTF8), new CommandArgs<>(StringCodec.UTF8).add(key).add(item)));
    }

    public void rename(String oldKey, String newKey) {
        sync.rename(oldKey, newKey);
    }

    public Set<String> scanKeys(String pattern) {
        Set<String> keys = new HashSet<>();
        String cursor = "0";
        do {
            KeyScanCursor<String> scanResult = sync.scan(ScanCursor.of(cursor), io.lettuce.core.ScanArgs.Builder.matches(pattern).limit(100));
            cursor = scanResult.getCursor();
            keys.addAll(scanResult.getKeys());
        } while (!cursor.equals("0"));
        return keys;
    }

    public void close() {
        connection.close();
        redisClient.shutdown();
    }
}
