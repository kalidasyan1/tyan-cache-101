package singlenode;

import com.github.benmanes.caffeine.cache.*;

public class CaffeineBasicDemo {
  public static void main(String[] args) {
    Cache<String, String> cache = Caffeine.newBuilder()
        .maximumSize(100)
        .build();

    // Put and get
    cache.put("key1", "value1");
    String value = cache.getIfPresent("key1"); // "value1"
    System.out.println(value);

    // Load automatically
    String computed = cache.get("key2", k -> "computed-" + k);
    System.out.println(computed); // "computed-key2"
  }
}
