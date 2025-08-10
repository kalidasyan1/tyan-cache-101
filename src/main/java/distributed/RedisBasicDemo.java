package distributed;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.Pipeline;
import redis.clients.jedis.Transaction;
import redis.clients.jedis.params.SetParams;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Comprehensive Redis tutorial demonstrating basic usage and major features
 */
public class RedisBasicDemo {
  private JedisPool jedisPool;

  public static void main(String[] args) {
    RedisBasicDemo demo = new RedisBasicDemo();

    System.out.println("=== Redis Distributed Cache Tutorial ===\n");

    demo.initializeRedis();
    demo.basicOperations();
    demo.dataTypesDemo();
    demo.expirationAndTTL();
    demo.pipelineOperations();
    demo.transactionDemo();
    demo.pubSubDemo();
    demo.connectionPooling();
    demo.cleanup();
  }

  /**
   * Initialize Redis connection pool
   */
  public void initializeRedis() {
    System.out.println("1. Redis Connection Setup:");
    try {
      JedisPoolConfig poolConfig = new JedisPoolConfig();
      poolConfig.setMaxTotal(10);
      poolConfig.setMaxIdle(5);
      poolConfig.setMinIdle(1);
      poolConfig.setTestOnBorrow(true);

      jedisPool = new JedisPool(poolConfig, "localhost", 6379);

      // Test connection
      try (Jedis jedis = jedisPool.getResource()) {
        jedis.ping();
        System.out.println("   ✓ Connected to Redis server");
        System.out.println("   Redis version: " + jedis.info("server").split("\r\n")[1]);
      }
    } catch (Exception e) {
      System.err.println("   ✗ Failed to connect to Redis: " + e.getMessage());
      System.err.println("   Make sure Redis server is running on localhost:6379");
      return;
    }
    System.out.println();
  }

  /**
   * Basic Redis operations - strings, get, set, delete
   */
  public void basicOperations() {
    System.out.println("2. Basic Redis Operations:");

    try (Jedis jedis = jedisPool.getResource()) {
      // Basic string operations
      jedis.set("user:1000", "John Doe");
      jedis.set("user:1001", "Jane Smith");

      String user1 = jedis.get("user:1000");
      String user2 = jedis.get("user:1001");

      System.out.println("   Retrieved user:1000 = " + user1);
      System.out.println("   Retrieved user:1001 = " + user2);

      // Conditional operations
      String result1 = jedis.set("user:1000", "John Updated", SetParams.setParams().nx());
      String result2 = jedis.set("user:1002", "Bob Wilson", SetParams.setParams().nx());

      System.out.println("   Set if not exists (existing key): " + result1);
      System.out.println("   Set if not exists (new key): " + result2);

      // Increment operations
      jedis.set("counter", "0");
      Long count1 = jedis.incr("counter");
      Long count2 = jedis.incrBy("counter", 5);

      System.out.println("   Counter after incr: " + count1);
      System.out.println("   Counter after incrBy 5: " + count2);

      // Multiple operations
      jedis.mset("key1", "value1", "key2", "value2", "key3", "value3");
      List<String> values = jedis.mget("key1", "key2", "key3");
      System.out.println("   Multi-get result: " + values);

      // Check if key exists
      boolean exists = jedis.exists("user:1000");
      System.out.println("   Key user:1000 exists: " + exists);

      // Delete operations
      Long deleted = jedis.del("user:1001", "key1");
      System.out.println("   Deleted keys count: " + deleted);
    }
    System.out.println();
  }

  /**
   * Demonstrates Redis data types: Lists, Sets, Hashes, Sorted Sets
   */
  public void dataTypesDemo() {
    System.out.println("3. Redis Data Types:");

    try (Jedis jedis = jedisPool.getResource()) {
      // Lists (ordered collection)
      System.out.println("   a) Lists:");
      jedis.del("tasks");
      jedis.rpush("tasks", "task1", "task2", "task3");
      jedis.lpush("tasks", "urgent_task");

      List<String> tasks = jedis.lrange("tasks", 0, -1);
      System.out.println("      All tasks: " + tasks);

      String nextTask = jedis.lpop("tasks");
      System.out.println("      Next task: " + nextTask);

      // Sets (unique elements)
      System.out.println("   b) Sets:");
      jedis.del("tags");
      jedis.sadd("tags", "java", "redis", "cache", "java"); // duplicate ignored

      Set<String> allTags = jedis.smembers("tags");
      System.out.println("      All tags: " + allTags);

      boolean isMember = jedis.sismember("tags", "redis");
      System.out.println("      Is 'redis' a tag: " + isMember);

      // Hashes (field-value pairs)
      System.out.println("   c) Hashes:");
      jedis.del("user:profile:123");
      jedis.hset("user:profile:123", "name", "Alice");
      jedis.hset("user:profile:123", "email", "alice@example.com");
      jedis.hset("user:profile:123", "age", "30");

      Map<String, String> profile = jedis.hgetAll("user:profile:123");
      System.out.println("      User profile: " + profile);

      String email = jedis.hget("user:profile:123", "email");
      System.out.println("      User email: " + email);

      // Sorted Sets (scored elements)
      System.out.println("   d) Sorted Sets:");
      jedis.del("leaderboard");
      jedis.zadd("leaderboard", 100, "player1");
      jedis.zadd("leaderboard", 150, "player2");
      jedis.zadd("leaderboard", 75, "player3");
      jedis.zadd("leaderboard", 200, "player4");

      List<String> topPlayers = jedis.zrevrange("leaderboard", 0, 2);
      System.out.println("      Top 3 players: " + topPlayers);

      Double score = jedis.zscore("leaderboard", "player2");
      System.out.println("      Player2 score: " + score);
    }
    System.out.println();
  }

  /**
   * Demonstrates expiration and TTL features
   */
  public void expirationAndTTL() {
    System.out.println("4. Expiration and TTL:");

    try (Jedis jedis = jedisPool.getResource()) {
      // Set with expiration
      jedis.setex("session:abc123", 5, "user_session_data");
      System.out.println("   Set session with 5s expiration");

      // Check TTL
      Long ttl = jedis.ttl("session:abc123");
      System.out.println("   Session TTL: " + ttl + " seconds");

      // Set expiration on existing key
      jedis.set("temp_data", "temporary");
      jedis.expire("temp_data", 3);
      System.out.println("   Set expiration on existing key: 3s");

      // Check if key exists
      boolean exists1 = jedis.exists("temp_data");
      System.out.println("   Key exists immediately: " + exists1);

      // Wait and check again
      try {
        Thread.sleep(3500);
        boolean exists2 = jedis.exists("temp_data");
        System.out.println("   Key exists after 3.5s: " + exists2);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }

      // Persist (remove expiration)
      jedis.setex("persistent_key", 10, "data");
      jedis.persist("persistent_key");
      Long ttlAfterPersist = jedis.ttl("persistent_key");
      System.out.println("   TTL after persist: " + ttlAfterPersist + " (-1 means no expiration)");
    }
    System.out.println();
  }

  /**
   * Demonstrates pipeline operations for batch processing
   */
  public void pipelineOperations() {
    System.out.println("5. Pipeline Operations (Batch Processing):");

    try (Jedis jedis = jedisPool.getResource()) {
      long startTime = System.currentTimeMillis();

      // Without pipeline (individual commands)
      for (int i = 0; i < 100; i++) {
        jedis.set("individual:" + i, "value" + i);
      }
      long individualTime = System.currentTimeMillis() - startTime;

      // With pipeline (batched commands)
      startTime = System.currentTimeMillis();
      Pipeline pipeline = jedis.pipelined();

      for (int i = 0; i < 100; i++) {
        pipeline.set("pipelined:" + i, "value" + i);
      }

      List<Object> results = pipeline.syncAndReturnAll();
      long pipelineTime = System.currentTimeMillis() - startTime;

      System.out.println("   Individual commands time: " + individualTime + "ms");
      System.out.println("   Pipeline commands time: " + pipelineTime + "ms");
      System.out.println("   Pipeline speedup: " + (individualTime / (double) pipelineTime) + "x");
      System.out.println("   Pipeline results count: " + results.size());

      // Cleanup
      pipeline = jedis.pipelined();
      for (int i = 0; i < 100; i++) {
        pipeline.del("individual:" + i, "pipelined:" + i);
      }
      pipeline.sync();
    }
    System.out.println();
  }

  /**
   * Demonstrates Redis transactions with MULTI/EXEC
   */
  public void transactionDemo() {
    System.out.println("6. Redis Transactions (MULTI/EXEC):");

    try (Jedis jedis = jedisPool.getResource()) {
      // Setup initial state
      jedis.set("account:A", "100");
      jedis.set("account:B", "50");

      System.out.println("   Initial: Account A = " + jedis.get("account:A") +
                        ", Account B = " + jedis.get("account:B"));

      // Transaction: Transfer 25 from A to B
      Transaction transaction = jedis.multi();
      transaction.decrBy("account:A", 25);
      transaction.incrBy("account:B", 25);
      transaction.set("transfer:log", "A->B: 25 at " + System.currentTimeMillis());

      List<Object> results = transaction.exec();

      System.out.println("   Transaction executed: " + (results != null ? "SUCCESS" : "FAILED"));
      System.out.println("   Final: Account A = " + jedis.get("account:A") +
                        ", Account B = " + jedis.get("account:B"));
      System.out.println("   Transfer log: " + jedis.get("transfer:log"));

      // Demonstrate transaction rollback with DISCARD
      transaction = jedis.multi();
      transaction.set("temp:key", "temp:value");
      transaction.discard(); // Cancel transaction

      boolean tempExists = jedis.exists("temp:key");
      System.out.println("   Temp key after DISCARD: " + tempExists);
    }
    System.out.println();
  }

  /**
   * Demonstrates Redis Pub/Sub messaging
   */
  public void pubSubDemo() {
    System.out.println("7. Redis Pub/Sub Messaging:");

    try {
      // Create subscriber in separate thread
      Thread subscriberThread = new Thread(() -> {
        try (Jedis subscriber = jedisPool.getResource()) {
          subscriber.subscribe(new redis.clients.jedis.JedisPubSub() {
            @Override
            public void onMessage(String channel, String message) {
              System.out.println("   Received message on " + channel + ": " + message);
            }

            @Override
            public void onSubscribe(String channel, int subscribedChannels) {
              System.out.println("   Subscribed to channel: " + channel);
            }
          }, "notifications");
        }
      });

      subscriberThread.start();
      Thread.sleep(100); // Let subscriber start

      // Publish messages
      try (Jedis publisher = jedisPool.getResource()) {
        Long subscribers1 = publisher.publish("notifications", "User logged in");
        Long subscribers2 = publisher.publish("notifications", "New order received");
        Long subscribers3 = publisher.publish("notifications", "System maintenance scheduled");

        System.out.println("   Published 3 messages to " + subscribers1 + " subscribers");
      }

      Thread.sleep(1000); // Wait for messages to be processed
      subscriberThread.interrupt();

    } catch (Exception e) {
      System.err.println("   Error in pub/sub demo: " + e.getMessage());
    }
    System.out.println();
  }

  /**
   * Demonstrates connection pooling best practices
   */
  public void connectionPooling() {
    System.out.println("8. Connection Pooling:");

    System.out.println("   Pool Configuration:");
    System.out.println("   - Max Total: 10");
    System.out.println("   - Max Idle: 5");
    System.out.println("   - Min Idle: 1");

    // Simulate concurrent usage
    System.out.println("   Simulating concurrent connections...");

    Thread[] threads = new Thread[5];
    for (int i = 0; i < 5; i++) {
      final int threadId = i;
      threads[i] = new Thread(() -> {
        try (Jedis jedis = jedisPool.getResource()) {
          jedis.set("thread:" + threadId, "data from thread " + threadId);
          String value = jedis.get("thread:" + threadId);
          System.out.println("   Thread " + threadId + " set and got: " + value);
        }
      });
    }

    for (Thread thread : threads) {
      thread.start();
    }

    for (Thread thread : threads) {
      try {
        thread.join();
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    }

    System.out.println("   All threads completed successfully");
    System.out.println();
  }

  /**
   * Cleanup resources
   */
  public void cleanup() {
    System.out.println("9. Cleanup:");

    try (Jedis jedis = jedisPool.getResource()) {
      // Clean up test keys
      jedis.del("user:1000", "user:1002", "counter", "key2", "key3");
      jedis.del("tasks", "tags", "user:profile:123", "leaderboard");
      jedis.del("session:abc123", "persistent_key");
      jedis.del("account:A", "account:B", "transfer:log");

      // Clean up thread keys
      for (int i = 0; i < 5; i++) {
        jedis.del("thread:" + i);
      }

      System.out.println("   Test keys cleaned up");
    }

    if (jedisPool != null) {
      jedisPool.close();
      System.out.println("   Connection pool closed");
    }

    System.out.println("   Cleanup completed");
  }
}
