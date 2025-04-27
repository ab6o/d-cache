package com.github.httpcache.dynamodb;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Represents an HTTP request with options for caching.
 */
public class HttpRequest {
    private final String url;
    private final Map<String, String> headers;
    private final String method;
    private final byte[] body;
    private final Integer ttl;
    private final String tenant;
    private final String countryCode;

    private HttpRequest(Builder builder) {
        this.url = builder.url;
        this.headers = Collections.unmodifiableMap(new HashMap<>(builder.headers));
        this.method = builder.method;
        this.body = builder.body;
        this.ttl = builder.ttl;
        this.tenant = builder.tenant;
        this.countryCode = builder.countryCode;
    }

    public String getUrl() {
        return url;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public String getMethod() {
        return method;
    }

    public byte[] getBody() {
        return body;
    }

    public Integer getTtl() {
        return ttl;
    }

    public String getTenant() {
        return tenant;
    }

    public String getCountryCode() {
        return countryCode;
    }

    /**
     * Creates a new builder for an HTTP request.
     *
     * @param url The request URL
     * @return A new builder
     */
    public static Builder builder(String url) {
        return new Builder(url);
    }

    /**
     * Builder for HttpRequest.
     */
    public static class Builder {
        private final String url;
        private final Map<String, String> headers = new HashMap<>();
        private String method = "GET";
        private byte[] body = null;
        private Integer ttl = null;
        private String tenant = null;
        private String countryCode = null;

        private Builder(String url) {
            this.url = url;
        }

        public Builder header(String name, String value) {
            this.headers.put(name, value);
            return this;
        }

        public Builder headers(Map<String, String> headers) {
            this.headers.putAll(headers);
            return this;
        }

        public Builder method(String method) {
            this.method = method;
            return this;
        }

        public Builder body(byte[] body) {
            this.body = body;
            return this;
        }

        public Builder ttl(Integer ttl) {
            this.ttl = ttl;
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

        public HttpRequest build() {
            return new HttpRequest(this);
        }
    }
} 