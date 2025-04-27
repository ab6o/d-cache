package com.github.httpcache.dynamodb.example;

import com.github.httpcache.dynamodb.HttpResponse;
import com.github.httpcache.dynamodb.annotation.CacheEvict;
import com.github.httpcache.dynamodb.annotation.CacheKeyStrategyType;
import com.github.httpcache.dynamodb.annotation.Cacheable;
import com.github.httpcache.dynamodb.annotation.EnableHttpCaching;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.util.HashMap;
import java.util.Map;

/**
 * Example demonstrating the use of the {@link Cacheable} and {@link CacheEvict} annotations.
 */
public class AnnotationExample {

    public static void main(String[] args) {
        try (AnnotationConfigApplicationContext context = 
                new AnnotationConfigApplicationContext(AppConfig.class)) {
            
            ApiService apiService = context.getBean(ApiService.class);
            
            // Example 1: Basic caching
            System.out.println("\n=== Example 1: Basic Caching ===");
            System.out.println("First call (cache miss):");
            HttpResponse response1 = apiService.fetchData("https://httpbin.org/get");
            System.out.println("Response from cache: " + response1.isFromCache());
            
            System.out.println("\nSecond call (cache hit):");
            HttpResponse response2 = apiService.fetchData("https://httpbin.org/get");
            System.out.println("Response from cache: " + response2.isFromCache());
            
            // Example 2: Caching with custom TTL
            System.out.println("\n=== Example 2: Custom TTL ===");
            System.out.println("First call (cache miss):");
            HttpResponse response3 = apiService.fetchDataWithCustomTtl("https://httpbin.org/get?custom=true");
            System.out.println("Response from cache: " + response3.isFromCache());
            
            System.out.println("\nSecond call (cache hit):");
            HttpResponse response4 = apiService.fetchDataWithCustomTtl("https://httpbin.org/get?custom=true");
            System.out.println("Response from cache: " + response4.isFromCache());
            
            // Example 3: Caching with headers
            System.out.println("\n=== Example 3: With Headers ===");
            Map<String, String> headers = new HashMap<>();
            headers.put("X-Custom-Header", "test-value");
            
            System.out.println("First call (cache miss):");
            HttpResponse response5 = apiService.fetchDataWithHeaders("https://httpbin.org/headers", headers);
            System.out.println("Response from cache: " + response5.isFromCache());
            
            System.out.println("\nSecond call with same headers (cache hit):");
            HttpResponse response6 = apiService.fetchDataWithHeaders("https://httpbin.org/headers", headers);
            System.out.println("Response from cache: " + response6.isFromCache());
            
            // Change the header value
            headers.put("X-Custom-Header", "different-value");
            System.out.println("\nThird call with different headers (depends on key strategy):");
            HttpResponse response7 = apiService.fetchDataWithHeaders("https://httpbin.org/headers", headers);
            System.out.println("Response from cache: " + response7.isFromCache());
            
            // Example 4: Cache eviction
            System.out.println("\n=== Example 4: Cache Eviction ===");
            System.out.println("Before eviction (cache hit):");
            HttpResponse response8 = apiService.fetchData("https://httpbin.org/get");
            System.out.println("Response from cache: " + response8.isFromCache());
            
            System.out.println("\nEvicting cache for URL:");
            apiService.evictCache("https://httpbin.org/get");
            
            System.out.println("\nAfter eviction (cache miss):");
            HttpResponse response9 = apiService.fetchData("https://httpbin.org/get");
            System.out.println("Response from cache: " + response9.isFromCache());
            
            // Example 5: Conditional caching
            System.out.println("\n=== Example 5: Conditional Caching ===");
            System.out.println("With condition true (will be cached):");
            HttpResponse response10 = apiService.fetchDataWithCondition("https://httpbin.org/get?param=yes", true);
            System.out.println("Cached: " + !response10.isFromCache()); // First call is a miss
            
            HttpResponse response11 = apiService.fetchDataWithCondition("https://httpbin.org/get?param=yes", true);
            System.out.println("Second call from cache: " + response11.isFromCache());
            
            System.out.println("\nWith condition false (won't be cached):");
            HttpResponse response12 = apiService.fetchDataWithCondition("https://httpbin.org/get?param=no", false);
            System.out.println("Cached: " + !response12.isFromCache()); // Always a miss
            
            HttpResponse response13 = apiService.fetchDataWithCondition("https://httpbin.org/get?param=no", false);
            System.out.println("Second call from cache: " + response13.isFromCache());
        }
    }
    
    /**
     * Spring configuration class.
     */
    @Configuration
    @EnableHttpCaching(tableName = "annotation-example-cache")
    @ComponentScan(basePackageClasses = ApiService.class)
    public static class AppConfig {
    }
    
    /**
     * Service that makes API calls with caching.
     */
    @Service
    public static class ApiService {
        
        private final HttpClient httpClient = HttpClient.newHttpClient();
        
        /**
         * Fetches data from a URL with caching.
         */
        @Cacheable
        public HttpResponse fetchData(String url) throws IOException {
            return executeRequest(url);
        }
        
        /**
         * Fetches data with a custom TTL.
         */
        @Cacheable(ttl = 60) // 1 minute TTL
        public HttpResponse fetchDataWithCustomTtl(String url) throws IOException {
            return executeRequest(url);
        }
        
        /**
         * Fetches data with headers included in the cache key.
         */
        @Cacheable(
            keyStrategy = CacheKeyStrategyType.WITH_HEADERS,
            includeHeaders = {"X-Custom-Header"}
        )
        public HttpResponse fetchDataWithHeaders(String url, Map<String, String> headers) throws IOException {
            return executeRequest(url, headers);
        }
        
        /**
         * Fetches data with a condition for caching.
         */
        @Cacheable(condition = "#shouldCache == true")
        public HttpResponse fetchDataWithCondition(String url, boolean shouldCache) throws IOException {
            return executeRequest(url);
        }
        
        /**
         * Evicts a URL from the cache.
         */
        @CacheEvict
        public void evictCache(String url) {
            System.out.println("Evicting cache for URL: " + url);
        }
        
        /**
         * Evicts all entries from the cache.
         */
        @CacheEvict(allEntries = true)
        public void evictAllCache() {
            System.out.println("Evicting all cache entries");
        }
        
        /**
         * Executes an HTTP request.
         */
        private HttpResponse executeRequest(String url) throws IOException {
            return executeRequest(url, new HashMap<>());
        }
        
        /**
         * Executes an HTTP request with headers.
         */
        private HttpResponse executeRequest(String url, Map<String, String> headers) throws IOException {
            try {
                HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .GET();
                
                // Add headers
                headers.forEach(requestBuilder::header);
                
                java.net.http.HttpResponse<byte[]> response = httpClient.send(
                        requestBuilder.build(),
                        java.net.http.HttpResponse.BodyHandlers.ofByteArray()
                );
                
                // Convert to our HttpResponse type
                com.github.httpcache.dynamodb.HttpResponse.Builder responseBuilder = 
                        com.github.httpcache.dynamodb.HttpResponse.builder()
                                .statusCode(response.statusCode())
                                .body(response.body());
                
                // Add headers
                response.headers().map().forEach((name, values) -> {
                    if (!values.isEmpty()) {
                        responseBuilder.header(name, values.get(0));
                    }
                });
                
                return responseBuilder.build();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException("Request interrupted", e);
            }
        }
    }
} 