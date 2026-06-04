package oneblock.storage;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import oneblock.PlayerInfo;
import oneblock.context.PluginContext;

/**
 * Gson-backed player data store. Writes / reads the {@code PlData.json} file in the plugin data
 * folder. Used as the primary persistence layer when no database is configured (or the database
 * save returned false).
 *
 * <p>Renamed from {@code JsonSimple} in Phase 3 (storage package rename). Method names lower-cased
 * to follow Java conventions; the file location ({@link #f}) and on-disk schema are unchanged.
 *
 * <p>Phase 3.5: Migrated from {@code Oneblock.plugin.getDataFolder()} to {@link PluginContext} for
 * testability.
 *
 * <p>Phase 5: Migrated from json-simple to Gson for better type safety and maintainability.
 * Implements {@link PlayerDataStore} for DI support.
 */
public class JsonPlayerDataStore implements PlayerDataStore {
  private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
  private static final Type playerInfoListType = new TypeToken<List<PlayerInfo>>() {}.getType();

  @SuppressFBWarnings(
      value = "CT_CONSTRUCTOR_THROW",
      justification =
          "The field initializer calls getDataFolder() which may throw IllegalStateException if"
              + " PluginContext is not initialized. This is intentional - it's a programming error"
              + " to use this class before the plugin is initialized. The exception is appropriate"
              + " to fail fast.")
  public static class Config {
    public final File f = new File(getDataFolder(), "PlData.json");

    private File getDataFolder() {
      PluginContext ctx = PluginContext.get();
      if (ctx == null) {
        throw new IllegalStateException("PluginContext not initialized");
      }
      return ctx.dataFolder();
    }
  }

  private static Config config = new Config();

  public static Config getConfig() {
    return config;
  }

  public static void setConfig(Config newConfig) {
    config = newConfig;
  }

  // Legacy static field accessor for backward compatibility during Phase 3 migration
  public static final File f = config.f;

  // Instance methods for PlayerDataStore interface
  @Override
  public void write(List<PlayerInfo> pls) {
    try (Writer writer =
        new OutputStreamWriter(new FileOutputStream(config.f), StandardCharsets.UTF_8)) {
      gson.toJson(pls, playerInfoListType, writer);
    } catch (IOException e) {
      PluginContext ctx = PluginContext.get();
      if (ctx != null) {
        ctx.plugin().getLogger().warning("Failed to write player data to JSON: " + e.getMessage());
      }
    }
  }

  @Override
  public List<PlayerInfo> read() {
    if (!config.f.exists()) {
      return new ArrayList<>();
    }
    try (Reader reader =
        new InputStreamReader(new FileInputStream(config.f), StandardCharsets.UTF_8)) {
      List<PlayerInfo> result = gson.fromJson(reader, playerInfoListType);
      return result != null ? result : new ArrayList<>();
    } catch (IOException | com.google.gson.JsonSyntaxException e) {
      PluginContext ctx = PluginContext.get();
      if (ctx != null) {
        ctx.plugin().getLogger().warning("Failed to read player data to JSON: " + e.getMessage());
      }
      return new ArrayList<>();
    }
  }

  // Static methods for backward compatibility (renamed to avoid interface conflict)
  public static void writeStatic(List<PlayerInfo> pls) {
    new JsonPlayerDataStore().write(pls);
  }

  public static List<PlayerInfo> readStatic() {
    return new JsonPlayerDataStore().read();
  }
}
