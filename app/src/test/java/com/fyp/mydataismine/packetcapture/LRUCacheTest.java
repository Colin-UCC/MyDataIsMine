package com.fyp.mydataismine.packetcapture;

import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;

import java.util.Map;

public class LRUCacheTest {

    private LRUCache<Integer, String> cache;
    private int maxSize = 3;
    private Map.Entry<Integer, String> evictedEntry;

    @Before
    public void setUp() {
        cache = new LRUCache<>(maxSize, eldest -> evictedEntry = eldest);
    }

    @Test
    public void testInsertionAndRetrieval() {
        cache.put(1, "one");
        cache.put(2, "two");
        cache.put(3, "three");

        assertEquals("one", cache.get(1));
        assertEquals("two", cache.get(2));
        assertEquals("three", cache.get(3));
    }

    @Test
    public void testEvictionOrder() {
        cache.put(1, "one");
        cache.put(2, "two");
        cache.put(3, "three");
        cache.get(1); // Access to make '1' recently used
        cache.put(4, "four"); // Should evict '2'

        assertNull(cache.get(2));
        assertNotNull(cache.get(1));
        assertNotNull(cache.get(3));
        assertNotNull(cache.get(4));
    }

    @Test
    public void testCleanupCallback() {
        cache.put(1, "one");
        cache.put(2, "two");
        cache.put(3, "three");
        cache.get(1); // Access to make '1' recently used
        cache.put(4, "four"); // Should evict '2'

        assertNotNull(evictedEntry);
        assertEquals(Integer.valueOf(2), evictedEntry.getKey());
        assertEquals("two", evictedEntry.getValue());
    }
}
