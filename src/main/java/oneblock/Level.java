package oneblock;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.entity.EntityType;

public final class Level {
  public static class Config {
    public Level max = new Level("__max__", "Level: MAX");
    public int multiplier = 5;
  }

  private static Config config = new Config();

  public static Config getConfig() {
    return config;
  }

  public static void setConfig(Config newConfig) {
    config = newConfig;
    max = config.max;
    multiplier = config.multiplier;
  }

  // Legacy static field accessors for backward compatibility during Phase 3 migration
  @SuppressFBWarnings(
      value = {"MS_SHOULD_BE_FINAL", "MS_CANNOT_BE_FINAL", "PA_PUBLIC_PRIMITIVE_ATTRIBUTE"},
      justification =
          "Legacy static field for backward compatibility during Phase 3 migration. This field"
              + " mirrors config.max and is retained for legacy code that directly accesses"
              + " Level.max instead of using the config-based accessor Level.max(). Will be removed"
              + " in a future phase once all callers are migrated to the config-based pattern.")
  public static Level max = config.max;

  @SuppressFBWarnings(
      value = {"MS_CANNOT_BE_FINAL", "PA_PUBLIC_PRIMITIVE_ATTRIBUTE"},
      justification =
          "Legacy static field for backward compatibility during Phase 3 migration. This field"
              + " mirrors config.multiplier and is intentionally non-final to allow runtime"
              + " configuration updates via ConfigManager. Will be removed in a future phase once"
              + " all callers are migrated to the config-based pattern.")
  public static int multiplier = config.multiplier;

  public static Level max() {
    return config.max;
  }

  public static int multiplier() {
    return config.multiplier;
  }

  /**
   * Levels list, published as an immutable snapshot via {@link #replaceAll(List)}. Phase 4.1 made
   * the field {@code volatile} and {@code private}: writers ({@code ConfigManager.loadBlocks})
   * build a fresh {@link ArrayList} fully and only call {@code replaceAll} once parsing completed
   * without throwing - if a parse step throws (rare, most YAML failures are tolerated with a
   * warning) the old list stays visible to readers. Readers ({@code Oneblock.generateBlock} and the
   * {@code OBP} placeholder helpers via {@link #get(int)} / {@link #size()}) snapshot the field
   * once and iterate the local reference, so they never observe a half-populated list during a
   * {@code /ob reload}.
   */
  private static volatile List<Level> levels = Collections.emptyList();

  public static Level get(int i) {
    List<Level> snapshot = levels;
    if (i < snapshot.size()) return snapshot.get(i);
    return max;
  }

  public static int size() {
    return levels.size();
  }

  /**
   * Snapshot of the current levels list. Always non-null and immutable; iterating it concurrently
   * with a {@link #replaceAll(List)} call from another thread is safe - the iterator walks the
   * captured pre-call reference even after the volatile field has been swapped.
   */
  public static List<Level> snapshot() {
    return levels;
  }

  /**
   * Atomically publish a new levels list. Defensive-copies the argument into an {@link
   * Collections#unmodifiableList(List) unmodifiable} wrapper before the volatile write so callers
   * cannot retroactively mutate the published state, then performs a single volatile assignment
   * which establishes a happens-before to every subsequent read of {@link #levels}. Passing {@code
   * null} or an empty list publishes {@link Collections#emptyList()}.
   */
  public static void replaceAll(List<Level> newLevels) {
    levels =
        (newLevels == null || newLevels.isEmpty())
            ? Collections.emptyList()
            : Collections.unmodifiableList(new ArrayList<>(newLevels));
  }

  public String id;
  public String name;
  public final List<String> nextThemes = new ArrayList<>();
  public final List<LevelTask> tasks = new ArrayList<>();

  @SuppressFBWarnings(
      value = "PA_PUBLIC_PRIMITIVE_ATTRIBUTE",
      justification =
          "Public field as part of the persistence schema (JSON/YAML serialization). "
              + "This WeightedPool is managed by the plugin and needs to be accessible for "
              + "reflection-based serialization. Access is controlled by the class design.")
  public WeightedPool<PoolEntry> blockPool = new WeightedPool<>();

  @SuppressFBWarnings(
      value = "PA_PUBLIC_PRIMITIVE_ATTRIBUTE",
      justification =
          "Public field as part of the persistence schema (JSON/YAML serialization). "
              + "This WeightedPool is managed by the plugin and needs to be accessible for "
              + "reflection-based serialization. Access is controlled by the class design.")
  public WeightedPool<EntityType> mobPool = new WeightedPool<>();

  public BarColor color;
  public BarStyle style;
  public int length = 100;

  /**
   * Parsed boolean expression that decides when this level is complete. Defaults to {@code ANY}
   * (i.e. one satisfied group is enough) so that levels created before the expression DSL behave
   * like the old Phase-2 hard-coded OR logic.
   */
  public CompletionExpr completionExpr;

  public Level(String name) {
    this(null, name);
  }

  public Level(String id, String name) {
    this.id = id;
    this.name = name;
  }

  public int getId() {
    List<Level> snapshot = levels;
    for (int i = 0; i < snapshot.size(); i++) if (snapshot.get(i) == this) return i;
    return 1;
  }

  public int blockPoolSize() {
    return blockPool.size();
  }

  public int mobPoolSize() {
    return mobPool.size();
  }

  public void resetPools() {
    blockPool = new WeightedPool<>();
    mobPool = new WeightedPool<>();
  }
}
