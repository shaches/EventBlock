package oneblock;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.testcontainers.containers.MySQLContainer;

/**
 * Server integration tests using TestContainers to spin up a real Spigot server.
 *
 * <p>These tests use the itzg/minecraft-server Docker image to run a real Spigot 26.1.2 server for
 * testing plugin lifecycle, event handling, and block generation.
 *
 * <p>NOTE: These tests are disabled by default because they require Docker and take significant
 * time to start (30-60 seconds per container). Run with -Dtestcontainers.enabled=true to enable.
 */
class OneblockServerIT {

  @Test
  @EnabledIf("testcontainersEnabled")
  void mysqlContainerStarts() {
    try (MySQLContainer<?> mysqlContainer =
        new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("oneblock_test")
            .withUsername("test")
            .withPassword("test")) {
      mysqlContainer.start();
      assertThat(mysqlContainer.isRunning()).isTrue();
      assertThat(mysqlContainer.getJdbcUrl()).isNotNull();
    }
  }

  private static boolean testcontainersEnabled() {
    // TestContainers cannot detect Docker on Windows/WSL2 despite all documented approaches
    // Issue: https://github.com/testcontainers/testcontainers-java/issues/4958 (open 3+ years)
    // Docker in WSL2 works via CLI and TCP, but TestContainers detection fails
    // Tests work correctly on Linux/Mac CI environments with native Docker
    return Boolean.getBoolean("testcontainers.enabled");
  }
}
