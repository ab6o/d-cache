package com.github.httpcache.dynamodb.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates that the result of the annotated method can be cached.
 * This annotation can be applied to methods that make HTTP requests
 * to enable automatic caching of the results in DynamoDB.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Cacheable {
    
    /**
     * The cache key to use. If not specified, a key will be generated based on the method parameters.
     * Supports SpEL expressions for dynamic key generation, e.g., "#param1 + '-' + #param2"
     */
    String key() default "";
    
    /**
     * The time-to-live (TTL) for the cache entry in seconds.
     * If not specified, the default TTL configured in the HttpCacheConfig will be used.
     */
    int ttl() default -1;
    
    /**
     * Whether to include method parameters in the cache key.
     */
    boolean includeParams() default true;
    
    /**
     * Names of parameters to exclude from the cache key when includeParams is true.
     */
    String[] excludeParams() default {};
    
    /**
     * Header names to include in the cache key.
     */
    String[] includeHeaders() default {};
    
    /**
     * The cache key strategy to use for this method.
     */
    CacheKeyStrategyType keyStrategy() default CacheKeyStrategyType.DEFAULT;
    
    /**
     * Condition that determines if caching should be applied.
     * Supports SpEL expressions, e.g., "#param1 > 10"
     */
    String condition() default "";
    
    /**
     * Whether to bypass the cache for this invocation.
     */
    boolean bypass() default false;
} 