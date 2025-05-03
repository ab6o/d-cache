package com.coupang.dcache.example;

import com.coupang.dcache.Cache;
import com.coupang.dcache.DynamoDbCache;
import com.coupang.dcache.DynamoDbCacheConfig;
import com.coupang.dcache.GuavaCache;
import com.coupang.dcache.GuavaCacheConfig;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

import java.net.URI;

public class SimpleExample {
    public static void main(String[] args) {
        // Example 1: Using Guava in-memory cache
        System.out.println("Example 1: Using Guava in-memory cache");
        GuavaCacheConfig guavaConfig = GuavaCacheConfig.builder()
                .defaultTtl(60) // 1 minute
                .maximumSize(1000)
                .build();
        
        Cache inMemoryCache = new GuavaCache(guavaConfig);
        runCacheExample(inMemoryCache);

        // Example 2: Using DynamoDB cache
//        System.out.println("\nExample 2: Using DynamoDB cache");
//        DynamoDbClient dynamoDbClient = DynamoDbClient.builder()
//                .endpointOverride(URI.create("http://localhost:8000"))
//                .region(Region.US_EAST_1)
//                .credentialsProvider(StaticCredentialsProvider.create(
//                        AwsBasicCredentials.create("test", "test")))
//                .build();
//
//        DynamoDbCacheConfig dynamoConfig = DynamoDbCacheConfig.builder()
//                .tableName("example-cache")
//                .region("us-east-1")
//                .withDynamoDbClient(dynamoDbClient)
//                .build();
//
//        Cache dynamoCache = new DynamoDbCache(dynamoConfig);
//        runCacheExample(dynamoCache);
    }

    private static void runCacheExample(Cache cache) {
        String key = "test-key";
        byte[] value = "Hello, World!".getBytes();

        // Put value in cache
        cache.put(key, value, 60);
        System.out.println("Put value in cache");

        // Get value from cache
        cache.get(key).ifPresent(v -> {
            System.out.println("Got value from cache: " + new String(v));
        });

        // Wait for 2 seconds
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Get value again (should still be there)
        cache.get(key).ifPresent(v -> {
            System.out.println("Got value from cache after 2 seconds: " + new String(v));
        });

        // Invalidate the cache entry
        cache.invalidate(key);
        System.out.println("Invalidated cache entry");

        // Try to get value again (should be gone)
        cache.get(key).ifPresentOrElse(
            v -> System.out.println("Value still in cache: " + new String(v)),
            () -> System.out.println("Value not found in cache (as expected)")
        );
    }
} 