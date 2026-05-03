package oneblock.migration;

import java.util.HashMap;
import java.util.Map;
import oneblock.LevelRegistry;
import oneblock.Oneblock;

/**
 * One-shot migration utility that maps legacy integer level indices to string level ids.
 *
 * <p>Resolution order:
 *
 * <ol>
 *   <li>Exact entry in the optional {@code legacy_mapping} section loaded from blocks.yml.
 *   <li>Auto-convention {@code "level_" + n} (e.g. 4 → "level_4").
 *   <li>If the resolved id does not exist in {@link LevelRegistry}, log a warning and pin the
 *       player at the first available starter level or "level_0".
 * </ol>
 */
public final class LegacyLevelMapper {

  private static final Map<Integer, String> CUSTOM_MAP = new HashMap<>();
  private static boolean initialized = false;

  public static void initialize(Map<?, ?> rawMapping) {
    CUSTOM_MAP.clear();
    if (rawMapping != null) {
      for (Map.Entry<?, ?> e : rawMapping.entrySet()) {
        Object key = e.getKey();
        if (e.getValue() instanceof String) {
          String val = (String) e.getValue();
          Integer intKey = null;
          if (key instanceof Number) {
            intKey = ((Number) key).intValue();
          } else if (key instanceof String) {
            try {
              intKey = Integer.parseInt((String) key);
            } catch (NumberFormatException ignored) {
            }
          }
          if (intKey != null) {
            CUSTOM_MAP.put(intKey, val);
          }
        }
      }
    }
    initialized = true;
  }

  public static boolean isInitialized() {
    return initialized;
  }

  /**
   * Convert a legacy integer level to a string id.
   *
   * @param legacyLevel the old integer level (e.g. 3)
   * @return a non-null level id guaranteed to exist in {@link LevelRegistry}
   */
  public static String fromInt(int legacyLevel) {
    String mapped = CUSTOM_MAP.get(legacyLevel);
    if (mapped != null) {
      if (LevelRegistry.contains(mapped)) {
        return mapped;
      }
      Oneblock.plugin
          .getLogger()
          .warning(
              "[Oneblock] Legacy mapping for level "
                  + legacyLevel
                  + " → '"
                  + mapped
                  + "' does not exist in the current blocks.yml; falling back to convention.");
    }

    String convention = "level_" + legacyLevel;
    if (LevelRegistry.contains(convention)) {
      return convention;
    }

    // Fallback: pin to the first registered level id, or trust the convention.
    LevelRegistry.Snapshot snap = LevelRegistry.snapshot();
    if (snap.size() > 0) {
      String fallback = snap.allOrdered().get(0).id;
      Oneblock.plugin
          .getLogger()
          .warning(
              "[Oneblock] Legacy level "
                  + legacyLevel
                  + " has no mapping in blocks.yml and convention '"
                  + convention
                  + "' does not exist either. Pinning to '"
                  + fallback
                  + "'.");
      return fallback;
    }
    // Registry not yet populated (e.g. early test / startup) — trust the convention.
    return convention;
  }
}
