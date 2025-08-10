package distributed;

import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.RedisFuture;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.async.RedisAsyncCommands;
import io.lettuce.core.api.reactive.RedisReactiveCommands;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.support.ConnectionPoolSupport;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

/**
 * Modern Redis implementation using Lettuce (non-blocking, async-capable client)
 * Demonstrates the difference from legacy JedisPool approach
 */
public class RedisModernDemo {
  private RedisClient redisClient;
  private GenericObjectPool<StatefulRedisConnection<String, String>> connectionPool;

  public static void main(String[] args) {
    RedisModernDemo demo = new RedisModernDemo();

    System.out.println("=== Modern Redis with Lettuce Tutorial ===\n");

    demo.initializeRedis();
    demo.synchronousOperations();
    demo.asynchronousOperations();
    demo.reactiveOperations();
    demo.connectionPoolingDemo();
    demo.cleanup();
  }

  /**
   * Initialize modern Redis client with Lettuce
   */
  public void initializeRedis() {
    System.out.println("1. Modern Redis Setup (Lettuce):");

    try {
      // Create Redis URI with connection settings
      RedisURI redisUri = RedisURI.builder()
          .withHost("localhost")
          .withPort(6379)
          .withTimeout(Duration.ofSeconds(5))
          .build();

      // Create client (thread-safe, reusable)
      redisClient = RedisClient.create(redisUri);

      // Test connection
      try (StatefulRedisConnection<String, String> connection = redisClient.connect()) {
        RedisCommands<String, String> commands = connection.sync();
        String pong = commands.ping();
        System.out.println("   ✓ Connected to Redis: " + pong);
        System.out.println("   ✓ Using Lettuce (modern, non-blocking client)");
      }

      // Setup connection pool (optional, for high-concurrency scenarios)
      GenericObjectPoolConfig<StatefulRedisConnection<String, String>> poolConfig = new GenericObjectPoolConfig<>();
      poolConfig.setMaxTotal(10);
      poolConfig.setMaxIdle(5);
      poolConfig.setMinIdle(1);

      connectionPool = ConnectionPoolSupport.createGenericObjectPool(
          () -> redisClient.connect(), poolConfig);

      System.out.println("   ✓ Connection pool initialized");

    } catch (Exception e) {
      System.err.println("   ✗ Failed to connect: " + e.getMessage());
      return;
    }
    System.out.println();
  }

  /**
   * Traditional synchronous operations (like Jedis)
   */
  public void synchronousOperations() {
    System.out.println("2. Synchronous Operations (Traditional Style):");

    try (StatefulRedisConnection<String, String> connection = redisClient.connect()) {
      RedisCommands<String, String> commands = connection.sync();

      // Basic operations
      commands.set("user:sync:1000", "John Doe");
      String user = commands.get("user:sync:1000");
      System.out.println("   Sync get: " + user);

      // Batch operations
      Map<String, String> keyValues = new HashMap<>();
      keyValues.put("key1", "value1");
      keyValues.put("key2", "value2");
      commands.mset(keyValues);
      System.out.println("   Sync mget: " + commands.mget("key1", "key2"));

    } catch (Exception e) {
      System.err.println("   Error in sync operations: " + e.getMessage());
    }
    System.out.println();
  }

  /**
   * Modern asynchronous operations (non-blocking)
   */
  public void asynchronousOperations() {
    System.out.println("3. Asynchronous Operations (Non-blocking):");

    try (StatefulRedisConnection<String, String> connection = redisClient.connect()) {
      RedisAsyncCommands<String, String> commands = connection.async();

      // Async operations return RedisFuture (which extends CompletableFuture)
      RedisFuture<String> setFuture = commands.set("user:async:1000", "Jane Smith");

      // Chain async operations - convert to CompletableFuture properly
      CompletableFuture<String> getFuture = setFuture.thenCompose(result ->
          commands.get("user:async:1000")).toCompletableFuture();

      // Non-blocking - can do other work here
      System.out.println("   Async operations initiated...");

      // Wait for result when needed
      String result = getFuture.get();
      System.out.println("   Async result: " + result);

      // Pipeline multiple async operations
      RedisFuture<String> op1 = commands.set("async:1", "value1");
      RedisFuture<String> op2 = commands.set("async:2", "value2");
      RedisFuture<Long> op3 = commands.incr("async:counter");

      // Wait for all operations to complete - convert RedisFuture to CompletableFuture
      CompletableFuture<Void> allOps = CompletableFuture.allOf(
          op1.toCompletableFuture(),
          op2.toCompletableFuture(),
          op3.toCompletableFuture());
      allOps.get(); // Wait for completion

      System.out.println("   Async pipeline completed");

    } catch (InterruptedException | ExecutionException e) {
      System.err.println("   Error in async operations: " + e.getMessage());
    }
    System.out.println();
  }

  /**
   * Reactive operations using Project Reactor
   */
  public void reactiveOperations() {
    System.out.println("4. Reactive Operations (Reactive Streams):");

    try (StatefulRedisConnection<String, String> connection = redisClient.connect()) {
      RedisReactiveCommands<String, String> commands = connection.reactive();

      // Reactive chain - store result to avoid unused variable warning
      commands.set("user:reactive:1000", "Bob Wilson")
          .then(commands.get("user:reactive:1000"))
          .doOnNext(value -> System.out.println("   Reactive result: " + value))
          .block(); // Block for demo purposes

      // Reactive stream processing
      commands.mget("key1", "key2", "async:1", "async:2")
          .doOnNext(values -> System.out.println("   Reactive mget: " + values))
          .subscribe();

      // Give reactive operations time to complete
      Thread.sleep(100);

    } catch (Exception e) {
      System.err.println("   Error in reactive operations: " + e.getMessage());
    }
    System.out.println();
  }

  /**
   * Connection pooling with modern approach
   */
  public void connectionPoolingDemo() {
    System.out.println("5. Modern Connection Pooling:");

    try {
      // Get connection from pool
      StatefulRedisConnection<String, String> connection = connectionPool.borrowObject();

      try {
        RedisCommands<String, String> commands = connection.sync();
        commands.set("pool:test", "pooled connection");
        String result = commands.get("pool:test");
        System.out.println("   Pool result: " + result);
        System.out.println("   ✓ Connection borrowed and used successfully");

      } finally {
        // Return connection to pool
        connectionPool.returnObject(connection);
        System.out.println("   ✓ Connection returned to pool");
      }

    } catch (Exception e) {
      System.err.println("   Error with connection pool: " + e.getMessage());
    }
    System.out.println();
  }

  /**
   * Cleanup resources
   */
  public void cleanup() {
    System.out.println("6. Cleanup:");
    try {
      if (connectionPool != null) {
        connectionPool.close();
        System.out.println("   ✓ Connection pool closed");
      }

      if (redisClient != null) {
        redisClient.shutdown();
        System.out.println("   ✓ Redis client shutdown");
      }
    } catch (Exception e) {
      System.err.println("   Error during cleanup: " + e.getMessage());
    }
  }
}
