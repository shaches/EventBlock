package oneblock.context;

/**
 * Test utility for {@link PluginContext}. Provides public access to package-private methods needed
 * for test setup/teardown.
 */
public final class PluginContextTestUtil {

  private PluginContextTestUtil() {
    // Utility class - prevent instantiation
  }

  /**
   * Reset the PluginContext instance. Calls the package-private {@link PluginContext#reset()}
   * method. Only for use in tests to clean up between test runs.
   */
  public static void reset() {
    PluginContext.reset();
  }
}
