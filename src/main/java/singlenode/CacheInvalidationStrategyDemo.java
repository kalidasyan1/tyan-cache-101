package singlenode;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

/**
 * Comprehensive demonstration of cache invalidation strategies and methods
 *
 * Covers all major invalidation approaches:
 * - TTL expiration
 * - Manual purge (single and bulk)
 * - Refresh patterns
 * - Stale-while-revalidate
 * - Ban patterns
 */
public class CacheInvalidationStrategyDemo {
  private ScheduledExecutorService executor = Executors.newScheduledThreadPool(2);

  public static void main(String[] args) {
    CacheInvalidationStrategyDemo demo = new CacheInvalidationStrategyDemo();

    System.out.println("=== Cache Invalidation Strategies Demo ===\n");

    demo.demoTTLExpiration();
    demo.demoPurge();
    demo.demoRefresh();
    demo.demoStaleWhileRevalidate();
    demo.demoBanPattern();

    demo.executor.shutdown();
  }

  /**
   * TTL (Time-To-Live) Expiration - Automatic expiration based on time
   * Most common invalidation strategy for ensuring data freshness
   */
  public void demoTTLExpiration() {
    System.out.println("1. TTL (Time-To-Live) Expiration:");
    System.out.println("   - Automatic expiration based on write/access time");
    System.out.println("   - Ensures data freshness without manual intervention");

    // Expire after write
    Cache<String, String> writeCache = Caffeine.newBuilder()
        .expireAfterWrite(Duration.ofSeconds(3))
        .recordStats()
        .build();

    // Expire after access
    Cache<String, String> accessCache = Caffeine.newBuilder()
        .expireAfterAccess(Duration.ofSeconds(3))
        .recordStats()
        .build();

    // Demo expire after write
    System.out.println("\n   a) Expire After Write (3 seconds):");
    System.out.println("      Entry expires 3 seconds after being written, regardless of access");

    writeCache.put("session:user1", "active");
    writeCache.put("session:user2", "active");
    System.out.println("      Time 0s: Added session:user1");

    try {
      Thread.sleep(1500);
      System.out.println("      Time 1.5s: Accessing session:user1 = " + writeCache.getIfPresent("session:user1"));

      Thread.sleep(1000);
      System.out.println("      Time 2.5s: Accessing session:user1 = " + writeCache.getIfPresent("session:user1"));

      Thread.sleep(1000);
      System.out.println("      Time 3.5s: Accessing session:user1 = " + writeCache.getIfPresent("session:user1"));
      System.out.println("      ↳ Expired! Even though we kept accessing it, it expired 3s after write");

    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }

    // Demo expire after access
    System.out.println("\n   b) Expire After Access (3 seconds):");
    System.out.println("      Entry expires 3 seconds after LAST access (read/write)");

    accessCache.put("data:temp", "temporary");
    System.out.println("      Time 0s: Added data:temp");

    try {
      Thread.sleep(2000);
      System.out.println("      Time 2s: Accessing data:temp = " + accessCache.getIfPresent("data:temp"));
      System.out.println("      ↳ Still there! Access timer reset to 0");

      Thread.sleep(2000);
      System.out.println("      Time 4s: Accessing data:temp = " + accessCache.getIfPresent("data:temp"));
      System.out.println("      ↳ Still there! Each access resets the 3-second timer");

      System.out.println("      Now waiting 4 seconds without accessing...");
      Thread.sleep(4000);
      System.out.println("      Time 8s: Accessing data:temp = " + accessCache.getIfPresent("data:temp"));
      System.out.println("      ↳ Expired! No access for 4 seconds (> 3 second TTL)");

    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }

    System.out.println("\n   Key Difference:");
    System.out.println("   • expireAfterWrite: Timer starts from write time, never resets");
    System.out.println("   • expireAfterAccess: Timer resets on every read/write access");
    System.out.println();
  }

  /**
   * Purge - Manual cache invalidation (single key or bulk)
   * Used when you need immediate invalidation for specific data
   */
  public void demoPurge() {
    System.out.println("2. Purge (Manual Invalidation):");
    System.out.println("   - Immediate removal of specific cache entries");
    System.out.println("   - Used when data becomes invalid or needs immediate update");

    Cache<String, String> cache = Caffeine.newBuilder().maximumSize(100).build();

    // Populate the cache
    cache.put("user:1", "John Doe");
    cache.put("user:2", "Jane Smith");
    cache.put("user:3", "Bob Johnson");
    cache.put("config:app", "v1.0");
    cache.put("config:db", "localhost:5432");

    System.out.println("   Initial cache size: " + cache.estimatedSize() + " items");
    System.out.println("   user:1 = " + cache.getIfPresent("user:1"));

    // Single key purge
    System.out.println("\n   a) Single Key Purge:");
    cache.invalidate("user:1");
    System.out.println("   After purging user:1: " + cache.getIfPresent("user:1"));
    System.out.println("   Remaining size: " + cache.estimatedSize());

    // Pattern-based purge (simulated)
    System.out.println("\n   b) Pattern-based Purge (config:* entries):");
    cache.asMap().keySet().removeIf(key -> key.startsWith("config:"));
    System.out.println("   After purging config entries: " + cache.estimatedSize() + " items");

    // Bulk purge
    System.out.println("\n   c) Bulk Purge (all remaining):");
    cache.invalidateAll();
    System.out.println("   After bulk purge: " + cache.estimatedSize() + " items");
    System.out.println();
  }

  /**
   * Refresh - Update cache with fresh data from source
   * Proactive approach to maintain data freshness
   */
  public void demoRefresh() {
    System.out.println("3. Refresh (Reload from source):");
    System.out.println("   - Proactively update cache with fresh data");
    System.out.println("   - Can be manual or automatic based on refresh policies");

    // Manual refresh with LoadingCache
    LoadingCache<String, String> cache = Caffeine.newBuilder()
        .maximumSize(100)
        .recordStats()
        .build(key -> {
          System.out.println("   Loading/refreshing data for: " + key);
          if (key.equals("special")) {
            return null; // Simulate data not found
          }
          return "fresh_" + key + "_" + System.currentTimeMillis();
        });

    try {
      // Initial load
      System.out.println("\n   a) Initial Load:");
      cache.put("data:1", "old_value_1");
      cache.put("special", "old_special");
      System.out.println("   data:1 = " + cache.getIfPresent("data:1"));
      System.out.println("   special = " + cache.getIfPresent("special"));

      // Manual refresh
      System.out.println("\n   b) Manual Refresh:");
      cache.refresh("data:1");
      Thread.sleep(100); // Allow refresh to complete
      System.out.println("   data:1 after refresh = " + cache.getIfPresent("data:1"));

      // Refresh with null result
      cache.refresh("special");
      Thread.sleep(100);
      System.out.println("   special after refresh = " + cache.getIfPresent("special"));

    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }

    // Automatic refresh policy
    System.out.println("\n   c) Automatic Refresh Policy:");
    LoadingCache<String, String> autoRefreshCache = Caffeine.newBuilder()
        .refreshAfterWrite(Duration.ofSeconds(1))
        .build(key -> "auto_refreshed_" + key + "_" + System.currentTimeMillis());

    try {
      System.out.println("   Setting initial value...");
      String initial = autoRefreshCache.get("config");
      System.out.println("   Initial: " + initial);

      Thread.sleep(1500);
      System.out.println("   After refresh period: " + autoRefreshCache.get("config"));
    } catch (Exception e) {
      System.err.println("   Error in refresh demo: " + e.getMessage());
    }
    System.out.println();
  }

  /**
   * Stale-While-Revalidate - Serve stale data while refreshing in background
   * Optimizes user experience by avoiding cache miss delays
   *
   * Note: Caffeine doesn't have built-in stale-while-revalidate, but we can implement it
   * using refreshAfterWrite and custom logic.
   */
  public void demoStaleWhileRevalidate() {
    System.out.println("4. Stale-While-Revalidate Pattern:");
    System.out.println("   - Returns stale data immediately while refreshing in background");
    System.out.println("   - Minimizes perceived latency for users");
    System.out.println("   - Caffeine doesn't have built-in support, but can be implemented");

    // Approach 1: Using refreshAfterWrite (closest to stale-while-revalidate)
    System.out.println("\n   a) Using refreshAfterWrite (Caffeine's closest equivalent):");
    LoadingCache<String, String> refreshCache = Caffeine.newBuilder()
        .refreshAfterWrite(Duration.ofSeconds(2))
        .build(key -> {
          System.out.println("   Loading fresh data for: " + key);
          try {
            Thread.sleep(500); // Simulate slow data source
          } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
          }
          return "fresh_data_" + System.currentTimeMillis();
        });

    try {
      // Initial load
      String initial = refreshCache.get("api:data");
      System.out.println("   Initial load: " + initial);

      Thread.sleep(1000);
      System.out.println("   Access after 1s: " + refreshCache.get("api:data"));
      System.out.println("   ↳ Same data - refresh not triggered yet");

      Thread.sleep(1500);
      System.out.println("   Access after 2.5s: " + refreshCache.get("api:data"));
      System.out.println("   ↳ Returns stale data immediately, triggers background refresh");

      Thread.sleep(1000);
      System.out.println("   Access after 3.5s: " + refreshCache.get("api:data"));
      System.out.println("   ↳ Now shows refreshed data");

    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }

    // Approach 2: Custom stale-while-revalidate with dual cache
    System.out.println("\n   b) Custom implementation with dual cache pattern:");

    Cache<String, String> primaryCache = Caffeine.newBuilder()
        .expireAfterWrite(Duration.ofSeconds(2))
        .build();

    Cache<String, String> staleCache = Caffeine.newBuilder()
        .expireAfterWrite(Duration.ofSeconds(10)) // Keep stale data longer
        .build();

    String key = "user:profile";
    String originalValue = "profile_v1_" + System.currentTimeMillis();

    // Store in both caches
    primaryCache.put(key, originalValue);
    staleCache.put(key, originalValue);
    System.out.println("   Stored initial value: " + originalValue);

    try {
      Thread.sleep(2500); // Let primary cache expire

      // Custom stale-while-revalidate logic
      String fresh = primaryCache.getIfPresent(key);
      if (fresh == null) {
        // Return stale data immediately
        String stale = staleCache.getIfPresent(key);
        System.out.println("   Primary cache miss - returning stale: " + stale);

        // Trigger async refresh
        executor.submit(() -> {
          try {
            Thread.sleep(300); // Simulate API call
            String newValue = "profile_v2_" + System.currentTimeMillis();
            primaryCache.put(key, newValue);
            staleCache.put(key, newValue);
            System.out.println("   Background refresh completed: " + newValue);
          } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
          }
        });

        // Return stale data to user immediately (no waiting)
        if (stale != null) {
          System.out.println("   ↳ User gets immediate response with stale data");
        }
      }

      Thread.sleep(500);
      System.out.println("   After refresh: " + primaryCache.getIfPresent(key));

    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }

    System.out.println("\n   Summary:");
    System.out.println("   • refreshAfterWrite: Caffeine's built-in solution");
    System.out.println("   • Custom dual cache: Full stale-while-revalidate control");
    System.out.println("   • Both avoid user-facing latency during refresh");
    System.out.println();
  }

  /**
   * Ban Pattern - Invalidate related entries based on tags or patterns
   * Useful for invalidating groups of related cache entries
   */
  public void demoBanPattern() {
    System.out.println("5. Ban Pattern (Tag-based Invalidation):");
    System.out.println("   - Invalidate groups of related cache entries");
    System.out.println("   - Useful for complex data relationships");

    Cache<String, String> cache = Caffeine.newBuilder().maximumSize(100).build();

    // Populate cache with tagged data
    cache.put("user:1:profile", "John's Profile");
    cache.put("user:1:settings", "John's Settings");
    cache.put("user:1:posts", "John's Posts");
    cache.put("user:2:profile", "Jane's Profile");
    cache.put("user:2:settings", "Jane's Settings");
    cache.put("global:config", "Global Config");

    System.out.println("   Initial cache size: " + cache.estimatedSize() + " items");

    // Ban all entries for user:1
    System.out.println("\n   a) Banning all entries for user:1:");
    // Count entries before removal
    long bannedCount = cache.asMap().keySet().stream()
        .filter(key -> key.startsWith("user:1:"))
        .count();
    cache.asMap().keySet().removeIf(key -> key.startsWith("user:1:"));
    System.out.println("   Banned " + bannedCount + " entries");
    System.out.println("   Remaining size: " + cache.estimatedSize());

    // Show remaining entries
    System.out.println("   Remaining entries:");
    cache.asMap().forEach((key, value) ->
        System.out.println("   - " + key + " = " + value));

    // Ban all user entries (simulating user data invalidation)
    System.out.println("\n   b) Banning all user entries:");
    cache.asMap().keySet().removeIf(key -> key.startsWith("user:"));
    System.out.println("   Remaining size: " + cache.estimatedSize());
    System.out.println();
  }
}
