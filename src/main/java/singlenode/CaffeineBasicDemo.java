package singlenode;

import com.github.benmanes.caffeine.cache.*;
import java.time.Duration;

public class CaffeineBasicDemo {
  public static void main(String[] args) {
    CaffeineBasicDemo demo = new CaffeineBasicDemo();

    System.out.println("=== Caffeine Cache Tutorial ===\n");

    demo.basicUsage();
    demo.cacheReplacementPolicies();
    demo.loadingCacheDemo();
    demo.asyncCacheDemo();
  }

  /**
   * Demonstrates basic cache operations
   */
  public void basicUsage() {
    System.out.println("1. Basic Cache Usage:");

    Cache<String, String> cache = Caffeine.newBuilder()
        .maximumSize(100)
        .expireAfterWrite(Duration.ofMinutes(5))
        .build();

    // Put and get operations
    cache.put("user:123", "John Doe");
    cache.put("user:456", "Jane Smith");

    String user = cache.getIfPresent("user:123");
    System.out.println("Retrieved user: " + user);

    // Compute if absent
    String computed = cache.get("user:789", k -> "New User - " + k.split(":")[1]);
    System.out.println("Computed user: " + computed);

    // Bulk operations
    cache.putAll(java.util.Map.of(
        "product:1", "Laptop",
        "product:2", "Phone"
    ));

    System.out.println("Cache size: " + cache.estimatedSize());
    System.out.println();
  }

  /**
   * Demonstrates cache replacement policies
   * Caffeine uses W-TinyLFU (Window Tiny Least Frequently Used) which is more efficient than LRU
   */
  public void cacheReplacementPolicies() {
    System.out.println("2. Cache Replacement Policies (W-TinyLFU - evolved from LRU):");

    // Small cache to trigger eviction
    Cache<String, String> cache = Caffeine.newBuilder()
        .maximumSize(3) // Small size to demonstrate eviction
        .recordStats() // Enable statistics
        .build();

    // Fill cache beyond capacity
    System.out.println("Adding items to cache (capacity: 3)");
    cache.put("item1", "value1");
    cache.put("item2", "value2");
    cache.put("item3", "value3");
    System.out.println("Cache size: " + cache.estimatedSize());

    // Access item1 multiple times to increase its frequency
    for (int i = 0; i < 5; i++) {
        cache.getIfPresent("item1");
    }

    // Add more items to trigger eviction
    cache.put("item4", "value4");
    cache.put("item5", "value5");

    System.out.println("After adding more items:");
    System.out.println("item1 (frequently accessed): " + cache.getIfPresent("item1"));
    System.out.println("item2 (less frequent): " + cache.getIfPresent("item2"));
    System.out.println("item3 (less frequent): " + cache.getIfPresent("item3"));
    System.out.println("item4 (recent): " + cache.getIfPresent("item4"));
    System.out.println("item5 (recent): " + cache.getIfPresent("item5"));

    // Print cache statistics
    var stats = cache.stats();
    System.out.println("Cache stats - Hit rate: " + String.format("%.2f%%", stats.hitRate() * 100));
    System.out.println();
  }

  /**
   * Demonstrates LoadingCache for automatic value computation
   */
  public void loadingCacheDemo() {
    System.out.println("3. LoadingCache - Automatic Value Loading:");

    LoadingCache<String, String> cache = Caffeine.newBuilder()
        .maximumSize(100)
        .expireAfterWrite(Duration.ofMinutes(10))
        .build(key -> {
          // Simulate expensive computation/database lookup
          System.out.println("Loading value for key: " + key);
          Thread.sleep(100); // Simulate delay
          return "loaded-" + key + "-" + System.currentTimeMillis();
        });

    try {
      // Values are loaded automatically
      System.out.println("First access: " + cache.get("expensive-key"));
      System.out.println("Second access (cached): " + cache.get("expensive-key"));

      // Bulk loading
      var keys = java.util.List.of("key1", "key2", "key3");
      var bulkResult = cache.getAll(keys);
      System.out.println("Bulk loaded: " + bulkResult);
    } catch (Exception e) {
      System.err.println("Error in loading cache demo: " + e.getMessage());
    }

    System.out.println();
  }

  /**
   * Demonstrates AsyncCache for non-blocking operations
   */
  public void asyncCacheDemo() {
    System.out.println("4. AsyncCache - Non-blocking Operations:");

    AsyncCache<String, String> cache = Caffeine.newBuilder()
        .maximumSize(100)
        .buildAsync();

    // Async put and get
    cache.put("async-key", java.util.concurrent.CompletableFuture.completedFuture("async-value"));

    cache.getIfPresent("async-key")
        .thenAccept(value -> System.out.println("Async retrieved: " + value))
        .join(); // Wait for completion in demo

    // Async computation
    cache.get("computed-async", (key, executor) ->
        java.util.concurrent.CompletableFuture.supplyAsync(() -> {
          try {
            Thread.sleep(50); // Simulate async work
            return "async-computed-" + key;
          } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return "error";
          }
        }, executor))
        .thenAccept(value -> System.out.println("Async computed: " + value))
        .join();

    System.out.println();
  }
}
