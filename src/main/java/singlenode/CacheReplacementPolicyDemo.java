package singlenode;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Demonstrates different cache replacement policies
 */
public class CacheReplacementPolicyDemo {

  public static void main(String[] args) {
    CacheReplacementPolicyDemo demo = new CacheReplacementPolicyDemo();

    System.out.println("=== Cache Replacement Policies Demo ===\n");

    demo.demonstrateLRU();
    demo.demonstrateLFU();
    demo.demonstrateFIFO();
    demo.caffeinePolicyComparison();
  }

  /**
   * LRU (Least Recently Used) - Evicts least recently accessed items
   */
  public void demonstrateLRU() {
    System.out.println("1. LRU (Least Recently Used) Policy:");
    System.out.println("   - Evicts items that haven't been accessed recently");

    // Simple LRU implementation using LinkedHashMap
    Map<String, String> lruCache = new LinkedHashMap<String, String>(3, 0.75f, true) {
      @Override
      protected boolean removeEldestEntry(Map.Entry<String, String> eldest) {
        return size() > 3; // Max capacity of 3
      }
    };

    // Fill cache
    lruCache.put("A", "ValueA");
    lruCache.put("B", "ValueB");
    lruCache.put("C", "ValueC");
    System.out.println("Initial cache: " + lruCache.keySet());

    // Access A and B to make them recently used
    lruCache.get("A");
    lruCache.get("B");
    System.out.println("After accessing A and B: " + lruCache.keySet());

    // Add new item - should evict C (least recently used)
    lruCache.put("D", "ValueD");
    System.out.println("After adding D (C evicted): " + lruCache.keySet());
    System.out.println();
  }

  /**
   * LFU (Least Frequently Used) - Evicts least frequently accessed items
   */
  public void demonstrateLFU() {
    System.out.println("2. LFU (Least Frequently Used) Policy:");
    System.out.println("   - Evicts items with lowest access frequency");

    // Caffeine uses W-TinyLFU which is an advanced LFU variant
    Cache<String, String> lfuCache = Caffeine.newBuilder()
        .maximumSize(3)
        .recordStats()
        .build();

    // Fill cache and establish frequency patterns
    lfuCache.put("X", "ValueX");
    lfuCache.put("Y", "ValueY");
    lfuCache.put("Z", "ValueZ");
    System.out.println("Initial cache - X, Y, Z added");

    // Create distinct frequency patterns
    System.out.println("Creating frequency patterns:");

    // Access X multiple times (high frequency)
    System.out.println("  Accessing X 10 times (high frequency)");
    for (int i = 0; i < 10; i++) {
      lfuCache.getIfPresent("X");
    }

    // Access Y moderately (medium frequency)
    System.out.println("  Accessing Y 3 times (medium frequency)");
    for (int i = 0; i < 3; i++) {
      lfuCache.getIfPresent("Y");
    }

    // Access Z only once (low frequency)
    System.out.println("  Accessing Z 1 time (low frequency)");
    lfuCache.getIfPresent("Z");

    // Force cache maintenance to ensure frequency data is processed
    lfuCache.cleanUp();

    System.out.println("Cache state before eviction:");
    System.out.println("  X: " + lfuCache.getIfPresent("X"));
    System.out.println("  Y: " + lfuCache.getIfPresent("Y"));
    System.out.println("  Z: " + lfuCache.getIfPresent("Z"));

    // Add new items to trigger eviction - add multiple to force eviction
    System.out.println("\nAdding new items W and V to trigger eviction...");
    lfuCache.put("W", "ValueW");
    lfuCache.cleanUp(); // Force maintenance

    lfuCache.put("V", "ValueV");
    lfuCache.cleanUp(); // Force maintenance again

    System.out.println("\nAfter eviction (least frequent items should be evicted):");
    System.out.println("  X (high freq): " + (lfuCache.getIfPresent("X") != null ? "KEPT" : "EVICTED"));
    System.out.println("  Y (med freq): " + (lfuCache.getIfPresent("Y") != null ? "KEPT" : "EVICTED"));
    System.out.println("  Z (low freq): " + (lfuCache.getIfPresent("Z") != null ? "KEPT" : "EVICTED"));
    System.out.println("  W (new): " + (lfuCache.getIfPresent("W") != null ? "KEPT" : "EVICTED"));
    System.out.println("  V (new): " + (lfuCache.getIfPresent("V") != null ? "KEPT" : "EVICTED"));

    System.out.println("Final cache size: " + lfuCache.estimatedSize());

    var stats = lfuCache.stats();
    System.out.println("Hit rate: " + String.format("%.2f%%", stats.hitRate() * 100));
    System.out.println("Eviction count: " + stats.evictionCount());
    System.out.println();
  }

  /**
   * FIFO (First In, First Out) - Evicts oldest inserted items
   */
  public void demonstrateFIFO() {
    System.out.println("3. FIFO (First In, First Out) Policy:");
    System.out.println("   - Evicts items in order they were inserted (oldest first)");

    // FIFO implementation using LinkedHashMap without access-order
    Map<String, String> fifoCache = new LinkedHashMap<String, String>(3, 0.75f, false) {
      @Override
      protected boolean removeEldestEntry(Map.Entry<String, String> eldest) {
        if (size() > 3) {
          System.out.println("   Evicting oldest entry: " + eldest.getKey());
          return true;
        }
        return false;
      }
    };

    // Fill cache in order
    System.out.println("Adding items in order: P, Q, R");
    fifoCache.put("P", "ValueP");
    fifoCache.put("Q", "ValueQ");
    fifoCache.put("R", "ValueR");
    System.out.println("Cache contents: " + fifoCache.keySet());

    // Access middle item multiple times (shouldn't affect FIFO order)
    fifoCache.get("Q");
    fifoCache.get("Q");
    System.out.println("After accessing Q multiple times: " + fifoCache.keySet());

    // Add new item - should evict P (first in)
    System.out.println("Adding S (should evict P):");
    fifoCache.put("S", "ValueS");
    System.out.println("Cache contents: " + fifoCache.keySet());

    // Add another item - should evict Q (next first in)
    System.out.println("Adding T (should evict Q):");
    fifoCache.put("T", "ValueT");
    System.out.println("Cache contents: " + fifoCache.keySet());
    System.out.println();
  }

  /**
   * Compares Caffeine's advanced policy with traditional ones
   */
  public void caffeinePolicyComparison() {
    System.out.println("4. Caffeine's W-TinyLFU vs Traditional Policies:");
    System.out.println("   - W-TinyLFU combines benefits of LRU and LFU");
    System.out.println("   - Uses frequency and recency information");
    System.out.println("   - More efficient than pure LRU or LFU");

    Cache<String, String> caffeineCache = Caffeine.newBuilder()
        .maximumSize(4)
        .recordStats()
        .build();

    // Simulate workload with mixed access patterns
    System.out.println("\nSimulating mixed access patterns:");

    // Phase 1: Initial load
    caffeineCache.put("frequent", "value1");
    caffeineCache.put("recent", "value2");
    caffeineCache.put("old", "value3");
    caffeineCache.put("rare", "value4");

    // Phase 2: Create access patterns
    // Make "frequent" highly accessed
    for (int i = 0; i < 20; i++) {
      caffeineCache.getIfPresent("frequent");
    }

    // Make "recent" recently accessed
    try {
      Thread.sleep(100);
      for (int i = 0; i < 5; i++) {
        caffeineCache.getIfPresent("recent");
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }

    // "old" accessed moderately in the past
    for (int i = 0; i < 3; i++) {
      caffeineCache.getIfPresent("old");
    }

    // "rare" accessed very little
    caffeineCache.getIfPresent("rare");

    // Phase 3: Add new items to trigger eviction
    caffeineCache.put("new1", "newValue1");
    caffeineCache.put("new2", "newValue2");

    System.out.println("After adding new items:");
    System.out.println("frequent (high freq): " + caffeineCache.getIfPresent("frequent"));
    System.out.println("recent (recent): " + caffeineCache.getIfPresent("recent"));
    System.out.println("old (moderate): " + caffeineCache.getIfPresent("old"));
    System.out.println("rare (low freq): " + caffeineCache.getIfPresent("rare"));
    System.out.println("new1: " + caffeineCache.getIfPresent("new1"));
    System.out.println("new2: " + caffeineCache.getIfPresent("new2"));

    var stats = caffeineCache.stats();
    System.out.println("\nCache Statistics:");
    System.out.println("Hit Rate: " + String.format("%.2f%%", stats.hitRate() * 100));
    System.out.println("Miss Rate: " + String.format("%.2f%%", stats.missRate() * 100));
    System.out.println("Eviction Count: " + stats.evictionCount());
  }
}
