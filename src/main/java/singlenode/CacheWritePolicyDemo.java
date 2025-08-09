package singlenode;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;


public class CacheWritePolicyDemo {
  Map<String, String> db = new HashMap<>();

  public static void main(String[] args) throws InterruptedException {
    CacheWritePolicyDemo demo = new CacheWritePolicyDemo();
    demo.demoWriteThrough();
    demo.demoWriteAround();
    demo.demoWriteBehind();
  }

  public void demoWriteThrough() {
    System.out.println("Demoing Write-Through Cache with Caffeine");
    System.out.println("Write to cache and store simultaneously.");
    // Note: Caffeine does not have a built-in write-through cache feature.
    // Instead, we can simulate it by using a CacheLoader that writes to an external store.
    // Here, we will use a simple example with a simulated database.

    Cache<String, String> cache = Caffeine.newBuilder().maximumSize(100).build();

    db.clear();

    // Load data into cache and database
    String key = "key1";
    String value = "value1";
    cache.put(key, value);
    db.put(key, value);

    // Read from cache
    String cachedValue = cache.getIfPresent(key);

    // Simulate write-through by updating both cache and database
    String newValue = "new-value1";
    cache.put(key, newValue);
    db.put(key, newValue);

    String output = String.format("Cached Value: %s, New Value in DB: %s", cachedValue, db.get(key));
    System.out.println(output);
    System.out.println();
  }

  public void demoWriteAround() {
    System.out.println("Demoing Write-Around Cache with Caffeine");
    System.out.println("Write to store; read from cache (load from store if not present).");
    // Note: Caffeine does not have a built-in write-around cache feature.
    // Instead, we can simulate it by not updating the cache on writes.

    Cache<String, String> cache = Caffeine.newBuilder().maximumSize(100).build();

    db.clear();

    // Load data into database
    String key = "key1";
    String value = "value1";
    db.put(key, value);

    // Read from cache (load from DB if not present)
    String valueFromCache = cache.get(key, k -> db.get(k)); // This will load from DB if not present in cache
    System.out.println("Value from Cache (loaded from DB): " + valueFromCache);
    System.out.println();
  }

  public void demoWriteBehind() throws InterruptedException {
    System.out.println("Demoing Write-Behind Cache with Caffeine");
    System.out.println("Write to cache; asynchronously write to store later.");
    // Note: Caffeine does not have a built-in write-behind cache feature.
    // Instead, we can simulate it by updating the cache and then writing to the database later.

    Cache<String, String> cache = Caffeine.newBuilder().maximumSize(100).build();

    db.clear();

    // Load data into cache and database
    String key = "key1";
    String value = "value1";

    // Write to cache
    cache.put(key, value);

    // Simulate asynchronous write to database
    ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    scheduler.schedule(() -> {
      // Write to database after some delay
      db.put(key, value);
      System.out.println("Asynchronously wrote to DB: " + key + " = " + db.get(key));
      System.out.println();
    }, 1, java.util.concurrent.TimeUnit.SECONDS);

    db.get(key); // DB should be empty at this point
    String cachedValue = cache.getIfPresent(key);
    String output = String.format("Cached Value: %s, DB Value (before async write): %s", cachedValue, db.get(key));
    System.out.println(output);

    Thread.sleep(2000); // Wait for async write to complete
    scheduler.shutdown();
  }
}
