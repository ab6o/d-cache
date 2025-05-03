package com.coupang.dcache;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.io.IOException;
import java.net.URI;
import java.time.Instant;
import java.util.*;
import java.util.function.Consumer;

/**
 * A client for caching data in DynamoDB with configurable TTL.
 * This can be used for HTTP responses or any other data that needs to be cached.
 */
public class HttpCache implements AutoCloseable {
    private static final Logger LOGGER = LoggerFactory.getLogger(HttpCache.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    
    private final HttpCacheConfig config;
    private final DynamoDbClient dynamoDbClient;
    private final CloseableHttpClient httpClient;

    /**
     * Creates a new HttpCache with the specified configuration.
     *
     * @param config The configuration
     */
    public HttpCache(HttpCacheConfig config) {
        this.config = config;
        this.dynamoDbClient = config.getDynamoDbClient();
        this.httpClient = HttpClients.createDefault();
    }

    /**
     * Fetches the HTTP response for the URL, using the cache if available.
     * Uses the default tenant and country code from the configuration if they are set.
     *
     * @param url The URL to fetch
     * @return The HTTP response
     * @throws IOException If an I/O error occurs
     */
    public HttpResponse fetch(String url) throws IOException {
        return fetch(url, options -> {
            // Add default tenant and country code if configured
            if (config.getDefaultTenant() != null) {
                options.tenant(config.getDefaultTenant());
            }
            if (config.getDefaultCountryCode() != null) {
                options.countryCode(config.getDefaultCountryCode());
            }
        });
    }

    /**
     * Fetches the HTTP response for the URL, using the cache if available.
     *
     * @param url The URL to fetch
     * @param optionsConsumer A consumer that configures options for this request
     * @return The HTTP response
     * @throws IOException If an I/O error occurs
     */
    public HttpResponse fetch(String url, Consumer<HttpRequest.Builder> optionsConsumer) throws IOException {
        HttpRequest.Builder requestBuilder = HttpRequest.builder(url);
        
        // Add default tenant and country code if configured and not overridden by the consumer
        if (config.getDefaultTenant() != null) {
            requestBuilder.tenant(config.getDefaultTenant());
        }
        if (config.getDefaultCountryCode() != null) {
            requestBuilder.countryCode(config.getDefaultCountryCode());
        }
        
        optionsConsumer.accept(requestBuilder);
        HttpRequest request = requestBuilder.build();
        
        // Check if we should bypass the cache
        if (config.isBypassCache() || request.getTtl() != null && request.getTtl() <= 0) {
            LOGGER.debug("Cache bypass requested for URL: {}, tenant: {}, country: {}", 
                         url, request.getTenant(), request.getCountryCode());
            return executeRequest(request);
        }
        
        // Check if we have a cached response
        String cacheKey = createCacheKey(request);
        Optional<CacheEntry> cachedEntry = getCachedEntry(cacheKey);
        
        if (cachedEntry.isPresent()) {
            LOGGER.debug("Cache hit for URL: {}, tenant: {}, country: {}", 
                         url, request.getTenant(), request.getCountryCode());
            return cachedEntry.get().toHttpResponse();
        }
        
        // No cache hit, execute the request
        LOGGER.debug("Cache miss for URL: {}, tenant: {}, country: {}", 
                     url, request.getTenant(), request.getCountryCode());
        HttpResponse response = executeRequest(request);
        
        // Cache the response if it was successful
        if (response.getStatusCode() >= 200 && response.getStatusCode() < 400) {
            cacheResponse(cacheKey, response, request);
        }
        
        return response;
    }

    /**
     * Stores a data item in the cache with a specific key.
     *
     * @param key The cache key
     * @param data The data to cache
     * @param ttl Time to live in seconds, or null to use default
     * @param tenant The tenant identifier, or null if not used
     * @param countryCode The country code, or null if not used
     */
    public void put(String key, byte[] data, Integer ttl, String tenant, String countryCode) {
        int actualTtl = ttl != null ? ttl : config.getDefaultTtl();
        Instant expiryTime = Instant.now().plusSeconds(actualTtl);
        
        CacheEntry.Builder builder = CacheEntry.builder()
                .key(key)
                .statusCode(200) // Default status code
                .headers(new HashMap<>()) // Empty headers
                .body(data)
                .timestamp(Instant.now())
                .expires(expiryTime);
        
        if (tenant != null) {
            builder.tenant(tenant);
        }
        
        if (countryCode != null) {
            builder.countryCode(countryCode);
        }
        
        CacheEntry cacheEntry = builder.build();
        
        Map<String, AttributeValue> item = new HashMap<>();
        item.put("key", AttributeValue.builder().s(cacheEntry.getKey()).build());
        
        if (cacheEntry.getTenant() != null) {
            item.put("tenant", AttributeValue.builder().s(cacheEntry.getTenant()).build());
        }
        
        if (cacheEntry.getCountryCode() != null) {
            item.put("country_code", AttributeValue.builder().s(cacheEntry.getCountryCode()).build());
        }
        
        item.put("body", AttributeValue.builder().b(SdkBytes.fromByteArray(cacheEntry.getBody())).build());
        item.put("timestamp", AttributeValue.builder().n(String.valueOf(cacheEntry.getTimestamp())).build());
        item.put("expires", AttributeValue.builder().n(String.valueOf(cacheEntry.getExpires())).build());
        
        try {
            dynamoDbClient.putItem(PutItemRequest.builder()
                    .tableName(config.getTableName())
                    .item(item)
                    .build());
            
            LOGGER.debug("Cached data for key: {}, expires at: {}, tenant: {}, country: {}", 
                     key, expiryTime, tenant, countryCode);
        } catch (Exception e) {
            LOGGER.warn("Failed to cache data for key: {}", key, e);
        }
    }
    
    /**
     * Retrieves data from the cache.
     *
     * @param key The cache key
     * @return The cached data, or empty if not found
     */
    public Optional<byte[]> get(String key) {
        try {
            GetItemResponse response = dynamoDbClient.getItem(GetItemRequest.builder()
                    .tableName(config.getTableName())
                    .key(Collections.singletonMap("key", AttributeValue.builder().s(key).build()))
                    .consistentRead(true)
                    .build());
            
            if (!response.hasItem() || response.item().isEmpty()) {
                return Optional.empty();
            }
            
            Map<String, AttributeValue> item = response.item();
            
            if (item.containsKey("body")) {
                return Optional.of(item.get("body").b().asByteArray());
            }
            
            return Optional.empty();
        } catch (Exception e) {
            LOGGER.warn("Failed to get item from cache: {}", key, e);
            return Optional.empty();
        }
    }

    /**
     * Invalidates the cache entry for the URL.
     * Uses the default tenant and country code from the configuration.
     *
     * @param url The URL to invalidate
     */
    public void invalidate(String url) {
        HttpRequest.Builder builder = HttpRequest.builder(url);
        
        // Add default tenant and country code if configured
        if (config.getDefaultTenant() != null) {
            builder.tenant(config.getDefaultTenant());
        }
        if (config.getDefaultCountryCode() != null) {
            builder.countryCode(config.getDefaultCountryCode());
        }
        
        invalidate(builder.build());
    }
    
    /**
     * Invalidates the cache entry for the specified request.
     *
     * @param request The request to invalidate
     */
    public void invalidate(HttpRequest request) {
        String cacheKey = createCacheKey(request);
        invalidateByKey(cacheKey);
    }
    
    /**
     * Invalidates a cache entry by its key.
     *
     * @param key The cache key to invalidate
     */
    public void invalidateByKey(String key) {
        try {
            dynamoDbClient.deleteItem(DeleteItemRequest.builder()
                    .tableName(config.getTableName())
                    .key(Collections.singletonMap("key", AttributeValue.builder().s(key).build()))
                    .build());
            LOGGER.debug("Invalidated cache for key: {}", key);
        } catch (Exception e) {
            LOGGER.warn("Failed to invalidate cache for key: {}", key, e);
        }
    }

    /**
     * Invalidates all cache entries for the default tenant and country code.
     */
    public void invalidateAll() {
        invalidateAll(config.getDefaultTenant(), config.getDefaultCountryCode());
    }
    
    /**
     * Invalidates all cache entries for a specific tenant and country code.
     * 
     * @param tenant The tenant to invalidate entries for, or null for all tenants
     * @param countryCode The country code to invalidate entries for, or null for all countries
     */
    public void invalidateAll(String tenant, String countryCode) {
        // Build scan conditions based on tenant and country code if provided
        Map<String, AttributeValue> expressionValues = new HashMap<>();
        StringBuilder filterExpression = new StringBuilder();
        
        if (tenant != null && !tenant.isEmpty()) {
            filterExpression.append("tenant = :tenant");
            expressionValues.put(":tenant", AttributeValue.builder().s(tenant).build());
        }
        
        if (countryCode != null && !countryCode.isEmpty()) {
            if (filterExpression.length() > 0) {
                filterExpression.append(" AND ");
            }
            filterExpression.append("country_code = :country");
            expressionValues.put(":country", AttributeValue.builder().s(countryCode).build());
        }
        
        try {
            ScanRequest.Builder scanBuilder = ScanRequest.builder()
                    .tableName(config.getTableName())
                    .attributesToGet("key");
            
            // Add filter expression if we have one
            if (filterExpression.length() > 0) {
                scanBuilder.filterExpression(filterExpression.toString())
                           .expressionAttributeValues(expressionValues);
            }
            
            ScanResponse scanResponse = dynamoDbClient.scan(scanBuilder.build());
            
            List<WriteRequest> writeRequests = new ArrayList<>();
            for (Map<String, AttributeValue> item : scanResponse.items()) {
                writeRequests.add(WriteRequest.builder()
                        .deleteRequest(DeleteRequest.builder()
                                .key(Collections.singletonMap("key", item.get("key")))
                                .build())
                        .build());
                
                // Process in batches of 25 (DynamoDB limit)
                if (writeRequests.size() == 25) {
                    batchDeleteItems(writeRequests);
                    writeRequests.clear();
                }
            }
            
            // Process any remaining items
            if (!writeRequests.isEmpty()) {
                batchDeleteItems(writeRequests);
            }
            
            LOGGER.debug("Invalidated cache entries for tenant: {}, country: {}", tenant, countryCode);
        } catch (Exception e) {
            LOGGER.warn("Failed to invalidate cache entries", e);
        }
    }

    /**
     * Sets up the DynamoDB table required for caching.
     */
    public void setupTable() {
        setupTable(false);
    }
    
    /**
     * Sets up the DynamoDB table required for caching with options for tenant and country code indexes.
     * 
     * @param withIndexes Whether to create secondary indexes for tenant and country code
     */
    public void setupTable(boolean withIndexes) {
        try {
            // Check if table exists
            try {
                dynamoDbClient.describeTable(DescribeTableRequest.builder()
                        .tableName(config.getTableName())
                        .build());
                LOGGER.info("Table {} already exists", config.getTableName());
                return;
            } catch (ResourceNotFoundException e) {
                // Table doesn't exist, create it
            }
            
            // Create the table
            CreateTableRequest.Builder requestBuilder = CreateTableRequest.builder()
                    .tableName(config.getTableName())
                    .attributeDefinitions(
                        AttributeDefinition.builder()
                            .attributeName("key")
                            .attributeType(ScalarAttributeType.S)
                            .build()
                    )
                    .keySchema(
                        KeySchemaElement.builder()
                            .attributeName("key")
                            .keyType(KeyType.HASH)
                            .build()
                    )
                    .provisionedThroughput(
                        ProvisionedThroughput.builder()
                            .readCapacityUnits(5L)
                            .writeCapacityUnits(5L)
                            .build()
                    );
            
            // Add tenant and country code attribute definitions if creating indexes
            if (withIndexes) {
                List<AttributeDefinition> attributeDefinitions = new ArrayList<>();
                attributeDefinitions.add(AttributeDefinition.builder()
                    .attributeName("key")
                    .attributeType(ScalarAttributeType.S)
                    .build());
                attributeDefinitions.add(AttributeDefinition.builder()
                    .attributeName("tenant")
                    .attributeType(ScalarAttributeType.S)
                    .build());
                attributeDefinitions.add(AttributeDefinition.builder()
                    .attributeName("country_code")
                    .attributeType(ScalarAttributeType.S)
                    .build());
                
                requestBuilder.attributeDefinitions(attributeDefinitions);
                
                // Add GSIs for tenant and country code
                requestBuilder.globalSecondaryIndexes(
                    GlobalSecondaryIndex.builder()
                        .indexName("tenant-index")
                        .keySchema(KeySchemaElement.builder()
                                .attributeName("tenant")
                                .keyType(KeyType.HASH)
                                .build())
                        .projection(Projection.builder()
                                .projectionType(ProjectionType.ALL)
                                .build())
                        .provisionedThroughput(ProvisionedThroughput.builder()
                                .readCapacityUnits(5L)
                                .writeCapacityUnits(5L)
                                .build())
                        .build(),
                    GlobalSecondaryIndex.builder()
                        .indexName("country-code-index")
                        .keySchema(KeySchemaElement.builder()
                                .attributeName("country_code")
                                .keyType(KeyType.HASH)
                                .build())
                        .projection(Projection.builder()
                                .projectionType(ProjectionType.ALL)
                                .build())
                        .provisionedThroughput(ProvisionedThroughput.builder()
                                .readCapacityUnits(5L)
                                .writeCapacityUnits(5L)
                                .build())
                        .build()
                );
            }
            
            dynamoDbClient.createTable(requestBuilder.build());
            
            // Wait for the table to be created
            LOGGER.info("Waiting for table {} to be created...", config.getTableName());
            dynamoDbClient.waiter().waitUntilTableExists(DescribeTableRequest.builder()
                    .tableName(config.getTableName())
                    .build());
            
            // Enable TTL on the table
            dynamoDbClient.updateTimeToLive(UpdateTimeToLiveRequest.builder()
                    .tableName(config.getTableName())
                    .timeToLiveSpecification(TimeToLiveSpecification.builder()
                            .attributeName("expires")
                            .enabled(true)
                            .build())
                    .build());
            
            if (withIndexes) {
                LOGGER.info("Table {} created successfully with TTL enabled on 'expires' attribute and GSIs for tenant and country code", config.getTableName());
            } else {
                LOGGER.info("Table {} created successfully with TTL enabled on 'expires' attribute", config.getTableName());
            }
        } catch (Exception e) {
            LOGGER.error("Failed to setup DynamoDB table", e);
            throw new RuntimeException("Failed to setup DynamoDB table", e);
        }
    }

    @Override
    public void close() {
        try {
            httpClient.close();
        } catch (IOException e) {
            LOGGER.warn("Failed to close HTTP client", e);
        }
        
        dynamoDbClient.close();
    }

    private String createCacheKey(HttpRequest request) {
        String key;
        String tenant = request.getTenant();
        String countryCode = request.getCountryCode();
        
        // Only include tenant and country code in the key if configured to do so
        boolean includeTenant = config.isIncludeTenantInKey() && tenant != null;
        boolean includeCountryCode = config.isIncludeCountryCodeInKey() && countryCode != null;
        
        // If not including tenant or country in key, use null values in the key generation
        String tenantForKey = includeTenant ? tenant : null;
        String countryForKey = includeCountryCode ? countryCode : null;
        
        switch (config.getCacheKeyStrategy()) {
            case SIMPLE:
                key = includeTenant || includeCountryCode ? 
                      CacheKeyGenerator.generateKeyWithTenantAndCountry(request.getUrl(), tenantForKey, countryForKey) :
                      CacheKeyGenerator.generateSimpleKey(request.getUrl());
                break;
            case WITH_HEADERS:
                key = includeTenant || includeCountryCode ?
                      CacheKeyGenerator.generateKeyWithHeadersAndTenant(request.getUrl(), request.getHeaders(), tenantForKey, countryForKey) :
                      CacheKeyGenerator.generateKeyWithHeaders(request.getUrl(), request.getHeaders());
                break;
            case WITH_METHOD:
                key = includeTenant || includeCountryCode ?
                      CacheKeyGenerator.generateKeyWithMethodAndTenant(request.getUrl(), request.getMethod(), request.getHeaders(), tenantForKey, countryForKey) :
                      CacheKeyGenerator.generateKeyWithMethod(request.getUrl(), request.getMethod(), request.getHeaders());
                break;
            case HASHED:
                // Generate a key with method, headers, tenant and country, then hash it
                String fullKey = CacheKeyGenerator.generateKeyWithMethodAndTenant(
                    request.getUrl(), 
                    request.getMethod(), 
                    request.getHeaders(),
                    tenantForKey,
                    countryForKey
                );
                key = CacheKeyGenerator.generateHashedKey(fullKey);
                break;
            default:
                key = request.getUrl();
        }
        
        return key;
    }

    private Optional<CacheEntry> getCachedEntry(String cacheKey) {
        try {
            GetItemResponse response = dynamoDbClient.getItem(GetItemRequest.builder()
                    .tableName(config.getTableName())
                    .key(Collections.singletonMap("key", AttributeValue.builder().s(cacheKey).build()))
                    .consistentRead(true)
                    .build());
            
            if (!response.hasItem() || response.item().isEmpty()) {
                return Optional.empty();
            }
            
            Map<String, AttributeValue> item = response.item();
            
            CacheEntry.Builder builder = CacheEntry.builder()
                    .key(item.get("key").s())
                    .timestamp(Instant.ofEpochMilli(Long.parseLong(item.get("timestamp").n())))
                    .expires(Instant.ofEpochSecond(Long.parseLong(item.get("expires").n())));
            
            // Status code and headers are no longer stored in DynamoDB
            // Provide default values that will be overridden in the application code
            builder.statusCode(200);
            builder.headers(new HashMap<>());
            
            // Get tenant and country code if available
            if (item.containsKey("tenant")) {
                builder.tenant(item.get("tenant").s());
            }
            
            if (item.containsKey("country_code")) {
                builder.countryCode(item.get("country_code").s());
            }
            
            // Get body
            if (item.containsKey("body")) {
                builder.body(item.get("body").b().asByteArray());
            }
            
            return Optional.of(builder.build());
        } catch (Exception e) {
            LOGGER.warn("Failed to get item from cache: {}", cacheKey, e);
            return Optional.empty();
        }
    }

    private void cacheResponse(String cacheKey, HttpResponse response, HttpRequest request) {
        try {
            int ttl = request.getTtl() != null ? request.getTtl() : config.getDefaultTtl();
            Instant expiryTime = Instant.now().plusSeconds(ttl);
            
            CacheEntry.Builder builder = CacheEntry.builder()
                    .key(cacheKey)
                    .statusCode(response.getStatusCode())
                    .headers(response.getHeaders())
                    .body(response.getBody())
                    .timestamp(response.getTimestamp())
                    .expires(expiryTime);
            
            // Add tenant and country code if provided
            if (request.getTenant() != null) {
                builder.tenant(request.getTenant());
            }
            
            if (request.getCountryCode() != null) {
                builder.countryCode(request.getCountryCode());
            }
            
            CacheEntry cacheEntry = builder.build();
            
            Map<String, AttributeValue> item = new HashMap<>();
            item.put("key", AttributeValue.builder().s(cacheEntry.getKey()).build());
            
            if (cacheEntry.getTenant() != null) {
                item.put("tenant", AttributeValue.builder().s(cacheEntry.getTenant()).build());
            }
            
            if (cacheEntry.getCountryCode() != null) {
                item.put("country_code", AttributeValue.builder().s(cacheEntry.getCountryCode()).build());
            }
            
            // statusCode and headers are no longer stored in DynamoDB
            item.put("body", AttributeValue.builder().b(SdkBytes.fromByteArray(cacheEntry.getBody())).build());
            item.put("timestamp", AttributeValue.builder().n(String.valueOf(cacheEntry.getTimestamp())).build());
            item.put("expires", AttributeValue.builder().n(String.valueOf(cacheEntry.getExpires())).build());
            
            dynamoDbClient.putItem(PutItemRequest.builder()
                    .tableName(config.getTableName())
                    .item(item)
                    .build());
            
            LOGGER.debug("Cached response for key: {}, expires at: {}, tenant: {}, country: {}", 
                         cacheKey, expiryTime, request.getTenant(), request.getCountryCode());
        } catch (Exception e) {
            LOGGER.warn("Failed to cache response: {}", cacheKey, e);
        }
    }

    private HttpResponse executeRequest(HttpRequest request) throws IOException {
        HttpRequestBase httpRequest;
        
        // Create the appropriate HTTP request
        if ("POST".equalsIgnoreCase(request.getMethod())) {
            HttpPost httpPost = new HttpPost(request.getUrl());
            if (request.getBody() != null) {
                httpPost.setEntity(new ByteArrayEntity(request.getBody()));
            }
            httpRequest = httpPost;
        } else {
            httpRequest = new HttpGet(request.getUrl());
        }
        
        // Add headers
        for (Map.Entry<String, String> header : request.getHeaders().entrySet()) {
            httpRequest.setHeader(header.getKey(), header.getValue());
        }
        
        // Execute the request
        try (CloseableHttpResponse response = httpClient.execute(httpRequest)) {
            HttpEntity entity = response.getEntity();
            byte[] body = entity != null ? EntityUtils.toByteArray(entity) : new byte[0];
            
            // Extract headers
            Map<String, String> headers = new HashMap<>();
            Arrays.stream(response.getAllHeaders())
                    .forEach(header -> headers.put(header.getName(), header.getValue()));
            
            // Build and return the response
            return HttpResponse.builder()
                    .statusCode(response.getStatusLine().getStatusCode())
                    .headers(headers)
                    .body(body)
                    .timestamp(Instant.now())
                    .fromCache(false)
                    .build();
        }
    }

    private void batchDeleteItems(List<WriteRequest> writeRequests) {
        try {
            BatchWriteItemRequest batchWriteItemRequest = BatchWriteItemRequest.builder()
                    .requestItems(Collections.singletonMap(
                            config.getTableName(),
                            writeRequests
                    ))
                    .build();
            
            dynamoDbClient.batchWriteItem(batchWriteItemRequest);
        } catch (Exception e) {
            LOGGER.warn("Failed to batch delete items", e);
        }
    }
} 