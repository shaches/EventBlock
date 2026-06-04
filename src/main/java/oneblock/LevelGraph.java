package oneblock;

import java.util.Collections;
import java.util.List;

/**
 * Lightweight read-only view of the level progression graph built from {@link Level#nextThemes}.
 *
 * <p>All lookups delegate to the current {@link LevelRegistry#snapshot()} so they are consistent
 * with the last published config reload.
 */
public final class LevelGraph {

  private LevelGraph() {}

  /**
   * Return the outgoing edges (next theme ids) for a given level.
   *
   * @param levelId the source level id
   * @return unmodifiable list of destination level ids; empty if leaf / unknown
   */
  public static List<String> getOutgoing(String levelId) {
    Level level = LevelRegistry.get(levelId);
    if (level == null || level.nextThemes == null || level.nextThemes.isEmpty()) {
      return Collections.emptyList();
    }
    return Collections.unmodifiableList(level.nextThemes);
  }

  /** Whether the given level id has any outgoing edges (i.e. is not a leaf / max node). */
  public static boolean hasChoices(String levelId) {
    return !getOutgoing(levelId).isEmpty();
  }

  /** Whether the given destination is a direct successor of the source level. */
  public static boolean isSuccessor(String sourceId, String destinationId) {
    return getOutgoing(sourceId).contains(destinationId);
  }
}
