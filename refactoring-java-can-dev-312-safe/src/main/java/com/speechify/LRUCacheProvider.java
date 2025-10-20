package com.speechify;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 *
 * Use the provided com.speechify.LRUCacheProviderTest in `src/test/java/LruCacheTest.java` to validate your
 * implementation.
 *
 * You may:
 *  - Read online API references for Java standard library or JVM collections.
 * You must not:
 *  - Read guides about how to code an LRU cache.
 */

public class LRUCacheProvider {

    public static <T> LRUCache<T> createLRUCache(CacheLimits options) {
        return new LinkedHashMapCache<>(options.getMaxItemsCount());
    }
    
    /**
     * LRU Cache implementation using LinkedHashMap with access-order mode.
     * Provides O(1) time complexity for both get and set operations.
     */
    private static class LinkedHashMapCache<V> implements LRUCache<V> {
        
        private final int capacity;
        private final Map<String, V> map;
        
        private LinkedHashMapCache(int capacity) {
            this.capacity = capacity;
            // LinkedHashMap with access-order mode (true = move to end on access)
            this.map = new LinkedHashMap<String, V>(capacity, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, V> eldest) {
                    // Automatically remove eldest (least recently used) when size exceeds capacity
                    return size() > LinkedHashMapCache.this.capacity;
                }
            };
        }
        
        @Override
        public V get(String key) {
            return map.get(key);
        }
        
        @Override
        public void set(String key, V value) {
            map.put(key, value);
        }
    }
}
