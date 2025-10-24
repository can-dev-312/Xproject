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

    private static class LinkedHashMapCache<V> implements LRUCache<V>{

        private final int capacity;
        private final Map<String, V> map;

        public LinkedHashMapCache(int maxItemsCount) {
            this.capacity = maxItemsCount;

            this.map = new LinkedHashMap<String,V>(capacity, 0.75f, true) {

                @Override
                protected boolean removeEldestEntry(Map.Entry<String,V> eldest){
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
