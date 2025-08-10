package tutorial;

import singlenode.*;
import distributed.*;

/**
 * Cache Tutorial Runner - Step-by-Step Guide
 *
 * This tutorial demonstrates comprehensive caching concepts using:
 * - Caffeine: Most popular single-node cache framework
 * - Redis: Most popular distributed cache framework (Legacy vs Modern clients)
 *
 * Topics covered:
 * 1. Cache Replacement Policies: LRU, LFU, FIFO
 * 2. Cache Write Policies: Write-through, Write-around, Write-behind, Write-back
 * 3. Cache Invalidation Methods: TTL, Purge, Refresh, Ban, Stale-while-revalidate
 * 4. Legacy vs Modern Redis clients: JedisPool vs Lettuce
 */
public class CacheTutorialRunner {

  public static void main(String[] args) throws InterruptedException {
    System.out.println("🚀 Welcome to the Complete Cache Tutorial!");
    System.out.println("=========================================\n");

    printTutorialOverview();

    // Run tutorials based on what's available
    if (args.length > 0) {
      switch (args[0].toLowerCase()) {
        case "caffeine":
          runCaffeineOnlyTutorial();
          break;
        case "redis":
          runRedisOnlyTutorial();
          break;
        case "redis-modern":
          runModernRedisTutorial();
          break;
        case "comparison":
          runRedisComparison();
          break;
        case "full":
        default:
          runFullTutorial();
          break;
      }
    } else {
      runFullTutorial();
    }
  }

  private static void printTutorialOverview() {
    System.out.println("📚 Tutorial Structure:");
    System.out.println("├── Single-Node Caching (Caffeine)");
    System.out.println("│   ├── Basic Usage & Features");
    System.out.println("│   ├── Cache Replacement Policies (LRU, LFU, FIFO)");
    System.out.println("│   ├── Write Policies & Invalidation Strategies");
    System.out.println("│   └── Advanced Features (Loading, Async)");
    System.out.println("│");
    System.out.println("└── Distributed Caching (Redis)");
    System.out.println("    ├── Basic Operations & Data Types (Legacy Jedis)");
    System.out.println("    ├── Modern Redis Client (Lettuce - Recommended)");
    System.out.println("    ├── Cache Policies & Advanced Patterns");
    System.out.println("    └── Legacy vs Modern Comparison");
    System.out.println();

    System.out.println("💡 Usage Options:");
    System.out.println("   java CacheTutorialRunner [caffeine|redis|redis-modern|comparison|full]");
    System.out.println();
  }

  private static void runCaffeineOnlyTutorial() throws InterruptedException {
    System.out.println("🔧 Running Caffeine-Only Tutorial...\n");

    runCaffeineDemo();

    System.out.println("✅ Caffeine tutorial completed!");
    System.out.println("💡 Next: Try 'redis' or 'redis-modern' to learn distributed caching");
  }

  private static void runRedisOnlyTutorial() {
    System.out.println("🔧 Running Redis Tutorial (Legacy Jedis)...\n");

    RedisBasicDemo.main(new String[]{});
    waitForUser();
    RedisCachePoliciesDemo.main(new String[]{});

    System.out.println("✅ Redis (legacy) tutorial completed!");
    System.out.println("💡 Try 'redis-modern' to see the modern approach with Lettuce");
  }

  private static void runModernRedisTutorial() {
    System.out.println("🔧 Running Modern Redis Tutorial (Lettuce)...\n");

    RedisModernDemo.main(new String[]{});

    System.out.println("✅ Modern Redis tutorial completed!");
    System.out.println("💡 Try 'comparison' to see legacy vs modern differences");
  }

  private static void runRedisComparison() {
    System.out.println("🔧 Running Redis Legacy vs Modern Comparison...\n");

    System.out.println("=== LEGACY JEDIS APPROACH ===");
    RedisBasicDemo.main(new String[]{});

    waitForUser();

    System.out.println("\n=== MODERN LETTUCE APPROACH ===");
    RedisModernDemo.main(new String[]{});

    printRedisComparison();
  }

  private static void runFullTutorial() throws InterruptedException {
    System.out.println("🔧 Running Complete Tutorial...\n");

    runCaffeineDemo();
    waitForUser();

    runRedisComparison();

    System.out.println("🎉 Complete cache tutorial finished!");
    System.out.println("📖 Check README.md for more details and advanced examples");
  }

  private static void runCaffeineDemo() throws InterruptedException {
    CaffeineBasicDemo.main(new String[]{});
    waitForUser();
    CacheReplacementPolicyDemo.main(new String[]{});
    waitForUser();
    CacheWritePolicyDemo.main(new String[]{});
    waitForUser();
    CacheInvalidationStrategyDemo.main(new String[]{});
  }

  private static void printRedisComparison() {
    System.out.println("\n📊 REDIS CLIENT COMPARISON");
    System.out.println("==========================");
    System.out.println();
    System.out.println("JedisPool (Legacy)              vs      Lettuce (Modern)");
    System.out.println("─────────────────────────────────────────────────────────────");
    System.out.println("• Synchronous/Blocking                  • Async/Non-blocking");
    System.out.println("• Thread-per-connection                 • Connection sharing");
    System.out.println("• Manual resource management            • Auto resource management");
    System.out.println("• No reactive support                   • Full reactive support");
    System.out.println("• Basic connection pooling              • Advanced connection pooling");
    System.out.println("• Manual reconnection                   • Auto-reconnection");
    System.out.println("• Spring Boot 1.x default              • Spring Boot 2.0+ default");
    System.out.println();
    System.out.println("🚀 Recommendation: Use Lettuce for new projects!");
    System.out.println("📚 Both implementations are shown for educational purposes.");
  }

  private static void waitForUser() {
    System.out.println("\n⏸️  Press Enter to continue to the next section...");
    try {
      System.in.read();
    } catch (Exception e) {
      // Ignore
    }
  }
}
