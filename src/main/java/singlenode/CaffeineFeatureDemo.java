package singlenode;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;


public class CaffeineFeatureDemo {
  public static void main(String[] args) {
    CaffeineFeatureDemo demo = new CaffeineFeatureDemo();
    String result = demo.demoEviction();
    System.out.println(result);
  }

  public String demoEviction() {
    // Note: Caffeine uses Window TinyLFU by default, not pure LRU
    // For true LRU behavior, we need to understand that Caffeine's algorithm
    // considers both recency and frequency for better performance
    Cache<String, String> cache = Caffeine.newBuilder()
        .maximumSize(3)
        .build();

    cache.put("a", "A");  // Order: a
    cache.put("b", "B");  // Order: a, b
    cache.put("c", "C");  // Order: a, b, c

    // Access "a" to make it recently used
    cache.getIfPresent("a"); // This makes "a" recently accessed

    // Add 4th entry - this should evict the least recently used
    cache.put("d", "D");

    // Force eviction to happen immediately
    cache.cleanUp();

    String valueB = cache.getIfPresent("b"); // Check if "b" was evicted
    String valueA = cache.getIfPresent("a"); // Should be "A" (recently accessed)
    String valueC = cache.getIfPresent("c"); // May or may not be evicted
    String valueD = cache.getIfPresent("d"); // Should be "D" (just added)

    return String.format("B: %s, A: %s, C: %s, D: %s", valueB, valueA, valueC, valueD);
  }
}
