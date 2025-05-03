package com.coupang.dcache.example;

import com.coupang.dcache.HttpCache;
import com.coupang.dcache.HttpCacheConfig;
import com.coupang.dcache.annotation.EnableHttpCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.DynamoDbException;

import java.net.URI;

@Configuration
@EnableAspectJAutoProxy
@EnableHttpCaching(
    tableName = "example-http-cache",
    region = "us-east-1",
    endpoint = "http://localhost:8000"
)
public class ExampleConfiguration {

    @Bean
    public DynamoDbClient dynamoDbClient() {
        // Use static credentials for testing
        StaticCredentialsProvider credentialsProvider = StaticCredentialsProvider.create(
            AwsBasicCredentials.create("test", "test")
        );

        return DynamoDbClient.builder()
                .endpointOverride(URI.create("http://localhost:8000"))
                .region(Region.US_EAST_1)
                .credentialsProvider(credentialsProvider)
                .build();
    }

    @Bean
    public HttpCacheConfig httpCacheConfig(DynamoDbClient dynamoDbClient) {
        return HttpCacheConfig.builder()
                .tableName("example-http-cache")
                .region("us-east-1")
                .withDynamoDbClient(dynamoDbClient)
                .build();
    }

    @Bean
    public HttpCache httpCache(HttpCacheConfig config) {
        try {
            HttpCache cache = new HttpCache(config);
            cache.setupTable(true); // Create table with indexes if it doesn't exist
            return cache;
        } catch (DynamoDbException e) {
            System.err.println("Failed to set up DynamoDB table: " + e.getMessage());
            System.err.println("Make sure DynamoDB Local is running on port 8000");
            throw e;
        }
    }
} 