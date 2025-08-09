package singlenode;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;


public class CacheInvalidationStrategyDemo {
  public static void main(String[] args) {
    CacheInvalidationStrategyDemo demo = new CacheInvalidationStrategyDemo();
    demo.demoPurge();
    demo.demoRefresh();
  }

  public void demoPurge() {
    System.out.println("------------Demoing Cache Invalidation Strategy with Caffeine------------");
    Cache<String, String> cache = Caffeine.newBuilder().maximumSize(100).build();

    // Populate the cache
    cache.put("key1", "value1");
    cache.put("key2", "value2");
    cache.put("key3", "value3");

    // Access some entries to simulate usage
    String value1 = cache.getIfPresent("key1"); // Should be "value1"
    System.out.println("Value for key1: " + value1);

    // Simulate a purge operation
    cache.invalidate("key1"); // This will remove key1 from the cache
    String value1AfterPurge = cache.getIfPresent("key1"); // Should be null after purge
    System.out.println("Value for key1 after purge: " + value1AfterPurge);

    // Check remaining entries
    String value2 = cache.getIfPresent("key2"); // Should still be "value2"
    String value3 = cache.getIfPresent("key3"); // Should still be "value3"
    System.out.println("Value for key2: " + value2);
    System.out.println("Value for key3: " + value3);

    cache.invalidateAll();
    System.out.println("All entries invalidated.");
    String value2AfterInvalidate = cache.getIfPresent("key2"); // Should be null after invalidate
    String value3AfterInvalidate = cache.getIfPresent("key3"); // Should be null after invalidate
    System.out.println("Value for key2 after invalidate: " + value2AfterInvalidate);
    System.out.println("Value for key3 after invalidate: " + value3AfterInvalidate);
    System.out.println("Cache invalidation strategy demo completed.");
    System.out.println("------------------------------------------------");
  }

  public void demoRefresh() {
    System.out.println("------------Demoing Cache Refresh Strategy with Caffeine------------");

    // Use LoadingCache with expireAfterWrite for clearer refresh demonstration
    LoadingCache<String, String> cache = Caffeine.newBuilder()
        .maximumSize(100)
        .expireAfterWrite(java.time.Duration.ofSeconds(3)) // Expire after 3 seconds
        .build(key -> {
          // This is the refresh logic - called when a value needs to be loaded/refreshed
          System.out.println("Loading/refreshing value for key: " + key);
          if (key.equals("B")) {
            return null; // Return null for key 'B' as expected
          }
          return "refreshed_" + key + "_" + System.currentTimeMillis();
        });

    // Populate the cache
    cache.put("A", "A");
    cache.put("B", "B");
    cache.put("C", "C");
    cache.put("D", "D");

    // Access entries to see initial values
    System.out.println("Initial values:");
    System.out.println("B: " + cache.getIfPresent("B") + ", A: " + cache.getIfPresent("A") +
                       ", C: " + cache.getIfPresent("C") + ", D: " + cache.getIfPresent("D"));

    //Loads a new value for the key, asynchronously. While the new value is loading the previous value (if any) will continue to be returned by get(key) unless it is evicted.
    cache.refresh("A"); // Trigger refresh for key 'A'
    System.out.println("After refresh for A:");
    System.out.println("A: " + cache.getIfPresent("A"));

    // Wait for 4 seconds to trigger expiration
    try {
      Thread.sleep(4000);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }

    // Access the entries again - this will trigger loading for expired entries
    System.out.println("After expiration period:");
    System.out.println("B: " + cache.get("B") + ", A: " + cache.get("A") +
                       ", C: " + cache.get("C") + ", D: " + cache.get("D"));
    System.out.println("-------------------------------------------------");
  }
}
