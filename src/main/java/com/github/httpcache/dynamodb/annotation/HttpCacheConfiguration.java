package com.github.httpcache.dynamodb.annotation;

import com.github.httpcache.dynamodb.HttpCache;
import com.github.httpcache.dynamodb.HttpCacheConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.util.StringUtils;

import java.util.Map;

/**
 * Configuration class that sets up HTTP caching with DynamoDB.
 */
@Configuration
@EnableAspectJAutoProxy
@ComponentScan(basePackageClasses = CacheableAspect.class)
public class HttpCacheConfiguration {
    
    private final AnnotationAttributes attributes;
    
    @Autowired
    public HttpCacheConfiguration(AnnotationMetadata importMetadata) {
        Map<String, Object> attributesMap = importMetadata.getAnnotationAttributes(
                EnableHttpCaching.class.getName(), false);
        this.attributes = AnnotationAttributes.fromMap(attributesMap);
    }
    
    /**
     * Creates an HttpCacheConfig bean based on the EnableHttpCaching annotation values.
     */
    @Bean
    public HttpCacheConfig httpCacheConfig() {
        HttpCacheConfig.Builder builder = HttpCacheConfig.builder()
                .tableName(attributes.getString("tableName"))
                .region(attributes.getString("region"))
                .defaultTtl(attributes.getNumber("defaultTtl"))
                .bypassCache(attributes.getBoolean("bypassCache"));
        
        // Get the key strategy and convert it to the HttpCacheConfig.CacheKeyStrategy
        CacheKeyStrategyType keyStrategyType = attributes.getEnum("keyStrategy");
        builder.cacheKeyStrategy(keyStrategyType.toConfigStrategy(HttpCacheConfig.CacheKeyStrategy.SIMPLE));
        
        // Set endpoint if provided
        String endpoint = attributes.getString("endpoint");
        if (StringUtils.hasText(endpoint)) {
            builder.endpoint(endpoint);
        }
        
        return builder.build();
    }
    
    /**
     * Creates an HttpCache bean using the HttpCacheConfig.
     */
    @Bean
    public HttpCache httpCache(HttpCacheConfig config) {
        return new HttpCache(config);
    }
} 