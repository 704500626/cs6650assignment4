package cache;

import java.util.*;

public class LocalLRUCache<K, V> {
    private final Map<K, V> lru;

    public LocalLRUCache(int capacity) {
        this.lru = Collections.synchronizedMap(new LinkedHashMap<>(capacity, 0.75f, true) {
            protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
                return size() > capacity;
            }
        });
    }

    public V get(K key) {
        return lru.get(key);
    }

    public void put(K key, V value) {
        lru.put(key, value);
    }

    public void remove(K key) {
        lru.remove(key);
    }

    public Set<K> keySetSnapshot() {
        synchronized (lru) {
            return new HashSet<>(lru.keySet());
        }
    }
}
