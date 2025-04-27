package com.github.httpcache.dynamodb;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Represents a cached HTTP response entry in DynamoDB.
 */
public class CacheEntry {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @JsonProperty("key")
    private String key;

    @JsonProperty("tenant")
    private String tenant;

    @JsonProperty("country_code")
    private String countryCode;

    @JsonProperty("status_code")
    private int statusCode;

    @JsonProperty("headers")
    private Map<String, String> headers;

    @JsonProperty("body")
    private byte[] body;

    @JsonProperty("timestamp")
    private long timestamp;

    @JsonProperty("expires")
    private long expires;

    // Default constructor for Jackson
    public CacheEntry() {
    }

    private CacheEntry(Builder builder) {
        this.key = builder.key;
        this.tenant = builder.tenant;
        this.countryCode = builder.countryCode;
        this.statusCode = builder.statusCode;
        this.headers = builder.headers;
        this.body = builder.body;
        this.timestamp = builder.timestamp.toEpochMilli();
        this.expires = builder.expires.getEpochSecond();
    }

    /**
     * Converts this cache entry to a DynamoDB item.
     *
     * @return A map representing the DynamoDB item
     */
    public Map<String, Object> toDynamoDbItem() {
        Map<String, Object> item = new HashMap<>();
        item.put("key", key);
        
        if (tenant != null) {
            item.put("tenant", tenant);
        }
        
        if (countryCode != null) {
            item.put("country_code", countryCode);
        }
        
        // statusCode and headers are no longer stored in DynamoDB
        item.put("body", body);
        item.put("timestamp", timestamp);
        item.put("expires", expires);
        
        return item;
    }

    /**
     * Creates a HttpResponse from this cache entry.
     *
     * @return The HTTP response
     */
    public HttpResponse toHttpResponse() {
        return HttpResponse.builder()
                .statusCode(statusCode)
                .headers(headers)
                .body(body)
                .timestamp(Instant.ofEpochMilli(timestamp))
                .fromCache(true)
                .build();
    }

    public String getKey() {
        return key;
    }

    public String getTenant() {
        return tenant;
    }

    public String getCountryCode() {
        return countryCode;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public byte[] getBody() {
        return body;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public long getExpires() {
        return expires;
    }

    /**
     * Creates a new builder for a cache entry.
     *
     * @return A new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for CacheEntry.
     */
    public static class Builder {
        private String key;
        private String tenant;
        private String countryCode;
        private int statusCode;
        private Map<String, String> headers = new HashMap<>();
        private byte[] body;
        private Instant timestamp = Instant.now();
        private Instant expires;

        private Builder() {
        }

        public Builder key(String key) {
            this.key = key;
            return this;
        }

        public Builder tenant(String tenant) {
            this.tenant = tenant;
            return this;
        }

        public Builder countryCode(String countryCode) {
            this.countryCode = countryCode;
            return this;
        }

        public Builder statusCode(int statusCode) {
            this.statusCode = statusCode;
            return this;
        }

        public Builder headers(Map<String, String> headers) {
            this.headers = headers;
            return this;
        }

        public Builder body(byte[] body) {
            this.body = body;
            return this;
        }

        public Builder timestamp(Instant timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public Builder expires(Instant expires) {
            this.expires = expires;
            return this;
        }

        public CacheEntry build() {
            return new CacheEntry(this);
        }
    }
} 