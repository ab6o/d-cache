package com.coupang.dcache;

import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Represents an HTTP response.
 */
public class HttpResponse {
    private final int statusCode;
    private final Map<String, String> headers;
    private final byte[] body;
    private final Instant timestamp;
    private final boolean fromCache;

    private HttpResponse(Builder builder) {
        this.statusCode = builder.statusCode;
        this.headers = Collections.unmodifiableMap(new HashMap<>(builder.headers));
        this.body = builder.body;
        this.timestamp = builder.timestamp != null ? builder.timestamp : Instant.now();
        this.fromCache = builder.fromCache;
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

    public Instant getTimestamp() {
        return timestamp;
    }

    public boolean isFromCache() {
        return fromCache;
    }

    /**
     * Creates a new builder for an HTTP response.
     *
     * @return A new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for HttpResponse.
     */
    public static class Builder {
        private int statusCode;
        private final Map<String, String> headers = new HashMap<>();
        private byte[] body;
        private Instant timestamp;
        private boolean fromCache;

        private Builder() {
        }

        public Builder statusCode(int statusCode) {
            this.statusCode = statusCode;
            return this;
        }

        public Builder header(String name, String value) {
            this.headers.put(name, value);
            return this;
        }

        public Builder headers(Map<String, String> headers) {
            this.headers.putAll(headers);
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

        public Builder fromCache(boolean fromCache) {
            this.fromCache = fromCache;
            return this;
        }

        public HttpResponse build() {
            return new HttpResponse(this);
        }
    }
} 