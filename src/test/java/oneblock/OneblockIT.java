package oneblock;

/**
 * Integration tests for Oneblock class using MockBukkit.
 *
 * <p>MockBukkit v1.21 is incompatible with Spigot 26.1.2-SNAPSHOT due to SimpleCommandMap
 * constructor signature changes. Server integration tests are skipped until either: 1) Spigot API
 * is downgraded to match MockBukkit (breaks project), 2) MockBukkit is upgraded to a compatible
 * version, or 3) A different testing approach is used.
 *
 * <p>Database integration tests are working and provide coverage for DatabaseManager.
 */
class OneblockIT {
  // Placeholder for future MockBukkit integration tests when compatibility is resolved
}
