package com.coupang.dcache;

import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import com.coupang.dcache.serializer.CacheEntrySerializer;
import com.coupang.dcache.serializer.JsonCacheEntrySerializer;
import com.coupang.dcache.serializer.CacheKeySerializer;
import java.time.Duration;
import java.util.Objects;

/**
 * Configuration options for HttpCache.
 */
public class HttpCacheConfig {
    
    /**
     * Cache key strategy options.
     */
    public enum CacheKeyStrategy {
        /**
         * Simple URL-based key
         */
        SIMPLE,
        
        /**
         * URL + headers based key
         */
        WITH_HEADERS,
        
        /**
         * Method + URL + headers based key
         */
        WITH_METHOD,
        
        /**
         * Hashed key for potentially long keys
         */
        HASHED
    }
    
    private final String tableName;
    private final String region;
    private final int defaultTtl;
    private final AwsCredentialsProvider credentialsProvider;
    private final String endpoint;
    private final boolean bypassCache;
    private final CacheKeyStrategy cacheKeyStrategy;
    private final String defaultTenant;
    private final String defaultCountryCode;
    private final boolean includeTenantInKey;
    private final boolean includeCountryCodeInKey;
    private final DynamoDbClient dynamoDbClient;
    private final long ttlInSeconds;
    private final CacheEntrySerializer serializer;
    private final boolean createTableIfNotExists;
    private final long readCapacityUnits;
    private final long writeCapacityUnits;
    private final boolean enableMultiTenancy;
    private final boolean includeCountryCode;

    private HttpCacheConfig(Builder builder) {
        this.tableName = builder.tableName;
        this.region = builder.region;
        this.defaultTtl = builder.defaultTtl;
        this.credentialsProvider = builder.credentialsProvider;
        this.endpoint = builder.endpoint;
        this.bypassCache = builder.bypassCache;
        this.cacheKeyStrategy = builder.cacheKeyStrategy;
        this.defaultTenant = builder.defaultTenant;
        this.defaultCountryCode = builder.defaultCountryCode;
        this.includeTenantInKey = builder.includeTenantInKey;
        this.includeCountryCodeInKey = builder.includeCountryCodeInKey;
        this.dynamoDbClient = builder.dynamoDbClient;
        this.ttlInSeconds = builder.ttlInSeconds;
        this.serializer = builder.serializer;
        this.createTableIfNotExists = builder.createTableIfNotExists;
        this.readCapacityUnits = builder.readCapacityUnits;
        this.writeCapacityUnits = builder.writeCapacityUnits;
        this.enableMultiTenancy = builder.enableMultiTenancy;
        this.includeCountryCode = builder.includeCountryCode;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getTableName() {
        return tableName;
    }

    public String getRegion() {
        return region;
    }

    public Region getAwsRegion() {
        return Region.of(region);
    }

    public int getDefaultTtl() {
        return defaultTtl;
    }

    public AwsCredentialsProvider getCredentialsProvider() {
        return credentialsProvider;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public boolean isBypassCache() {
        return bypassCache;
    }

    public CacheKeyStrategy getCacheKeyStrategy() {
        return cacheKeyStrategy;
    }

    public String getDefaultTenant() {
        return defaultTenant;
    }

    public String getDefaultCountryCode() {
        return defaultCountryCode;
    }

    public boolean isIncludeTenantInKey() {
        return includeTenantInKey;
    }

    public boolean isIncludeCountryCodeInKey() {
        return includeCountryCodeInKey;
    }

    public DynamoDbClient getDynamoDbClient() {
        return dynamoDbClient;
    }

    public long getTtlInSeconds() {
        return ttlInSeconds;
    }

    public CacheEntrySerializer getSerializer() {
        return serializer;
    }

    public boolean isCreateTableIfNotExists() {
        return createTableIfNotExists;
    }

    public long getReadCapacityUnits() {
        return readCapacityUnits;
    }

    public long getWriteCapacityUnits() {
        return writeCapacityUnits;
    }

    public boolean isEnableMultiTenancy() {
        return enableMultiTenancy;
    }

    public boolean isIncludeCountryCode() {
        return includeCountryCode;
    }

    /**
     * Builder for HttpCacheConfig.
     */
    public static class Builder {
        private String tableName = "http-cache";
        private String region = "us-east-1";
        private int defaultTtl = 3600;
        private AwsCredentialsProvider credentialsProvider = DefaultCredentialsProvider.create();
        private String endpoint = null;
        private boolean bypassCache = false;
        private CacheKeyStrategy cacheKeyStrategy = CacheKeyStrategy.SIMPLE;
        private String defaultTenant = null;
        private String defaultCountryCode = null;
        private boolean includeTenantInKey = true;
        private boolean includeCountryCodeInKey = true;
        private DynamoDbClient dynamoDbClient;
        private long ttlInSeconds = 86400; // 24 hours
        private CacheEntrySerializer serializer = new JsonCacheEntrySerializer();
        private boolean createTableIfNotExists = true;
        private long readCapacityUnits = 5;
        private long writeCapacityUnits = 5;
        private boolean enableMultiTenancy = false;
        private boolean includeCountryCode = false;

        private Builder() {
        }

        public Builder tableName(String tableName) {
            this.tableName = tableName;
            return this;
        }

        public Builder region(String region) {
            this.region = region;
            return this;
        }

        public Builder defaultTtl(int defaultTtl) {
            this.defaultTtl = defaultTtl;
            return this;
        }

        public Builder credentialsProvider(AwsCredentialsProvider credentialsProvider) {
            this.credentialsProvider = credentialsProvider;
            return this;
        }

        public Builder endpoint(String endpoint) {
            this.endpoint = endpoint;
            return this;
        }

        public Builder bypassCache(boolean bypassCache) {
            this.bypassCache = bypassCache;
            return this;
        }

        public Builder cacheKeyStrategy(CacheKeyStrategy cacheKeyStrategy) {
            this.cacheKeyStrategy = cacheKeyStrategy;
            return this;
        }

        public Builder defaultTenant(String defaultTenant) {
            this.defaultTenant = defaultTenant;
            return this;
        }

        public Builder defaultCountryCode(String defaultCountryCode) {
            this.defaultCountryCode = defaultCountryCode;
            return this;
        }

        public Builder includeTenantInKey(boolean includeTenantInKey) {
            this.includeTenantInKey = includeTenantInKey;
            return this;
        }

        public Builder includeCountryCodeInKey(boolean includeCountryCodeInKey) {
            this.includeCountryCodeInKey = includeCountryCodeInKey;
            return this;
        }

        public Builder withDynamoDbClient(DynamoDbClient dynamoDbClient) {
            this.dynamoDbClient = dynamoDbClient;
            return this;
        }

        public Builder withTtlInSeconds(long ttlInSeconds) {
            this.ttlInSeconds = ttlInSeconds;
            return this;
        }

        public Builder withSerializer(CacheEntrySerializer serializer) {
            this.serializer = serializer;
            return this;
        }

        public Builder withCreateTableIfNotExists(boolean createTableIfNotExists) {
            this.createTableIfNotExists = createTableIfNotExists;
            return this;
        }

        public Builder withReadCapacityUnits(long readCapacityUnits) {
            this.readCapacityUnits = readCapacityUnits;
            return this;
        }

        public Builder withWriteCapacityUnits(long writeCapacityUnits) {
            this.writeCapacityUnits = writeCapacityUnits;
            return this;
        }

        public Builder withEnableMultiTenancy(boolean enableMultiTenancy) {
            this.enableMultiTenancy = enableMultiTenancy;
            return this;
        }

        public Builder withIncludeCountryCode(boolean includeCountryCode) {
            this.includeCountryCode = includeCountryCode;
            return this;
        }

        public HttpCacheConfig build() {
            if (dynamoDbClient == null) {
                throw new IllegalArgumentException("DynamoDB client must be provided");
            }
            return new HttpCacheConfig(this);
        }
    }
} 