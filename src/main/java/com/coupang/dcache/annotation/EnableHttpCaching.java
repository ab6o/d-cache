package com.coupang.dcache.annotation;

import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

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
@Documented
@Import(HttpCacheConfiguration.class)
public @interface EnableHttpCaching {
    
    /**
     * The name of the DynamoDB table to use for caching.
     */
    String tableName();
    
    /**
     * The AWS region where the DynamoDB table is located.
     * Defaults to us-east-1.
     */
    String region() default "us-east-1";
    
    /**
     * The default TTL (in seconds) for cached items.
     * Defaults to 3600 seconds (1 hour).
     */
    long defaultTtl() default 3600;
    
    /**
     * Whether to bypass the cache entirely.
     * Useful for development/testing.
     * Defaults to false.
     */
    boolean bypassCache() default false;
    
    /**
     * The strategy to use for generating cache keys.
     * Defaults to SIMPLE.
     */
    CacheKeyStrategyType keyStrategy() default CacheKeyStrategyType.SIMPLE;
    
    /**
     * The DynamoDB endpoint to use.
     * If not specified, the default AWS endpoint for the region will be used.
     */
    String endpoint() default "";
} 