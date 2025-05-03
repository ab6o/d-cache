package com.coupang.dcache.annotation;

import com.coupang.dcache.HttpCache;
import com.coupang.dcache.HttpCacheConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportAware;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.util.StringUtils;

/**
 * Configuration class that sets up HTTP caching with DynamoDB.
 */
@Configuration
public class HttpCacheConfiguration implements ImportAware {
    private AnnotationAttributes enableHttpCaching;

    @Override
    public void setImportMetadata(AnnotationMetadata importMetadata) {
        this.enableHttpCaching = AnnotationAttributes.fromMap(
            importMetadata.getAnnotationAttributes(EnableHttpCaching.class.getName(), false)
        );
    }

    /**
     * Creates an HttpCacheConfig bean based on the EnableHttpCaching annotation values.
     */
    @Bean
    public HttpCacheConfig httpCacheConfig(@Autowired(required = false) HttpCacheConfig config) {
        if (config != null) {
            return config;
        }

        String tableName = enableHttpCaching.getString("tableName");
        return HttpCacheConfig.builder()
                .tableName(tableName)
                .region(enableHttpCaching.getString("region"))
                .defaultTtl(enableHttpCaching.getNumber("defaultTtl"))
                .bypassCache(enableHttpCaching.getBoolean("bypassCache"))
                .build();
    }
    
    /**
     * Creates an HttpCache bean using the HttpCacheConfig.
     */
    @Bean
    public HttpCache httpCache(HttpCacheConfig config) {
        return new HttpCache(config);
    }

    @Bean
    public CacheableAspect cacheableAspect(HttpCache httpCache) {
        return new CacheableAspect(httpCache);
    }
} 