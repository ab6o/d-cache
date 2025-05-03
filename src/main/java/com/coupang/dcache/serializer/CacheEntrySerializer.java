package com.coupang.dcache.serializer;

import com.coupang.dcache.CacheEntry;

/**
 * Interface for serializing and deserializing cache entries.
 */
public interface CacheEntrySerializer {
    /**
     * Serializes a cache entry to bytes.
     *
     * @param entry The cache entry to serialize
     * @return The serialized bytes
     */
    byte[] serialize(CacheEntry entry);

    /**
     * Deserializes bytes to a cache entry.
     *
     * @param bytes The bytes to deserialize
     * @return The deserialized cache entry
     */
    CacheEntry deserialize(byte[] bytes);
} 