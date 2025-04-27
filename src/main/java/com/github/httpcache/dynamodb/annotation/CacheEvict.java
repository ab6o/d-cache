package com.github.httpcache.dynamodb.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates that the execution of the annotated method should evict entries from the cache.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface CacheEvict {
    
    /**
     * The cache key to evict. If not specified, a key will be generated based on the method parameters.
     * Supports SpEL expressions for dynamic key generation, e.g., "#param1 + '-' + #param2"
     */
    String key() default "";
    
    /**
     * Whether to evict all entries in the cache.
     */
    boolean allEntries() default false;
    
    /**
     * Whether to include method parameters in the cache key.
     */
    boolean includeParams() default true;
    
    /**
     * Names of parameters to exclude from the cache key when includeParams is true.
     */
    String[] excludeParams() default {};
    
    /**
     * Condition that determines if eviction should be applied.
     * Supports SpEL expressions, e.g., "#result > 0"
     */
    String condition() default "";
    
    /**
     * Whether the eviction should be done before or after the method is executed.
     */
    boolean beforeInvocation() default false;
} 