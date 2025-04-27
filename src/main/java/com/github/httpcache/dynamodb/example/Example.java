package com.github.httpcache.dynamodb.example;

import com.github.httpcache.dynamodb.HttpCache;
import com.github.httpcache.dynamodb.HttpCacheConfig;
import com.github.httpcache.dynamodb.HttpResponse;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Example usage of the HttpCache library.
 */
public class Example {

    public static void main(String[] args) {
        // Example with simple cache key strategy
        System.out.println("=== Example with Simple Cache Key Strategy ===");
        runExample(HttpCacheConfig.CacheKeyStrategy.SIMPLE);
        
        // Example with headers-based cache key strategy
        System.out.println("\n=== Example with Headers-Based Cache Key Strategy ===");
        runExample(HttpCacheConfig.CacheKeyStrategy.WITH_HEADERS);
        
        // Example with method-based cache key strategy
        System.out.println("\n=== Example with Method-Based Cache Key Strategy ===");
        runExample(HttpCacheConfig.CacheKeyStrategy.WITH_METHOD);
        
        // Example with hashed cache key strategy
        System.out.println("\n=== Example with Hashed Cache Key Strategy ===");
        runExample(HttpCacheConfig.CacheKeyStrategy.HASHED);
    }
    
    private static void runExample(HttpCacheConfig.CacheKeyStrategy strategy) {
        // Create cache configuration
        HttpCacheConfig config = HttpCacheConfig.builder()
                .tableName("http-cache-" + strategy.name().toLowerCase())
                .region("us-east-1")
                .defaultTtl(3600) // 1 hour default TTL
                .cacheKeyStrategy(strategy)
                .build();
        
        // Create the cache client
        try (HttpCache cache = new HttpCache(config)) {
            // Set up the DynamoDB table if it doesn't exist
            cache.setupTable();
            
            // Example 1: Basic caching
            System.out.println("Making first request (will be a cache miss)...");
            HttpResponse response1 = cache.fetch("https://httpbin.org/get");
            System.out.println("Response from cache: " + response1.isFromCache());
            System.out.println("Status code: " + response1.getStatusCode());
            
            // Example 2: Fetch again (should be a cache hit)
            System.out.println("\nMaking second request (should be a cache hit)...");
            HttpResponse response2 = cache.fetch("https://httpbin.org/get");
            System.out.println("Response from cache: " + response2.isFromCache());
            
            // Example 3: Custom TTL and headers
            System.out.println("\nMaking request with custom TTL and headers...");
            HttpResponse response3 = cache.fetch("https://httpbin.org/get?custom=true", options -> {
                options.ttl(300); // 5 minutes TTL
                options.header("Custom-Header", "CustomValue");
            });
            System.out.println("Response from cache: " + response3.isFromCache());
            
            // Example 4: Fetch with same URL but different headers (results depend on strategy)
            System.out.println("\nMaking request with same URL but different headers...");
            HttpResponse response4 = cache.fetch("https://httpbin.org/get?custom=true", options -> {
                options.header("Different-Header", "DifferentValue");
            });
            System.out.println("Response from cache: " + response4.isFromCache() + 
                    " (should be " + (strategy == HttpCacheConfig.CacheKeyStrategy.SIMPLE ? "true" : "false") + 
                    " for " + strategy + " strategy)");
            
            // Example 5: Invalidate cache
            System.out.println("\nInvalidating cache for URL...");
            cache.invalidate("https://httpbin.org/get");
            
            // Example 6: Fetch after invalidation (should be a cache miss)
            System.out.println("\nMaking request after invalidation (should be a cache miss)...");
            HttpResponse response5 = cache.fetch("https://httpbin.org/get");
            System.out.println("Response from cache: " + response5.isFromCache());
            
        } catch (IOException e) {
            System.err.println("Error executing HTTP requests: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
} 