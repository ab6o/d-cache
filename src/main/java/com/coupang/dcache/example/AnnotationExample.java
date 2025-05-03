package com.coupang.dcache.example;

import com.coupang.dcache.HttpResponse;
import com.coupang.dcache.annotation.CacheEvict;
import com.coupang.dcache.annotation.CacheKeyStrategyType;
import com.coupang.dcache.annotation.Cacheable;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.ComponentScan;
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
@SpringBootApplication
@ComponentScan(basePackages = {"com.coupang.dcache"})
public class AnnotationExample {

    public static void main(String[] args) {
        ConfigurableApplicationContext context = SpringApplication.run(AnnotationExample.class, args);
        ApiService apiService = context.getBean(ApiService.class);

        try {
            // Basic caching example
            System.out.println("Basic caching example:");
            System.out.println("First call - should be a cache miss");
            HttpResponse response1 = apiService.fetchData("https://httpbin.org/get");
            System.out.println("Response 1: " + response1.getBody());

            System.out.println("\nSecond call - should be a cache hit");
            HttpResponse response2 = apiService.fetchData("https://httpbin.org/get");
            System.out.println("Response 2: " + response2.getBody());

            // Custom TTL example
            System.out.println("\nCustom TTL example:");
            System.out.println("First call with custom TTL - should be a cache miss");
            HttpResponse response3 = apiService.fetchDataWithCustomTtl("https://httpbin.org/get?custom=true");
            System.out.println("Response 3: " + response3.getBody());

            System.out.println("\nSecond call with custom TTL - should be a cache hit");
            HttpResponse response4 = apiService.fetchDataWithCustomTtl("https://httpbin.org/get?custom=true");
            System.out.println("Response 4: " + response4.getBody());

            // Headers example
            System.out.println("\nHeaders example:");
            Map<String, String> headers = new HashMap<>();
            headers.put("X-Custom-Header", "test-value");

            System.out.println("First call with headers - should be a cache miss");
            HttpResponse response5 = apiService.fetchDataWithHeaders("https://httpbin.org/headers", headers);
            System.out.println("Response 5: " + response5.getBody());

            System.out.println("\nSecond call with same headers - should be a cache hit");
            HttpResponse response6 = apiService.fetchDataWithHeaders("https://httpbin.org/headers", headers);
            System.out.println("Response 6: " + response6.getBody());

            headers.put("X-Custom-Header", "different-value");
            System.out.println("\nThird call with different headers - should be a cache miss");
            HttpResponse response7 = apiService.fetchDataWithHeaders("https://httpbin.org/headers", headers);
            System.out.println("Response 7: " + response7.getBody());

            // Cache key generation example
            System.out.println("\nCache key generation example:");
            System.out.println("First call - should be a cache miss");
            HttpResponse response8 = apiService.fetchData("https://httpbin.org/get");
            System.out.println("Response 8: " + response8.getBody());

            Thread.sleep(1000); // Wait for 1 second

            System.out.println("\nSecond call after delay - should still be a cache hit");
            HttpResponse response9 = apiService.fetchData("https://httpbin.org/get");
            System.out.println("Response 9: " + response9.getBody());

            // Conditional caching example
            System.out.println("\nConditional caching example:");
            System.out.println("First call with condition=true - should use cache");
            HttpResponse response10 = apiService.fetchDataWithCondition("https://httpbin.org/get?param=yes", true);
            System.out.println("Response 10: " + response10.getBody());

            System.out.println("\nSecond call with condition=true - should be a cache hit");
            HttpResponse response11 = apiService.fetchDataWithCondition("https://httpbin.org/get?param=yes", true);
            System.out.println("Response 11: " + response11.getBody());

            System.out.println("\nFirst call with condition=false - should bypass cache");
            HttpResponse response12 = apiService.fetchDataWithCondition("https://httpbin.org/get?param=no", false);
            System.out.println("Response 12: " + response12.getBody());

            System.out.println("\nSecond call with condition=false - should bypass cache again");
            HttpResponse response13 = apiService.fetchDataWithCondition("https://httpbin.org/get?param=no", false);
            System.out.println("Response 13: " + response13.getBody());

        } catch (IOException | InterruptedException e) {
            System.err.println("Error occurred: " + e.getMessage());
            e.printStackTrace();
        } finally {
            context.close();
        }
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
                com.coupang.dcache.HttpResponse.Builder responseBuilder = 
                        com.coupang.dcache.HttpResponse.builder()
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