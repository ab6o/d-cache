package com.coupang.dcache.serializer;

import com.coupang.dcache.CacheEntry;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * JSON implementation of the CacheEntrySerializer.
 */
public class JsonCacheEntrySerializer implements CacheEntrySerializer {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Override
    public byte[] serialize(CacheEntry entry) {
        try {
            return OBJECT_MAPPER.writeValueAsBytes(entry);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize cache entry", e);
        }
    }

    @Override
    public CacheEntry deserialize(byte[] bytes) {
        try {
            return OBJECT_MAPPER.readValue(bytes, CacheEntry.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to deserialize cache entry", e);
        }
    }
} 