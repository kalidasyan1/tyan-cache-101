package distributed;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.Pipeline;
import redis.clients.jedis.params.SetParams;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Demonstrates Redis cache policies and invalidation strategies for distributed caching
 */
public class RedisCachePoliciesDemo {
  private JedisPool jedisPool;
  private Map<String, String> database;
  private ScheduledExecutorService executor;

  public static void main(String[] args) {
    RedisCachePoliciesDemo demo = new RedisCachePoliciesDemo();

    System.out.println("=== Redis Distributed Cache Policies Demo ===\n");

    demo.initialize();
    demo.demoWriteThroughPolicy();
    demo.demoWriteAroundPolicy();
    demo.demoWriteBehindPolicy();
    demo.demoWriteBackPolicy();
    demo.demoCacheInvalidationStrategies();
    demo.demoDistributedCacheFeatures();
    demo.cleanup();
  }

  public void initialize() {
    System.out.println("1. Initializing Redis Cache Policies Demo:");

    try {
      JedisPoolConfig config = new JedisPoolConfig();
      config.setMaxTotal(20);
      config.setMaxIdle(10);
      jedisPool = new JedisPool(config, "localhost", 6379);

      database = new HashMap<>();
      executor = Executors.newScheduledThreadPool(3);

      try (Jedis jedis = jedisPool.getResource()) {
        jedis.ping();
        jedis.flushDB(); // Clear Redis for clean demo
        System.out.println("   ✓ Redis connected and cleared");
      }

    } catch (Exception e) {
      System.err.println("   ✗ Redis connection failed: " + e.getMessage());
      System.err.println("   Please ensure Redis is running on localhost:6379");
      return;
    }
    System.out.println();
  }

  /**
   * Write-Through: Synchronous writes to both cache and database
   */
  public void demoWriteThroughPolicy() {
    System.out.println("2. Write-Through Cache Policy (Redis):");
    System.out.println("   - All writes go through cache to database");
    System.out.println("   - Guarantees cache-database consistency");

    try (Jedis jedis = jedisPool.getResource()) {
      String key = "user:writethrough:123";
      String value = "Alice Johnson";

      long startTime = System.currentTimeMillis();

      // Write-through implementation
      jedis.set(key, value);
      simulateDatabaseWrite(key, value);

      long duration = System.currentTimeMillis() - startTime;

      System.out.println("   Wrote '" + value + "' via write-through");
      System.out.println("   Cache value: " + jedis.get(key));
      System.out.println("   Database value: " + database.get(key));
      System.out.println("   Write duration: " + duration + "ms");
      System.out.println("   ✓ Cache and database are consistent");
    }
    System.out.println();
  }

  /**
   * Write-Around: Writes bypass cache, go directly to database
   */
  public void demoWriteAroundPolicy() {
    System.out.println("3. Write-Around Cache Policy (Redis):");
    System.out.println("   - Writes bypass cache, reducing cache pollution");
    System.out.println("   - Useful for write-heavy, read-light workloads");

    try (Jedis jedis = jedisPool.getResource()) {
      String key = "batch:writearound:456";
      String value = "Batch Processing Data";

      System.out.println("   Writing '" + value + "' with write-around...");

      // Write-around: skip cache, write directly to database
      simulateDatabaseWrite(key, value);

      System.out.println("   Cache value (should be null): " + jedis.get(key));
      System.out.println("   Database value: " + database.get(key));

      // Subsequent read will cache miss and load from database
      System.out.println("   Reading from cache (cache miss)...");
      String cachedValue = jedis.get(key);
      if (cachedValue == null) {
        cachedValue = database.get(key);
        if (cachedValue != null) {
          jedis.set(key, cachedValue);
          System.out.println("   Loaded from database and cached: " + cachedValue);
        }
      }

      System.out.println("   Now cache value: " + jedis.get(key));
    }
    System.out.println();
  }

  /**
   * Write-Behind: Asynchronous database writes
   */
  public void demoWriteBehindPolicy() {
    System.out.println("4. Write-Behind Cache Policy (Redis):");
    System.out.println("   - Immediate cache write, deferred database write");
    System.out.println("   - Better write performance, risk of data loss");

    try (Jedis jedis = jedisPool.getResource()) {
      String key = "session:writebehind:789";
      String value = "User Session Data";

      long startTime = System.currentTimeMillis();

      // Write to cache immediately
      jedis.set(key, value);
      long cacheWriteTime = System.currentTimeMillis() - startTime;

      // Schedule asynchronous database write
      executor.schedule(() -> {
        simulateDatabaseWrite(key, value);
        System.out.println("   ⏰ Async database write completed for: " + key);
      }, 200, TimeUnit.MILLISECONDS);

      System.out.println("   Cache write time: " + cacheWriteTime + "ms (immediate)");
      System.out.println("   Cache value: " + jedis.get(key));
      System.out.println("   Database value (before async): " + database.get(key));

      // Wait for async write
      try {
        Thread.sleep(300);
        System.out.println("   Database value (after async): " + database.get(key));
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    }
    System.out.println();
  }

  /**
   * Write-Back: Batched writes with coalescing
   */
  public void demoWriteBackPolicy() {
    System.out.println("5. Write-Back Cache Policy (Redis):");
    System.out.println("   - Batches multiple writes, coalesces updates");
    System.out.println("   - Optimal for high-frequency updates");

    try (Jedis jedis = jedisPool.getResource()) {
      String key = "counter:writeback:001";

      System.out.println("   Performing rapid counter updates...");

      // Simulate rapid updates (only latest value matters)
      Pipeline pipeline = jedis.pipelined();
      for (int i = 1; i <= 10; i++) {
        pipeline.set(key, String.valueOf(i * 100));
        System.out.println("   Updated cache counter: " + (i * 100));
      }
      pipeline.sync();

      String finalValue = jedis.get(key);

      // Schedule batched database write (only final value)
      executor.schedule(() -> {
        System.out.println("   📦 Batched database write executing...");
        simulateDatabaseWrite(key, finalValue);
        System.out.println("   ✓ Database updated with final value: " + finalValue);
      }, 250, TimeUnit.MILLISECONDS);

      System.out.println("   Final cache value: " + finalValue);
      System.out.println("   Database value (before batch): " + database.get(key));

      try {
        Thread.sleep(350);
        System.out.println("   Database value (after batch): " + database.get(key));
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    }
    System.out.println();
  }

  /**
   * Demonstrates various cache invalidation strategies
   */
  public void demoCacheInvalidationStrategies() {
    System.out.println("6. Redis Cache Invalidation Strategies:");

    try (Jedis jedis = jedisPool.getResource()) {

      // TTL Expiration
      System.out.println("   a) TTL (Time-To-Live) Expiration:");
      jedis.setex("session:ttl:abc", 3, "session_data");
      System.out.println("   Set session with 3s TTL: " + jedis.get("session:ttl:abc"));
      System.out.println("   TTL remaining: " + jedis.ttl("session:ttl:abc") + "s");

      try {
        Thread.sleep(3500);
        System.out.println("   After expiry: " + jedis.get("session:ttl:abc"));
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }

      // Manual Purge/Invalidation
      System.out.println("   b) Manual Purge (DEL command):");
      jedis.set("temp:data1", "value1");
      jedis.set("temp:data2", "value2");
      System.out.println("   Before purge: " + jedis.exists("temp:data1", "temp:data2"));

      Long deleted = jedis.del("temp:data1", "temp:data2");
      System.out.println("   Purged " + deleted + " keys");
      System.out.println("   After purge: " + jedis.exists("temp:data1", "temp:data2"));

      // Pattern-based invalidation (Ban)
      System.out.println("   c) Pattern-based Invalidation (Ban):");
      jedis.set("user:cache:1", "user1");
      jedis.set("user:cache:2", "user2");
      jedis.set("product:cache:1", "product1");

      // Invalidate all user cache entries
      var userKeys = jedis.keys("user:cache:*");
      if (!userKeys.isEmpty()) {
        jedis.del(userKeys.toArray(new String[0]));
        System.out.println("   Banned (deleted) " + userKeys.size() + " user cache keys");
      }

      System.out.println("   Remaining keys: " + jedis.keys("*cache*"));

      // Refresh pattern (reload from source)
      System.out.println("   d) Cache Refresh:");
      String configKey = "config:app:settings";
      jedis.set(configKey, "old_config");

      // Simulate config refresh
      String newConfig = "refreshed_config_" + System.currentTimeMillis();
      jedis.set(configKey, newConfig);
      System.out.println("   Refreshed config: " + jedis.get(configKey));

      // Conditional refresh (only if exists)
      String conditionalKey = "cache:conditional";
      String updated = jedis.set(conditionalKey, "new_value", SetParams.setParams().xx());
      System.out.println("   Conditional refresh (key doesn't exist): " + updated);

      jedis.set(conditionalKey, "existing_value");
      updated = jedis.set(conditionalKey, "updated_value", SetParams.setParams().xx());
      System.out.println("   Conditional refresh (key exists): " + updated);
    }
    System.out.println();
  }

  /**
   * Demonstrates distributed cache features unique to Redis
   */
  public void demoDistributedCacheFeatures() {
    System.out.println("7. Distributed Cache Features (Redis-specific):");

    try (Jedis jedis = jedisPool.getResource()) {

      // Atomic operations for distributed coordination
      System.out.println("   a) Atomic Operations for Distributed Coordination:");
      String lockKey = "lock:resource:123";
      String lockValue = "server-" + System.currentTimeMillis();

      // Distributed lock (SET with NX and EX)
      String lockResult = jedis.set(lockKey, lockValue, SetParams.setParams().nx().ex(5));
      System.out.println("   Acquired distributed lock: " + (lockResult != null));

      if (lockResult != null) {
        System.out.println("   Processing critical section...");
        try {
          Thread.sleep(100); // Simulate work
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
        }

        // Release lock safely (only if we own it)
        String script = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
        Long released = (Long) jedis.eval(script, 1, lockKey, lockValue);
        System.out.println("   Released lock: " + (released == 1));
      }

      // Pub/Sub for cache invalidation across instances
      System.out.println("   b) Pub/Sub for Distributed Cache Invalidation:");

      // Simulate cache invalidation message
      executor.submit(() -> {
        try (Jedis publisher = jedisPool.getResource()) {
          Thread.sleep(100);
          publisher.publish("cache:invalidate", "user:cache:*");
          System.out.println("   📢 Published cache invalidation message");
        } catch (Exception e) {
          System.err.println("   Error publishing: " + e.getMessage());
        }
      });

      // Subscriber would listen for invalidation messages
      System.out.println("   📱 Cache instances would subscribe to 'cache:invalidate' channel");

      // Sorted sets for distributed leaderboards/rankings
      System.out.println("   c) Distributed Data Structures (Sorted Sets):");
      String leaderboard = "game:leaderboard:global";

      jedis.zadd(leaderboard, 1500, "player:alice");
      jedis.zadd(leaderboard, 2000, "player:bob");
      jedis.zadd(leaderboard, 1200, "player:charlie");
      jedis.zadd(leaderboard, 1800, "player:diana");

      var topPlayers = jedis.zrevrangeWithScores(leaderboard, 0, 2);
      System.out.println("   Global top 3 players: " + topPlayers);

      // Get player rank
      Long aliceRank = jedis.zrevrank(leaderboard, "player:alice");
      System.out.println("   Alice's rank: " + (aliceRank != null ? aliceRank + 1 : "not ranked"));

      // HyperLogLog for distributed counting
      System.out.println("   d) HyperLogLog for Distributed Unique Counting:");
      String counterKey = "unique:visitors:today";

      // Simulate unique visitor tracking
      jedis.pfadd(counterKey, "user:100", "user:200", "user:300", "user:100"); // duplicate ignored
      Long uniqueCount = jedis.pfcount(counterKey);
      System.out.println("   Estimated unique visitors: " + uniqueCount);

    } catch (Exception e) {
      System.err.println("   Error in distributed features demo: " + e.getMessage());
    }
    System.out.println();
  }

  /**
   * Simulates database write with latency
   */
  private void simulateDatabaseWrite(String key, String value) {
    try {
      Thread.sleep(30); // Simulate database write latency
      database.put(key, value);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }

  /**
   * Cleanup resources
   */
  public void cleanup() {
    System.out.println("8. Cleanup:");

    try (Jedis jedis = jedisPool.getResource()) {
      jedis.flushDB();
      System.out.println("   Redis database cleared");
    }

    if (jedisPool != null) {
      jedisPool.close();
      System.out.println("   Connection pool closed");
    }

    if (executor != null) {
      executor.shutdown();
      System.out.println("   Executor service shutdown");
    }

    System.out.println("   ✓ Cleanup completed");
  }
}
