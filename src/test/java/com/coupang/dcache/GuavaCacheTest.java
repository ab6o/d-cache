package com.coupang.dcache;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class GuavaCacheTest {
    private GuavaCache cache;

    @BeforeEach
    void setUp() {
        GuavaCacheConfig config = GuavaCacheConfig.builder()
            .defaultTtl(60)
            .maximumSize(1000)
            .build();
        cache = new GuavaCache(config);
    }

    @Test
    void shouldPutAndGetValue() {
        // Given
        String key = "test-key";
        byte[] value = "test-value".getBytes();

        // When
        cache.put(key, value, 60);
        Optional<byte[]> result = cache.get(key);

        // Then
        assertTrue(result.isPresent());
        assertArrayEquals(value, result.get());
    }

    @Test
    void shouldReturnEmptyWhenKeyNotFound() {
        // Given
        String key = "non-existent-key";

        // When
        Optional<byte[]> result = cache.get(key);

        // Then
        assertFalse(result.isPresent());
    }

    @Test
    void shouldInvalidateKey() {
        // Given
        String key = "test-key";
        byte[] value = "test-value".getBytes();
        cache.put(key, value, 60);

        // When
        cache.invalidate(key);
        Optional<byte[]> result = cache.get(key);

        // Then
        assertFalse(result.isPresent());
    }

    @Test
    void shouldInvalidateAll() {
        // Given
        String key1 = "test-key-1";
        String key2 = "test-key-2";
        byte[] value = "test-value".getBytes();
        cache.put(key1, value, 60);
        cache.put(key2, value, 60);

        // When
        cache.invalidateAll();
        Optional<byte[]> result1 = cache.get(key1);
        Optional<byte[]> result2 = cache.get(key2);

        // Then
        assertFalse(result1.isPresent());
        assertFalse(result2.isPresent());
    }
} 