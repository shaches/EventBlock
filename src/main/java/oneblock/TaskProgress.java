package oneblock;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Thread-safe live tracker for a player's task progress within their current level.
 *
 * <p>Internally stores per-task counters in a {@link ConcurrentHashMap} of {@link AtomicInteger}
 * values. A {@code volatile boolean} caches the overall level-completion state so that {@link
 * #isLevelComplete()} and {@link #getCompletedGroupRatio()} are O(1) for the common read paths
 * (BossBar, PlaceholderAPI, async save).
 *
 * <p>Whenever {@link #increment(String)} pushes a task group over its threshold, the
 * group-completion array is recomputed (O(groups), typically very small).
 */
public final class TaskProgress {

  private final ConcurrentHashMap<String, AtomicInteger> counters = new ConcurrentHashMap<>();
  private volatile boolean levelComplete = false;
  private volatile double cachedRatio = 0.0;

  /** Reset all counters and completion state (e.g. on level change). */
  public void reset() {
    counters.clear();
    levelComplete = false;
    cachedRatio = 0.0;
  }

  /**
   * Atomically increment the counter for the given task id.
   *
   * @return the new counter value after increment
   */
  public int increment(String taskId) {
    AtomicInteger ai = counters.computeIfAbsent(taskId, k -> new AtomicInteger(0));
    return ai.incrementAndGet();
  }

  /** Set the counter for a task id (used during deserialization / admin commands). */
  public void set(String taskId, int value) {
    counters.computeIfAbsent(taskId, k -> new AtomicInteger(0)).set(value);
  }

  /** Current value for a task, or 0 if never incremented. */
  public int get(String taskId) {
    AtomicInteger ai = counters.get(taskId);
    return ai == null ? 0 : ai.get();
  }

  /** Raw map for save/serialization. Values are stable at call time. */
  public Map<String, Integer> snapshot() {
    if (counters.isEmpty()) return Collections.emptyMap();
    ConcurrentHashMap<String, Integer> snap = new ConcurrentHashMap<>(counters.size());
    for (Map.Entry<String, AtomicInteger> e : counters.entrySet()) {
      snap.put(e.getKey(), e.getValue().get());
    }
    return snap;
  }

  /** Load a snapshot back into this tracker (used during deserialization). */
  public void loadSnapshot(Map<String, Integer> data) {
    counters.clear();
    if (data == null) return;
    for (Map.Entry<String, Integer> e : data.entrySet()) {
      if (e.getValue() != null) {
        counters.put(e.getKey(), new AtomicInteger(e.getValue()));
      }
    }
  }

  /** Mark the level as complete (e.g. when admin forces progression). */
  public void markComplete() {
    levelComplete = true;
  }

  public boolean isLevelComplete() {
    return levelComplete;
  }

  /**
   * Recompute level completion against the given level's tasks. This is called once after config
   * load / level change and whenever a task counter is incremented.
   *
   * @return true if the level is now complete
   */
  public boolean recomputeCompletion(Level level) {
    if (level == null || level.tasks == null || level.tasks.isEmpty()) {
      levelComplete = false;
      cachedRatio = 0.0;
      return false;
    }
    Set<String> groups = collectGroups(level.tasks);
    int completed = 0;
    for (String group : groups) {
      if (isGroupComplete(group, level.tasks)) completed++;
    }
    cachedRatio = groups.isEmpty() ? 0.0 : (double) completed / groups.size();
    if (level.completionExpr != null) {
      levelComplete = level.completionExpr.evaluate(completedGroups(level.tasks));
    } else {
      // Legacy fallback: ANY group completes (matches old Phase-2 behaviour)
      levelComplete = completed > 0;
    }
    return levelComplete;
  }

  /**
   * Return the cached ratio of completed groups to total groups. Updated during {@link
   * #recomputeCompletion(Level)} for O(1) reads.
   *
   * @return 0.0 .. 1.0
   */
  public double getCompletedGroupRatio(Level level) {
    return cachedRatio;
  }

  /** Number of groups that are fully complete in the given level. */
  public int getCompletedGroupCount(Level level) {
    if (level == null || level.tasks == null || level.tasks.isEmpty()) return 0;
    int completed = 0;
    for (String group : collectGroups(level.tasks)) {
      if (isGroupComplete(group, level.tasks)) completed++;
    }
    return completed;
  }

  /** Total number of distinct groups in the given level. */
  public int getTotalGroupCount(Level level) {
    if (level == null || level.tasks == null || level.tasks.isEmpty()) return 0;
    return collectGroups(level.tasks).size();
  }

  /* ---- internal helpers ---- */

  private boolean isGroupComplete(String group, List<LevelTask> tasks) {
    for (LevelTask task : tasks) {
      if (!group.equals(task.group)) continue;
      if (get(task.id) < task.amount) return false;
    }
    return true;
  }

  private java.util.Set<String> completedGroups(List<LevelTask> tasks) {
    java.util.Set<String> set = new java.util.HashSet<>();
    for (LevelTask task : tasks) {
      if (isGroupComplete(task.group, tasks)) set.add(task.group);
    }
    return set;
  }

  private static java.util.Set<String> collectGroups(List<LevelTask> tasks) {
    java.util.Set<String> set = new java.util.HashSet<>();
    for (LevelTask task : tasks) set.add(task.group);
    return set;
  }
}
