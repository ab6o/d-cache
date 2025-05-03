# Data Cache Library

A Java library for caching data with configurable TTL. Provides both in-memory (Guava-based) and DynamoDB implementations.

## Requirements

- Java 17 or higher
- Gradle 8.0 or higher
- AWS credentials configured in your environment (for DynamoDB implementation)

## Installation

Add the following dependency to your `build.gradle`:

```groovy
dependencies {
    implementation 'com.coupang.dcache:d-cache:1.0.0'
}
```

## Building from Source

Clone the repository and build with Gradle:

```bash
git clone https://github.com/yourusername/d-cache.git
cd d-cache
./gradlew build
```

## Usage

### In-Memory Cache (Guava)

```java
import com.coupang.dcache.Cache;
import com.coupang.dcache.GuavaCache;
import com.coupang.dcache.GuavaCacheConfig;

// Initialize the cache with configuration
GuavaCacheConfig config = GuavaCacheConfig.builder()
    .defaultTtl(3600) // Default TTL in seconds (1 hour)
    .maximumSize(10000) // Maximum number of entries
    .build();

Cache cache = new GuavaCache(config);

// Store data in cache
String key = "user:123";
byte[] userData = serializeUserData(user);
cache.put(key, userData, 1800); // TTL of 30 minutes

// Retrieve data from cache
Optional<byte[]> cachedData = cache.get(key);
if (cachedData.isPresent()) {
    User user = deserializeUserData(cachedData.get());
    // use the user data
}

// Remove from cache
cache.invalidate(key);

// Clear all cache
cache.invalidateAll();
```

### DynamoDB Cache

```java
import com.coupang.dcache.Cache;
import com.coupang.dcache.DynamoDbCache;
import com.coupang.dcache.DynamoDbCacheConfig;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

// Initialize DynamoDB client
DynamoDbClient dynamoDbClient = DynamoDbClient.builder()
    .region(Region.US_EAST_1)
    .build();

// Initialize the cache with configuration
DynamoDbCacheConfig config = DynamoDbCacheConfig.builder()
    .tableName("data-cache")
    .region("us-east-1")
    .withDynamoDbClient(dynamoDbClient)
    .build();

Cache cache = new DynamoDbCache(config);

// Store data in cache
String key = "user:123";
byte[] userData = serializeUserData(user);
cache.put(key, userData, 1800); // TTL of 30 minutes

// Retrieve data from cache
Optional<byte[]> cachedData = cache.get(key);
if (cachedData.isPresent()) {
    User user = deserializeUserData(cachedData.get());
    // use the user data
}

// Remove from cache
cache.invalidate(key);

// Clear all cache
cache.invalidateAll();
```

## Configuration Options

### Guava Cache Configuration

| Option | Description | Default |
|--------|-------------|---------|
| defaultTtl | Default cache TTL in seconds | 3600 (1 hour) |
| maximumSize | Maximum number of entries in cache | 10000 |

### DynamoDB Cache Configuration

| Option | Description | Default |
|--------|-------------|---------|
| tableName | DynamoDB table name | Required |
| region | AWS region | Required |
| credentials | AWS credentials | Default credential provider chain |
| endpoint | Custom DynamoDB endpoint | null |

## DynamoDB Table Setup

The DynamoDB implementation requires a table with the following schema:

- Partition Key: `key` (String)
- TTL Attribute: `expires` (Number)

You can create this table using the AWS CLI:

```bash
aws dynamodb create-table \
    --table-name data-cache \
    --attribute-definitions AttributeName=key,AttributeType=S \
    --key-schema AttributeName=key,KeyType=HASH \
    --provisioned-throughput ReadCapacityUnits=5,WriteCapacityUnits=5 \
    --time-to-live-specification Enabled=true,AttributeName=expires
```

## License

MIT 