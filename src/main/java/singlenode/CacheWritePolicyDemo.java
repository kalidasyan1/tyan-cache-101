package singlenode;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.RemovalListener;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Demonstrates cache write policies with clear distinctions:
 * - Write-Through: Immediate write to both cache and database
 * - Write-Around: Direct database write, bypassing cache
 * - Write-Behind: Immediate cache write, scheduled database write at intervals
 * - Write-Back: Cache-only write, database write only on eviction
 */
public class CacheWritePolicyDemo {
  private Map<String, String> database = new HashMap<>();
  private Map<String, String> pendingWrites = new ConcurrentHashMap<>();
  private ScheduledExecutorService executor = Executors.newScheduledThreadPool(2);

  public static void main(String[] args) throws InterruptedException {
    CacheWritePolicyDemo demo = new CacheWritePolicyDemo();

    System.out.println("=== Cache Write Policies Demo ===\n");

    demo.demoWriteThrough();
    demo.demoWriteAround();
    demo.demoWriteBehind();
    demo.demoWriteBack();

    demo.executor.shutdown();
  }

  /**
   * Write-Through: Data is written to cache and database simultaneously
   * Pros: Data consistency, immediate persistence
   * Cons: Higher latency on writes
   */
  public void demoWriteThrough() {
    System.out.println("1. Write-Through Cache Policy:");
    System.out.println("   - Writes go to both cache and database simultaneously");
    System.out.println("   - Ensures data consistency but higher write latency");

    Cache<String, String> cache = Caffeine.newBuilder().maximumSize(100).build();
    database.clear();

    // Simulate write-through operation
    String key = "user:123";
    String value = "John Doe";

    System.out.println("Writing '" + value + "' to cache and database...");
    long startTime = System.currentTimeMillis();

    // Write to both cache and database (simulating write-through)
    cache.put(key, value);
    simulateDbWrite(key, value); // Simulate database write delay

    long writeTime = System.currentTimeMillis() - startTime;

    System.out.println("Cached value: " + cache.getIfPresent(key));
    System.out.println("Database value: " + database.get(key));
    System.out.println("Write time: " + writeTime + "ms");
    System.out.println();
  }

  /**
   * Write-Around: Data is written only to database, bypassing cache
   * Pros: Reduces cache pollution from infrequently accessed data
   * Cons: Cache miss on subsequent reads
   */
  public void demoWriteAround() {
    System.out.println("2. Write-Around Cache Policy:");
    System.out.println("   - Writes go directly to database, bypassing cache");
    System.out.println("   - Prevents cache pollution but causes cache miss on read");

    Cache<String, String> cache = Caffeine.newBuilder().maximumSize(100).build();
    database.clear();

    String key = "temp:456";
    String value = "Temporary Data";

    System.out.println("Writing '" + value + "' directly to database (bypassing cache)...");

    // Write only to database (write-around)
    simulateDbWrite(key, value);
    // Cache is NOT updated

    System.out.println("Cached value: " + cache.getIfPresent(key)); // null
    System.out.println("Database value: " + database.get(key));

    // On read, need to load from database
    System.out.println("Reading from cache (cache miss, loading from DB)...");
    String cachedValue = cache.get(key, k -> database.get(k));
    System.out.println("Now cached value: " + cachedValue);
    System.out.println();
  }

  /**
   * Write-Behind: Data is written to cache immediately, database write at scheduled intervals
   * Key difference: Writes happen at REGULAR INTERVALS regardless of eviction
   * Pros: Lower write latency, can batch multiple updates
   * Cons: Risk of data loss if system fails before scheduled write
   */
  public void demoWriteBehind() {
    System.out.println("3. Write-Behind Cache Policy:");
    System.out.println("   - Writes go to cache immediately");
    System.out.println("   - Database writes happen at SCHEDULED INTERVALS");
    System.out.println("   - Multiple updates to same key get batched");

    Cache<String, String> cache = Caffeine.newBuilder().maximumSize(100).build();
    database.clear();
    pendingWrites.clear();

    // Schedule periodic database writes every 2 seconds
    executor.scheduleAtFixedRate(() -> {
      if (!pendingWrites.isEmpty()) {
        System.out.println("   [Scheduled Write] Writing " + pendingWrites.size() + " pending entries to database...");
        for (Map.Entry<String, String> entry : pendingWrites.entrySet()) {
          simulateDbWrite(entry.getKey(), entry.getValue());
          System.out.println("   [DB] " + entry.getKey() + " = " + entry.getValue());
        }
        pendingWrites.clear();
      }
    }, 2, 2, TimeUnit.SECONDS);

    // Perform multiple rapid writes
    String[] keys = {"order:1", "order:2", "order:1", "order:3", "order:1"}; // Note: order:1 updated multiple times
    String[] values = {"Order A", "Order B", "Order A Updated", "Order C", "Order A Final"};

    System.out.println("Performing rapid writes to cache...");
    for (int i = 0; i < keys.length; i++) {
      long startTime = System.currentTimeMillis();

      // Write to cache immediately
      cache.put(keys[i], values[i]);
      pendingWrites.put(keys[i], values[i]); // Track for later DB write (coalesced)

      long writeTime = System.currentTimeMillis() - startTime;
      System.out.println("Cache write " + (i + 1) + " (" + keys[i] + "): " + writeTime + "ms");
    }

    System.out.println("Cache state: " + cache.estimatedSize() + " items");
    System.out.println("Database state (before scheduled write): " + database.size() + " items");

    // Wait for scheduled write to occur
    try {
      Thread.sleep(3000);
      System.out.println("Database state (after scheduled write): " + database.size() + " items");
      System.out.println("Final database contents:");
      database.forEach((k, v) -> System.out.println("   " + k + " = " + v));
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
    System.out.println();
  }

  /**
   * Write-Back: Data is written to cache only, database write ONLY ON EVICTION
   * Key difference: Database writes happen only when cache needs to FREE SPACE
   * Pros: Minimal write latency, natural write coalescing
   * Cons: Higher risk of data loss, depends on eviction policy
   */
  public void demoWriteBack() {
    System.out.println("4. Write-Back Cache Policy:");
    System.out.println("   - Writes go to cache only");
    System.out.println("   - Database writes ONLY when cache entry is EVICTED");
    System.out.println("   - Most efficient but highest data loss risk");

    database.clear();

    // Create cache with small size to trigger evictions and write-back behavior
    Cache<String, String> cache = Caffeine.newBuilder()
        .maximumSize(3) // Small size to force evictions
        .removalListener((RemovalListener<String, String>) (key, value, cause) -> {
          if (cause.wasEvicted() && value != null) {
            System.out.println("   [EVICTION] Writing to database due to eviction: " + key + " = " + value);
            simulateDbWrite(key, value);
          }
        })
        .build();

    System.out.println("Performing writes to small cache (max size = 3)...");

    // Fill cache beyond capacity to trigger evictions
    String[] data = {"item1", "item2", "item3", "item4", "item5"};

    for (int i = 0; i < data.length; i++) {
      String key = "cache:" + (i + 1);
      String value = data[i] + "_v1";

      long startTime = System.currentTimeMillis();
      cache.put(key, value);
      long writeTime = System.currentTimeMillis() - startTime;

      System.out.println("Write " + (i + 1) + " (" + key + "): " + writeTime + "ms");
      System.out.println("   Cache size: " + cache.estimatedSize() + ", DB size: " + database.size());

      // Force cache maintenance to trigger eviction processing
      cache.cleanUp();

      try {
        Thread.sleep(100);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    }

    // Show final state
    System.out.println("\nFinal state:");
    System.out.println("Cache entries (still in memory):");
    cache.asMap().forEach((k, v) -> System.out.println("   " + k + " = " + v));

    System.out.println("Database entries (written on eviction):");
    database.forEach((k, v) -> System.out.println("   " + k + " = " + v));

    // Demonstrate that data in cache is NOT in database until eviction
    System.out.println("\nKey insight: Items still in cache are NOT in database!");
    System.out.println("They will only be written to database when evicted.");
    System.out.println();
  }

  private void simulateDbWrite(String key, String value) {
    try {
      Thread.sleep(50); // Simulate database write latency
      database.put(key, value);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }
}
