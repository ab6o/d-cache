package com.coupang.dcache;

import com.coupang.dcache.HttpCache;
import com.coupang.dcache.HttpCacheConfig;
import com.coupang.dcache.HttpRequest;
import com.coupang.dcache.HttpResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;
import software.amazon.awssdk.services.dynamodb.waiters.DynamoDbWaiter;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class HttpCacheTest {

    @Mock
    private DynamoDbClient dynamoDbClient;

    @Mock
    private DynamoDbWaiter dynamoDbWaiter;

    private HttpCache httpCache;
    private HttpCacheConfig config;

    @BeforeEach
    public void setUp() {
        config = HttpCacheConfig.builder()
                .tableName("test-cache")
                .region("us-east-1")
                .defaultTtl(60)
                .withDynamoDbClient(dynamoDbClient)
                .build();

        httpCache = new HttpCache(config) {
            @Override
            public void close() {
                // Do nothing in tests
            }
        };
    }

    @Test
    public void testSetupTable() {
        // Mock the table not existing
        when(dynamoDbClient.describeTable(any(DescribeTableRequest.class)))
                .thenThrow(ResourceNotFoundException.class)
                .thenReturn(DescribeTableResponse.builder().build());

        when(dynamoDbClient.createTable(any(CreateTableRequest.class)))
                .thenReturn(CreateTableResponse.builder().build());

        when(dynamoDbClient.updateTimeToLive(any(UpdateTimeToLiveRequest.class)))
                .thenReturn(UpdateTimeToLiveResponse.builder().build());

        when(dynamoDbClient.waiter()).thenReturn(dynamoDbWaiter);
        when(dynamoDbWaiter.waitUntilTableExists(any(DescribeTableRequest.class))).thenReturn(null);

        // Execute the setupTable method
        httpCache.setupTable();

        // Verify the interactions with our mock
        verify(dynamoDbClient).describeTable(any(DescribeTableRequest.class));
        verify(dynamoDbClient).createTable(any(CreateTableRequest.class));
        verify(dynamoDbClient).updateTimeToLive(any(UpdateTimeToLiveRequest.class));
    }

    @Test
    public void testFetchCacheHit() throws IOException {
        String url = "https://example.com/api";
        String cacheKey = url; // In the real implementation, this would be created by createCacheKey

        // Mock a cache hit
        Map<String, AttributeValue> item = new HashMap<>();
        item.put("key", AttributeValue.builder().s(cacheKey).build());
        item.put("status_code", AttributeValue.builder().n("200").build());
        item.put("headers", AttributeValue.builder().s("{}").build());
        item.put("body", AttributeValue.builder().b(SdkBytes.fromUtf8String("cached response")).build());
        item.put("timestamp", AttributeValue.builder().n("1600000000000").build());
        item.put("expires", AttributeValue.builder().n("1600001000").build());

        GetItemResponse getItemResponse = GetItemResponse.builder()
                .item(item)
                .build();

        when(dynamoDbClient.getItem(any(GetItemRequest.class)))
                .thenReturn(getItemResponse);

        HttpResponse response = httpCache.fetch(url);
        assertTrue(response.isFromCache());
        assertEquals(200, response.getStatusCode());
        assertEquals("cached response", new String(response.getBody()));
    }

    @Test
    public void testInvalidate() {
        String url = "https://example.com/api";

        // Mock successful delete
        when(dynamoDbClient.deleteItem(any(DeleteItemRequest.class)))
                .thenReturn(DeleteItemResponse.builder().build());

        httpCache.invalidate(url);
        verify(dynamoDbClient).deleteItem(any(DeleteItemRequest.class));
    }
} 