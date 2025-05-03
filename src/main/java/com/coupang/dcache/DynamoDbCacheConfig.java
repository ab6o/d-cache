package com.coupang.dcache;

import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

/**
 * Configuration for DynamoDB-based cache.
 */
public class DynamoDbCacheConfig {
    private final String tableName;
    private final String region;
    private final DynamoDbClient dynamoDbClient;

    private DynamoDbCacheConfig(Builder builder) {
        this.tableName = builder.tableName;
        this.region = builder.region;
        this.dynamoDbClient = builder.dynamoDbClient;
    }

    public String getTableName() {
        return tableName;
    }

    public String getRegion() {
        return region;
    }

    public DynamoDbClient getDynamoDbClient() {
        return dynamoDbClient;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String tableName;
        private String region;
        private DynamoDbClient dynamoDbClient;

        public Builder tableName(String tableName) {
            this.tableName = tableName;
            return this;
        }

        public Builder region(String region) {
            this.region = region;
            return this;
        }

        public Builder withDynamoDbClient(DynamoDbClient dynamoDbClient) {
            this.dynamoDbClient = dynamoDbClient;
            return this;
        }

        public DynamoDbCacheConfig build() {
            if (tableName == null || tableName.isEmpty()) {
                throw new IllegalArgumentException("Table name is required");
            }
            if (region == null || region.isEmpty()) {
                throw new IllegalArgumentException("Region is required");
            }
            if (dynamoDbClient == null) {
                throw new IllegalArgumentException("DynamoDB client is required");
            }
            return new DynamoDbCacheConfig(this);
        }
    }
} 