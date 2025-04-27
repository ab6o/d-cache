package com.github.httpcache.dynamodb.annotation;

import com.github.httpcache.dynamodb.HttpCacheConfig;

/**
 * Defines different strategies for generating cache keys.
 */
public enum CacheKeyStrategyType {
    /**
     * Use the default strategy as configured in HttpCacheConfig.
     */
    DEFAULT,
    
    /**
     * Simple URL-based key.
     * Maps to {@link HttpCacheConfig.CacheKeyStrategy#SIMPLE}
     */
    SIMPLE,
    
    /**
     * URL + headers based key.
     * Maps to {@link HttpCacheConfig.CacheKeyStrategy#WITH_HEADERS}
     */
    WITH_HEADERS,
    
    /**
     * Method + URL + headers based key.
     * Maps to {@link HttpCacheConfig.CacheKeyStrategy#WITH_METHOD}
     */
    WITH_METHOD,
    
    /**
     * Hashed key for potentially long keys.
     * Maps to {@link HttpCacheConfig.CacheKeyStrategy#HASHED}
     */
    HASHED;
    
    /**
     * Maps this enum value to the corresponding HttpCacheConfig.CacheKeyStrategy.
     * 
     * @param defaultStrategy The default strategy to use if this is DEFAULT
     * @return The corresponding HttpCacheConfig.CacheKeyStrategy
     */
    public HttpCacheConfig.CacheKeyStrategy toConfigStrategy(HttpCacheConfig.CacheKeyStrategy defaultStrategy) {
        switch (this) {
            case SIMPLE:
                return HttpCacheConfig.CacheKeyStrategy.SIMPLE;
            case WITH_HEADERS:
                return HttpCacheConfig.CacheKeyStrategy.WITH_HEADERS;
            case WITH_METHOD:
                return HttpCacheConfig.CacheKeyStrategy.WITH_METHOD;
            case HASHED:
                return HttpCacheConfig.CacheKeyStrategy.HASHED;
            case DEFAULT:
            default:
                return defaultStrategy;
        }
    }
} 