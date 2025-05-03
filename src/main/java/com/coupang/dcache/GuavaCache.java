package com.coupang.dcache;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * In-memory cache implementation using Google Guava.
 */
public class GuavaCache implements Cache {
    private static final Logger LOGGER = LoggerFactory.getLogger(GuavaCache.class);
    
    private final LoadingCache<String, byte[]> cache;

    /**
     * Creates a new GuavaCache with the specified configuration.
     *
     * @param config The cache configuration
     */
    public GuavaCache(GuavaCacheConfig config) {
        this.cache = CacheBuilder.newBuilder()
                .maximumSize(config.getMaximumSize())
                .expireAfterWrite(config.getDefaultTtl(), TimeUnit.SECONDS)
                .removalListener(notification -> {
                    if (notification.wasEvicted()) {
                        LOGGER.debug("Cache entry evicted: {}", notification.getKey());
                    }
                })
                .build(new CacheLoader<String, byte[]>() {
                    @Override
                    public byte[] load(String key) {
                        return null; // We don't use the loading feature
                    }
                });
    }

    @Override
    public void put(String key, byte[] value, int ttl) {
        if (ttl > 0) {
            cache.put(key, value);
            LOGGER.debug("Cached value for key: {}, TTL: {} seconds", key, ttl);
        }
    }

    @Override
    public Optional<byte[]> get(String key) {
        try {
            byte[] value = cache.get(key);
            return Optional.ofNullable(value);
        } catch (Exception e) {
            LOGGER.warn("Error getting value from cache for key: {}", key, e);
            return Optional.empty();
        }
    }

    @Override
    public void invalidate(String key) {
        cache.invalidate(key);
        LOGGER.debug("Invalidated cache for key: {}", key);
    }

    @Override
    public void invalidateAll() {
        cache.invalidateAll();
        LOGGER.debug("Invalidated all cache entries");
    }
} 