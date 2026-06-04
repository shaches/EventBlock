package oneblock;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Thread-safe registry that maps level string identifiers to {@link Level} instances.
 *
 * <p>Replaces the legacy {@code List<Level>} with an identity-lookup map while still maintaining a
 * positional list for callers that need integer indexing (backward compatibility with old {@link
 * Level#get(int)} consumers).
 *
 * <p>The internal state is published atomically via a {@code volatile} reference, mirroring the
 * Phase 4.1 publication pattern used by the old {@code List<Level>}.
 *
 * <p>Phase 6: Added level ID validation to prevent injection attacks.
 */
public final class LevelRegistry {

  private static volatile Snapshot snapshot =
      new Snapshot(Collections.emptyMap(), Collections.emptyList());

  // Allow-list pattern for level IDs to prevent injection attacks
  private static final Pattern LEVEL_ID_PATTERN = Pattern.compile("^[a-zA-Z0-9_]{1,64}$");

  /** Read-only snapshot of the registry at a point in time. */
  public static final class Snapshot {
    private final Map<String, Level> byId;
    private final List<Level> ordered;

    Snapshot(Map<String, Level> byId, List<Level> ordered) {
      this.byId = byId;
      this.ordered = ordered;
    }

    public Level byId(String id) {
      Level l = byId.get(id);
      return l == null ? Level.max() : l;
    }

    public boolean containsId(String id) {
      return byId.containsKey(id);
    }

    public Level byIndex(int i) {
      if (i < 0 || i >= ordered.size()) return Level.max();
      return ordered.get(i);
    }

    public int size() {
      return ordered.size();
    }

    @SuppressFBWarnings(
        value = "EI_EXPOSE_REP",
        justification =
            "The Snapshot class is designed to be immutable, with the ordered list set once in the"
                + " constructor. Returning the list directly is intentional for performance -"
                + " callers are expected to treat it as read-only. The list is only modified during"
                + " snapshot creation, and the snapshot itself is never modified after"
                + " construction.")
    public List<Level> allOrdered() {
      return ordered;
    }
  }

  /**
   * Atomically publish a new set of levels.
   *
   * @param levels list of levels; the order defines the legacy integer index.
   */
  public static void replaceAll(List<Level> levels) {
    Map<String, Level> map = new HashMap<>();
    List<Level> ordered = new ArrayList<>();
    if (levels != null) {
      for (Level l : levels) {
        if (l.id != null) {
          map.put(l.id, l);
        }
        ordered.add(l);
      }
    }
    snapshot =
        new Snapshot(
            Collections.unmodifiableMap(new HashMap<>(map)),
            Collections.unmodifiableList(new ArrayList<>(ordered)));
  }

  /** Current snapshot. Safe to cache locally for multiple reads. */
  public static Snapshot snapshot() {
    return snapshot;
  }

  /** Convenience: resolve a level by string id through the current snapshot. */
  public static Level get(String id) {
    return snapshot.byId(id);
  }

  /** Convenience: resolve a level by legacy integer index. */
  public static Level get(int i) {
    return snapshot.byIndex(i);
  }

  /** Number of registered levels. */
  public static int size() {
    return snapshot.size();
  }

  /** Whether the given string id is known in the registry. */
  public static boolean contains(String id) {
    return snapshot.containsId(id);
  }

  /** Return the ordered integer index of the given level id, or -1 if unknown. */
  public static int getIndex(String id) {
    Snapshot snap = snapshot;
    List<Level> ordered = snap.allOrdered();
    for (int i = 0; i < ordered.size(); i++) {
      if (id.equals(ordered.get(i).id)) return i;
    }
    return -1;
  }

  /**
   * Validate a level ID string. Returns true if the ID matches the allow-list pattern. Phase 6:
   * Security measure to prevent injection attacks via level IDs.
   */
  public static boolean isValidLevelId(String id) {
    return id != null && LEVEL_ID_PATTERN.matcher(id).matches();
  }
}
