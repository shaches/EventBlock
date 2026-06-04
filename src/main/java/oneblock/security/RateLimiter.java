package oneblock.security;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Simple rate limiter to prevent command spam and abuse. Phase 6: Security measure to protect
 * against DoS attacks via command spam.
 */
public class RateLimiter {
  private static final ConcurrentHashMap<UUID, PlayerBucket> buckets = new ConcurrentHashMap<>();
  private static final int MAX_REQUESTS_PER_WINDOW = 10;
  private static final long WINDOW_MS = 1000; // 1 second window

  private static class PlayerBucket {
    private final AtomicInteger count = new AtomicInteger(0);
    private long windowStart = System.currentTimeMillis();

    boolean tryConsume() {
      long now = System.currentTimeMillis();
      if (now - windowStart > WINDOW_MS) {
        count.set(0);
        windowStart = now;
      }
      return count.incrementAndGet() <= MAX_REQUESTS_PER_WINDOW;
    }
  }

  /**
   * Check if a player is allowed to execute a command.
   *
   * @param playerUuid The player's UUID
   * @return true if allowed, false if rate limited
   */
  public static boolean tryConsume(UUID playerUuid) {
    PlayerBucket bucket = buckets.computeIfAbsent(playerUuid, k -> new PlayerBucket());
    return bucket.tryConsume();
  }

  /**
   * Reset the rate limit for a player (e.g., after a cooldown period).
   *
   * @param playerUuid The player's UUID
   */
  public static void reset(UUID playerUuid) {
    buckets.remove(playerUuid);
  }

  /**
   * Clean up old entries to prevent memory leaks. Should be called periodically (e.g., in a
   * scheduled task).
   */
  public static void cleanup() {
    long now = System.currentTimeMillis();
    buckets
        .entrySet()
        .removeIf(
            entry -> {
              PlayerBucket bucket = entry.getValue();
              return now - bucket.windowStart
                  > WINDOW_MS * 10; // Remove after 10 seconds of inactivity
            });
  }
}
