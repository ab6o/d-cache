package com.coupang.dcache.serializer;

public class CacheKeySerializer {
    private CacheKeySerializer() {
        // Private constructor to prevent instantiation
    }

    /**
     * Serializes a cache key to bytes.
     *
     * @param key The cache key to serialize
     * @return The serialized bytes
     */
    public static byte[] serialize(String key) {
        return key.getBytes();
    }

    /**
     * Deserializes bytes to a cache key.
     *
     * @param bytes The bytes to deserialize
     * @return The deserialized cache key
     */
    public static String deserialize(byte[] bytes) {
        return new String(bytes);
    }
} 