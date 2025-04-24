# DynamoDB Master Data Cache

A Java library for caching data in DynamoDB with configurable TTL. While originally designed for HTTP responses, it can be used to cache any type of data.

## Requirements

- Java 17 or higher
- Maven 3.6 or higher
- AWS credentials configured in your environment

## Installation

Add the following dependency to your pom.xml:

```xml
<dependency>
    <groupId>com.github.httpcache</groupId>
    <artifactId>dynamodb-http-cache</artifactId>
    <version>1.0.0</version>
</dependency>
```

## Building from Source

Clone the repository and build with Maven:

```bash
git clone https://github.com/yourusername/dynamodb-http-cache.git
cd dynamodb-http-cache
mvn clean install
```

## Running the Example

The library includes examples that demonstrate its functionality:

```bash
# Direct API usage example
mvn exec:java -Dexec.mainClass="com.github.httpcache.dynamodb.example.Example"

# Annotation-based caching example (requires Spring)
mvn exec:java -Dexec.mainClass="com.github.httpcache.dynamodb.example.AnnotationExample"
```

Note: You'll need AWS credentials configured in your environment to run the examples.

## Usage

### HTTP Caching Usage

```java
import com.github.httpcache.dynamodb.MasterDataCache;
import com.github.httpcache.dynamodb.MasterDataCacheConfig;

// Initialize the cache with configuration
MasterDataCacheConfig config = MasterDataCacheConfig.builder()
    .tableName("master-data-cache")
    .region("us-east-1")
    .defaultTtl(3600) // Default TTL in seconds (1 hour)
    .build();

MasterDataCache cache = new MasterDataCache(config);

// Fetch with caching
HttpResponse response = cache.fetch("https://api.example.com/data", options -> {
    options.ttl(300); // Override TTL for this request (5 minutes)
    options.header("Authorization", "Bearer token");
});

// Clear cache entry
cache.invalidate("https://api.example.com/data");

// Clear all cache
cache.invalidateAll();
```

### Generic Data Caching

```java
import com.github.httpcache.dynamodb.MasterDataCache;
import com.github.httpcache.dynamodb.MasterDataCacheConfig;

// Initialize the cache with configuration
MasterDataCacheConfig config = MasterDataCacheConfig.builder()
    .tableName("master-data-cache")
    .region("us-east-1")
    .defaultTtl(3600) // Default TTL in seconds (1 hour)
    .build();

MasterDataCache cache = new MasterDataCache(config);

// Store data in cache
String key = "user:123";
byte[] userData = serializeUserData(user);
cache.put(key, userData, 1800, "tenant1", "US");

// Retrieve data from cache
Optional<byte[]> cachedData = cache.get(key);
if (cachedData.isPresent()) {
    User user = deserializeUserData(cachedData.get());
    // use the user data
}

// Invalidate by key
cache.invalidateByKey(key);
```

### Annotation-Based Caching (Spring)

The library also provides annotation-based caching using Spring AOP. To use this feature:

1. Add the `@EnableHttpCaching` annotation to your Spring configuration class:

```java
import com.github.httpcache.dynamodb.annotation.EnableHttpCaching;
import com.github.httpcache.dynamodb.annotation.CacheKeyStrategyType;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableHttpCaching(
    tableName = "master-data-cache",
    region = "us-east-1",
    defaultTtl = 3600,
    keyStrategy = CacheKeyStrategyType.SIMPLE
)
public class AppConfig {
}
```

2. Use the `@Cacheable` and `@CacheEvict` annotations on your methods:

```java
import com.github.httpcache.dynamodb.HttpResponse;
import com.github.httpcache.dynamodb.annotation.Cacheable;
import com.github.httpcache.dynamodb.annotation.CacheEvict;
import com.github.httpcache.dynamodb.annotation.CacheKeyStrategyType;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Map;

@Service
public class ApiService {

    // Basic caching
    @Cacheable
    public HttpResponse fetchData(String url) throws IOException {
        // Make HTTP request and return response
    }
    
    // With custom TTL
    @Cacheable(ttl = 60) // 1 minute TTL
    public HttpResponse fetchWithCustomTtl(String url) throws IOException {
        // Make HTTP request and return response
    }
    
    // With headers included in cache key
    @Cacheable(
        keyStrategy = CacheKeyStrategyType.WITH_HEADERS,
        includeHeaders = {"X-Custom-Header"}
    )
    public HttpResponse fetchWithHeaders(String url, Map<String, String> headers) throws IOException {
        // Make HTTP request with headers and return response
    }
    
    // With condition (only cache if shouldCache is true)
    @Cacheable(condition = "#shouldCache == true")
    public HttpResponse fetchConditionally(String url, boolean shouldCache) throws IOException {
        // Make HTTP request and return response
    }
    
    // Evict a specific URL from cache
    @CacheEvict
    public void evictCache(String url) {
        // Method implementation (optional)
    }
    
    // Evict all entries from cache
    @CacheEvict(allEntries = true)
    public void evictAllCache() {
        // Method implementation (optional)
    }
}
```

The annotations support:
- Custom cache keys and TTL values
- Including/excluding parameters from cache keys
- Including specific HTTP headers in cache keys
- Conditional caching using SpEL expressions
- Cache eviction before or after method execution

See the `AnnotationExample` class for a complete example.

## Configuration Options

| Option | Description | Default |
|--------|-------------|---------|
| tableName | DynamoDB table name | "master-data-cache" |
| region | AWS region | "us-east-1" |
| defaultTtl | Default cache TTL in seconds | 3600 (1 hour) |
| credentials | AWS credentials | Default credential provider chain |
| endpoint | Custom DynamoDB endpoint | null |
| bypassCache | Option to bypass cache globally | false |
| cacheKeyStrategy | Strategy for generating cache keys | CacheKeyStrategy.SIMPLE |

### Cache Key Strategies

The library supports multiple strategies for generating cache keys:

| Strategy | Description | Use Case |
|----------|-------------|----------|
| SIMPLE | Uses just the URL as the cache key | Simple GET requests without varying headers |
| WITH_HEADERS | Combines the URL and headers | Requests where headers affect the response (e.g., Accept, Accept-Language) |
| WITH_METHOD | Combines HTTP method, URL, and headers | When caching different HTTP methods to the same URL |
| HASHED | Generates a SHA-256 hash of method+URL+headers | When cache keys might be very long or for more privacy |

Example of setting a cache key strategy:

```java
MasterDataCacheConfig config = MasterDataCacheConfig.builder()
    .tableName("master-data-cache")
    .cacheKeyStrategy(MasterDataCacheConfig.CacheKeyStrategy.WITH_HEADERS)
    .build();
```

## DynamoDB Table Setup

The library requires a DynamoDB table with the following schema:

- Partition Key: `key` (String)
- TTL Attribute: `expires` (Number)
- Optional Attributes: `tenant` and `country_code` (for multi-tenancy support)

Note: As of the latest version, `statusCode` and `headers` are no longer stored in the DynamoDB table to reduce item size and storage costs. These values are maintained in the CacheEntry object in memory.

You can create this table in several ways:

### 1. Using the Library

```java
cache.setupTable();
```

### 2. Using the Included Shell Script

The repository includes a shell script (`setup-table.sh`) to create or delete the DynamoDB table:

```bash
# Create a table
./setup-table.sh --table-name master-data-cache --region us-east-1

# Delete a table
./setup-table.sh --table-name master-data-cache --region us-east-1 --delete

# For help and more options
./setup-table.sh --help
```

### 3. Using CloudFormation

The repository includes a CloudFormation template (`dynamodb-table-template.yaml`) that you can use to set up the DynamoDB table:

```bash
aws cloudformation deploy \
  --template-file dynamodb-table-template.yaml \
  --stack-name master-data-cache-stack \
  --parameter-overrides \
    TableName=master-data-cache \
    ReadCapacityUnits=5 \
    WriteCapacityUnits=5 \
    TimeToLiveAttribute=expires
```

## License

MIT 