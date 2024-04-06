import java.util.LinkedHashMap;
import java.util.Map;

public class Cache<K, V> {
    private final int MAX_ENTRIES;
    private final LinkedHashMap<K, V> map;

    public Cache(int capacity) {
        this.MAX_ENTRIES = capacity;
        this.map = new LinkedHashMap<K, V>(capacity, 0.75f, true) {
            protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
                return size() > MAX_ENTRIES;
            }
        };
    }

    public void put(K key, V value) {
        map.put(key, value);
    }

    public V get(K key) {
        return map.getOrDefault(key, null);
    }

    public boolean containsKey(K key) {
        return map.containsKey(key);
    }
}
