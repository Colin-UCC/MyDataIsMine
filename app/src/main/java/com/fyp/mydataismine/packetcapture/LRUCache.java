package com.fyp.mydataismine.packetcapture;

import java.util.LinkedHashMap;

/**
 * An LRU (Least Recently Used) cache implementation based on LinkedHashMap.
 * This cache removes the least recently accessed items first when the capacity is exceeded.
 *
 * @param <K> the type of keys maintained by this cache
 * @param <V> the type of mapped values
 */
public class LRUCache<K, V> extends LinkedHashMap<K, V>
{
    private int maxSize;
    private CleanupCallback callback;

    /**
     * Constructs an LRUCache with the specified maximum size and cleanup callback.
     *
     * @param maxSize   the maximum size of the cache
     * @param callback  the callback function to execute upon removing the eldest entry
     */
    public LRUCache(int maxSize, CleanupCallback callback)
    {
        super(maxSize + 1, 1, true);

        this.maxSize = maxSize;
        this.callback = callback;
    }

    /**
     * Determines if the eldest entry should be removed from the cache.
     *
     * @param eldest the least recently accessed entry
     * @return {@code true} if the eldest entry should be removed, {@code false} otherwise
     */
    @Override
    protected boolean removeEldestEntry(Entry<K, V> eldest)
    {
        if (size() > maxSize)
        {
            callback.cleanup(eldest);
            return true;
        }
        return false;
    }

    /**
     * Interface for cleanup actions to be executed upon removing an entry from the cache.
     */
    public static interface CleanupCallback<K, V>
    {
        public void cleanup(Entry<K, V> eldest);
    }
}