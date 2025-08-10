# Cache Tutorial 101 🚀

A comprehensive step-by-step guide to caching concepts, demonstrating both **single-node** and **distributed** cache frameworks with practical examples.

## 📚 What You'll Learn

This tutorial covers essential caching concepts using the most popular frameworks:

### 🎯 Core Topics
- **Cache Replacement Policies**: LRU, LFU, FIFO
- **Cache Write Policies**: Write-through, Write-around, Write-behind, Write-back
- **Cache Invalidation Strategies**: TTL expiration, Purge, Refresh, Ban, Stale-while-revalidate
- **Single-node vs Distributed Caching**
- **Legacy vs Modern Redis clients**

### 🛠️ Frameworks Covered
- **[Caffeine](https://github.com/ben-manes/caffeine)**: Most popular single-node cache framework
- **[Redis](https://redis.io/)**: Most popular distributed cache framework
  - **Jedis**: Traditional blocking client
  - **Lettuce**: Modern non-blocking, reactive client
  - **Redisson**: High-level distributed features

## 🏗️ Project Structure

```
src/main/java/
├── tutorial/
│   └── CacheTutorialRunner.java     # Main tutorial entry point
├── singlenode/                      # Caffeine demos
│   ├── CaffeineBasicDemo.java       # Basic Caffeine operations
│   ├── CacheReplacementPolicyDemo.java  # LRU, LFU, FIFO policies
│   ├── CacheWritePolicyDemo.java    # Write policies & strategies
│   └── CacheInvalidationStrategyDemo.java  # TTL, purge, refresh methods
└── distributed/                     # Redis demos
    ├── RedisBasicDemo.java          # Traditional Jedis client
    ├── RedisModernDemo.java         # Modern Lettuce client
    └── RedisCachePoliciesDemo.java  # Redis-specific cache policies
```

## 🚀 Quick Start

### Prerequisites
- Java 11 or higher
- Maven 3.6+
- Redis server (for distributed cache examples)

### Setup
1. Clone the repository:
   ```bash
   git clone <repository-url>
   cd tyan-cache-101
   ```

2. Install dependencies:
   ```bash
   mvn clean compile
   ```

3. Start Redis server (for distributed examples):
   ```bash
   # macOS with Homebrew
   brew install redis
   redis-server
   
   # Or using Docker
   docker run -d -p 6379:6379 redis:7-alpine
   ```

### Running the Tutorials

#### Run Complete Tutorial
```bash
mvn exec:java -Dexec.mainClass="tutorial.CacheTutorialRunner"
```

#### Run Specific Sections
```bash
# Caffeine single-node cache only
mvn exec:java -Dexec.mainClass="tutorial.CacheTutorialRunner" -Dexec.args="caffeine"

# Redis distributed cache only
mvn exec:java -Dexec.mainClass="tutorial.CacheTutorialRunner" -Dexec.args="redis"

# Modern Redis with Lettuce
mvn exec:java -Dexec.mainClass="tutorial.CacheTutorialRunner" -Dexec.args="redis-modern"

# Redis client comparison
mvn exec:java -Dexec.mainClass="tutorial.CacheTutorialRunner" -Dexec.args="comparison"
```

#### Run Individual Demos
```bash
# Caffeine basics
mvn exec:java -Dexec.mainClass="singlenode.CaffeineBasicDemo"

# Cache replacement policies (LRU, LFU, FIFO)
mvn exec:java -Dexec.mainClass="singlenode.CacheReplacementPolicyDemo"

# Cache write policies
mvn exec:java -Dexec.mainClass="singlenode.CacheWritePolicyDemo"

# Cache invalidation strategies
mvn exec:java -Dexec.mainClass="singlenode.CacheInvalidationStrategyDemo"

# Redis basic operations (Jedis)
mvn exec:java -Dexec.mainClass="distributed.RedisBasicDemo"

# Modern Redis operations (Lettuce)
mvn exec:java -Dexec.mainClass="distributed.RedisModernDemo"

# Redis cache policies
mvn exec:java -Dexec.mainClass="distributed.RedisCachePoliciesDemo"
```

## 📖 Tutorial Content

### 1. Single-Node Caching (Caffeine)

#### 🔧 Basic Operations (`CaffeineBasicDemo`)
- Cache creation and configuration
- Put/Get operations
- Loading cache with automatic population
- Asynchronous cache operations
- Cache statistics and monitoring

#### 🔄 Cache Replacement Policies (`CacheReplacementPolicyDemo`)
- **LRU (Least Recently Used)**: Evicts least recently accessed items
- **LFU (Least Frequently Used)**: Evicts least frequently accessed items  
- **FIFO (First In, First Out)**: Evicts oldest items first
- Practical examples and performance comparison

#### ✍️ Cache Write Policies (`CacheWritePolicyDemo`)
- **Write-through**: Synchronous write to cache and storage
- **Write-around**: Write directly to storage, bypass cache
- **Write-behind**: Asynchronous write to storage after cache
- **Write-back**: Write to cache only, periodic storage sync

#### 🔄 Cache Invalidation Strategies (`CacheInvalidationStrategyDemo`)
- **TTL Expiration**: Time-based automatic expiration
- **Manual Purge**: Explicit cache clearing
- **Refresh**: Proactive cache updates
- **Ban**: Pattern-based invalidation
- **Stale-while-revalidate**: Serve stale data during refresh

### 2. Distributed Caching (Redis)

#### 🏛️ Traditional Redis (`RedisBasicDemo` - Jedis)
- Connection setup with JedisPool
- Basic operations (strings, numbers, existence checks)
- Data types: Lists, Sets, Hashes, Sorted Sets
- Expiration and TTL management
- Pipeline operations for batch processing
- Transactions (MULTI/EXEC)
- Pub/Sub messaging
- Connection pooling

#### 🚀 Modern Redis (`RedisModernDemo` - Lettuce)
- Modern connection setup
- Synchronous operations (traditional style)
- **Asynchronous operations** (non-blocking)
- **Reactive operations** (reactive streams)
- Advanced connection pooling
- Performance comparison with traditional approach

#### 📋 Redis Cache Policies (`RedisCachePoliciesDemo`)
- Redis-specific eviction policies
- Memory management strategies
- Advanced Redis features

### 3. Legacy vs Modern Comparison

The tutorial demonstrates the evolution from traditional blocking clients to modern non-blocking, reactive clients:

| Feature | Jedis (Legacy) | Lettuce (Modern) |
|---------|---------------|------------------|
| **Blocking** | ✅ Synchronous only | ✅ Sync + Async + Reactive |
| **Thread Safety** | ❌ Connection per thread | ✅ Thread-safe connections |
| **Performance** | Good for simple use cases | ⚡ Superior for high concurrency |
| **Resource Usage** | Higher connection overhead | 🔋 Efficient connection sharing |
| **Learning Curve** | Simple, familiar | Moderate (reactive concepts) |

## 🎯 Key Learning Outcomes

After completing this tutorial, you'll understand:

1. **When to use single-node vs distributed caching**
2. **How different replacement policies affect performance**
3. **Trade-offs between write policies**
4. **Effective cache invalidation strategies**
5. **Modern vs traditional Redis client approaches**
6. **Performance optimization techniques**
7. **Real-world caching patterns and best practices**

## 🔧 Dependencies

### Core Dependencies
```xml
<!-- Caffeine (Single-node cache) -->
<dependency>
    <groupId>com.github.ben-manes.caffeine</groupId>
    <artifactId>caffeine</artifactId>
    <version>3.1.8</version>
</dependency>

<!-- Jedis (Traditional Redis client) -->
<dependency>
    <groupId>redis.clients</groupId>
    <artifactId>jedis</artifactId>
    <version>5.0.2</version>
</dependency>

<!-- Lettuce (Modern Redis client) -->
<dependency>
    <groupId>io.lettuce</groupId>
    <artifactId>lettuce-core</artifactId>
    <version>6.7.1.RELEASE</version>
</dependency>

<!-- Redisson (High-level Redis client) -->
<dependency>
    <groupId>org.redisson</groupId>
    <artifactId>redisson</artifactId>
    <version>3.24.3</version>
</dependency>

<!-- Apache Commons Pool (Connection pooling) -->
<dependency>
    <groupId>org.apache.commons</groupId>
    <artifactId>commons-pool2</artifactId>
    <version>2.12.0</version>
</dependency>
```

## 📈 Performance Notes

The tutorial includes practical performance demonstrations:
- **Pipeline vs Individual Commands**: Up to 10x performance improvement
- **Async vs Sync Operations**: Better resource utilization
- **Connection Pooling**: Reduced connection overhead
- **Cache Hit Ratios**: Impact on application performance

## 🤝 Contributing

Feel free to contribute improvements, additional examples, or bug fixes:
1. Fork the repository
2. Create a feature branch
3. Add your improvements with tests
4. Submit a pull request

## 📝 License

This project is intended for educational purposes. Feel free to use the code examples in your own projects.

## 🔗 Additional Resources

- [Caffeine Documentation](https://github.com/ben-manes/caffeine/wiki)
- [Redis Documentation](https://redis.io/documentation)
- [Lettuce Documentation](https://lettuce.io/core/release/reference/)
- [Jedis Documentation](https://github.com/redis/jedis)
- [Cache Patterns & Best Practices](https://redis.io/docs/manual/patterns/)

---

**Happy Caching! 🎉**
