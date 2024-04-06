import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The <code>Cache</code> class represents a simple cache implementation using a LinkedHashMap.
 * It allows storing key-value pairs with a specified maximum capacity.
 * When the cache exceeds its maximum capacity, the least recently used entry is evicted.
 * @param <K> The type of keys stored in the cache.
 * @param <V> The type of values stored in the cache.
 */
public class Cache<K, V> {
    private final int MAX_ENTRIES;
    private final LinkedHashMap<K, V> map;

    /**
     * Constructs a cache with the specified maximum capacity.
     * @param capacity The maximum capacity of the cache.
     */
    public Cache(int capacity) {
        this.MAX_ENTRIES = capacity;
        // Initialize the LinkedHashMap with the specified capacity and a load factor of 0.75
        // Override the removeEldestEntry method to implement the eviction policy
        this.map = new LinkedHashMap<K, V>(capacity, 0.75f, true) {
            protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
                // Check if the size of the map exceeds the maximum capacity
                return size() > MAX_ENTRIES;
            }
        };
    }

    /**
     * Adds a key-value pair to the cache.
     * @param key The key to be added.
     * @param value The value to be associated with the key.
     */
    public void put(K key, V value) {
        map.put(key, value);
    }

    /**
     * Retrieves the value associated with the specified key from the cache.
     * @param key The key whose associated value is to be retrieved.
     * @return The value associated with the specified key, or null if the key is not present in the cache.
     */
    public V get(K key) {
        return map.getOrDefault(key, null);
    }

    /**
     * Checks if the cache contains the specified key.
     * @param key The key to be checked for presence in the cache.
     * @return true if the cache contains the specified key, otherwise false.
     */
    public boolean containsKey(K key) {
        return map.containsKey(key);
    }
}
