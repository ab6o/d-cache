package com.coupang.dcache;

import java.util.Optional;

/**
 * Interface for a simple cache with TTL support.
 */
public interface Cache {
    /**
     * Put an item in the cache with a TTL.
     *
     * @param key The cache key
     * @param value The value to cache
     * @param ttl Time to live in seconds
     */
    void put(String key, byte[] value, int ttl);

    /**
     * Get an item from the cache.
     *
     * @param key The cache key
     * @return The cached value, or empty if not found or expired
     */
    Optional<byte[]> get(String key);

    /**
     * Remove an item from the cache.
     *
     * @param key The cache key
     */
    void invalidate(String key);

    /**
     * Remove all items from the cache.
     */
    void invalidateAll();
} 