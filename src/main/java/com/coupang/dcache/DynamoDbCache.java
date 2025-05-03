package com.coupang.dcache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * DynamoDB-based cache implementation.
 */
public class DynamoDbCache implements Cache {
    private static final Logger LOGGER = LoggerFactory.getLogger(DynamoDbCache.class);
    
    private final DynamoDbClient dynamoDbClient;
    private final String tableName;

    public DynamoDbCache(DynamoDbCacheConfig config) {
        this.dynamoDbClient = config.getDynamoDbClient();
        this.tableName = config.getTableName();
    }

    @Override
    public void put(String key, byte[] value, int ttl) {
        try {
            long expires = Instant.now().plusSeconds(ttl).getEpochSecond();
            
            Map<String, AttributeValue> item = new HashMap<>();
            item.put("key", AttributeValue.builder().s(key).build());
            item.put("value", AttributeValue.builder().b(SdkBytes.fromByteArray(value)).build());
            item.put("expires", AttributeValue.builder().n(String.valueOf(expires)).build());

            PutItemRequest request = PutItemRequest.builder()
                .tableName(tableName)
                .item(item)
                .build();

            dynamoDbClient.putItem(request);
            LOGGER.debug("Cached value for key: {}, TTL: {} seconds", key, ttl);
        } catch (Exception e) {
            LOGGER.error("Error putting value in cache for key: {}", key, e);
            throw new RuntimeException("Failed to put value in cache", e);
        }
    }

    @Override
    public Optional<byte[]> get(String key) {
        try {
            Map<String, AttributeValue> keyMap = new HashMap<>();
            keyMap.put("key", AttributeValue.builder().s(key).build());

            GetItemRequest request = GetItemRequest.builder()
                .tableName(tableName)
                .key(keyMap)
                .build();

            GetItemResponse response = dynamoDbClient.getItem(request);
            if (!response.hasItem()) {
                return Optional.empty();
            }

            Map<String, AttributeValue> item = response.item();
            long expires = Long.parseLong(item.get("expires").n());
            
            if (Instant.now().getEpochSecond() > expires) {
                // Item has expired, remove it
                invalidate(key);
                return Optional.empty();
            }

            byte[] value = item.get("value").b().asByteArray();
            return Optional.of(value);
        } catch (Exception e) {
            LOGGER.error("Error getting value from cache for key: {}", key, e);
            return Optional.empty();
        }
    }

    @Override
    public void invalidate(String key) {
        try {
            Map<String, AttributeValue> keyMap = new HashMap<>();
            keyMap.put("key", AttributeValue.builder().s(key).build());

            DeleteItemRequest request = DeleteItemRequest.builder()
                .tableName(tableName)
                .key(keyMap)
                .build();

            dynamoDbClient.deleteItem(request);
            LOGGER.debug("Invalidated cache for key: {}", key);
        } catch (Exception e) {
            LOGGER.error("Error invalidating cache for key: {}", key, e);
            throw new RuntimeException("Failed to invalidate cache", e);
        }
    }

    @Override
    public void invalidateAll() {
        try {
            // Note: This is a simple implementation that scans and deletes all items
            // In production, you might want to use a more efficient approach
            ScanRequest scanRequest = ScanRequest.builder()
                .tableName(tableName)
                .build();

            ScanResponse response = dynamoDbClient.scan(scanRequest);
            for (Map<String, AttributeValue> item : response.items()) {
                String key = item.get("key").s();
                invalidate(key);
            }
            LOGGER.debug("Invalidated all cache entries");
        } catch (Exception e) {
            LOGGER.error("Error invalidating all cache entries", e);
            throw new RuntimeException("Failed to invalidate all cache entries", e);
        }
    }
} 