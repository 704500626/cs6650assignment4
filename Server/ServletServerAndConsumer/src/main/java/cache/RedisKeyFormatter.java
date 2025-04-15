package cache;

public class RedisKeyFormatter {
    public static String format(String pattern, Object... values) {
        String key = pattern;
        for (Object value : values) {
            key = key.replaceFirst("\\{[^{}]+}", String.valueOf(value));
        }
        return key;
    }
}