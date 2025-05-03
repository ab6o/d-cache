package com.coupang.dcache;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DynamoDbCacheTest {
    private static final String TABLE_NAME = "test-cache";
    private static final String REGION = "us-east-1";

    @Mock
    private DynamoDbClient dynamoDbClient;

    private DynamoDbCache cache;

    @BeforeEach
    void setUp() {
        DynamoDbCacheConfig config = DynamoDbCacheConfig.builder()
            .tableName(TABLE_NAME)
            .region(REGION)
            .withDynamoDbClient(dynamoDbClient)
            .build();
        cache = new DynamoDbCache(config);
    }

    @Test
    void shouldPutAndGetValue() {
        // Given
        String key = "test-key";
        byte[] value = "test-value".getBytes();
        long expires = Instant.now().plusSeconds(60).getEpochSecond();
        Map<String, AttributeValue> item = new HashMap<>();
        item.put("key", AttributeValue.builder().s(key).build());
        item.put("value", AttributeValue.builder().b(SdkBytes.fromByteArray(value)).build());
        item.put("expires", AttributeValue.builder().n(String.valueOf(expires)).build());

        GetItemResponse response = GetItemResponse.builder()
            .item(item)
            .build();
        when(dynamoDbClient.getItem(any(GetItemRequest.class))).thenReturn(response);

        // When
        cache.put(key, value, 60);
        Optional<byte[]> result = cache.get(key);

        // Then
        assertTrue(result.isPresent());
        assertArrayEquals(value, result.get());
        verify(dynamoDbClient).putItem(any(PutItemRequest.class));
        verify(dynamoDbClient).getItem(any(GetItemRequest.class));
    }

    @Test
    void shouldReturnEmptyWhenKeyNotFound() {
        // Given
        String key = "non-existent-key";
        GetItemResponse response = GetItemResponse.builder().build();
        when(dynamoDbClient.getItem(any(GetItemRequest.class))).thenReturn(response);

        // When
        Optional<byte[]> result = cache.get(key);

        // Then
        assertFalse(result.isPresent());
        verify(dynamoDbClient).getItem(any(GetItemRequest.class));
    }

    @Test
    void shouldInvalidateKey() {
        // Given
        String key = "test-key";
        byte[] value = "test-value".getBytes();

        // When
        cache.invalidate(key);

        // Then
        verify(dynamoDbClient).deleteItem(any(DeleteItemRequest.class));
    }

    @Test
    void shouldInvalidateAll() {
        // Given
        Map<String, AttributeValue> item1 = new HashMap<>();
        item1.put("key", AttributeValue.builder().s("test-key-1").build());
        Map<String, AttributeValue> item2 = new HashMap<>();
        item2.put("key", AttributeValue.builder().s("test-key-2").build());

        ScanResponse scanResponse = ScanResponse.builder()
            .items(item1, item2)
            .build();
        when(dynamoDbClient.scan(any(ScanRequest.class))).thenReturn(scanResponse);

        // When
        cache.invalidateAll();

        // Then
        verify(dynamoDbClient).scan(any(ScanRequest.class));
        verify(dynamoDbClient, times(2)).deleteItem(any(DeleteItemRequest.class));
    }
} 