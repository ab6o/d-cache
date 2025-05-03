package com.coupang.dcache;

/**
 * Configuration for Guava-based in-memory cache.
 */
public class GuavaCacheConfig {
    private final int defaultTtl;
    private final long maximumSize;

    private GuavaCacheConfig(Builder builder) {
        this.defaultTtl = builder.defaultTtl;
        this.maximumSize = builder.maximumSize;
    }

    public int getDefaultTtl() {
        return defaultTtl;
    }

    public long getMaximumSize() {
        return maximumSize;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private int defaultTtl = 3600; // 1 hour
        private long maximumSize = 10000; // 10,000 entries

        public Builder defaultTtl(int defaultTtl) {
            this.defaultTtl = defaultTtl;
            return this;
        }

        public Builder maximumSize(long maximumSize) {
            this.maximumSize = maximumSize;
            return this;
        }

        public GuavaCacheConfig build() {
            return new GuavaCacheConfig(this);
        }
    }
} 