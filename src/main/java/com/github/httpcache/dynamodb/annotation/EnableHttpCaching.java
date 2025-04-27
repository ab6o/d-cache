package com.github.httpcache.dynamodb.annotation;

import org.springframework.context.annotation.Import;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Enables HTTP caching using DynamoDB.
 * <p>
 * Add this annotation to a Spring {@code @Configuration} class to enable the
 * automatic caching of HTTP responses in DynamoDB when using {@link Cacheable}
 * and {@link CacheEvict} annotations.
 * </p>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Import(HttpCacheConfiguration.class)
public @interface EnableHttpCaching {
    
    /**
     * The name of the DynamoDB table to use for caching.
     */
    String tableName() default "http-cache";
    
    /**
     * The AWS region to use.
     */
    String region() default "us-east-1";
    
    /**
     * The default TTL for cache entries in seconds.
     */
    int defaultTtl() default 3600;
    
    /**
     * The default cache key strategy to use.
     */
    CacheKeyStrategyType keyStrategy() default CacheKeyStrategyType.SIMPLE;
    
    /**
     * Custom endpoint for DynamoDB (useful for local testing).
     */
    String endpoint() default "";
    
    /**
     * Whether to bypass the cache globally.
     */
    boolean bypassCache() default false;
} 