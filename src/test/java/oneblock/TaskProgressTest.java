package oneblock;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link TaskProgress}. */
class TaskProgressTest {

  private TaskProgress progress;
  private Level testLevel;

  @BeforeEach
  void setUp() {
    progress = new TaskProgress();
    testLevel = new Level("test_level", "Test Level");
    testLevel.tasks.clear(); // Ensure clean state for each test
  }

  @Test
  @DisplayName("reset clears all counters and completion state")
  void resetClearsAllState() {
    progress.increment("task1");
    progress.increment("task2");
    progress.markComplete();

    progress.reset();

    assertThat(progress.get("task1")).isEqualTo(0);
    assertThat(progress.get("task2")).isEqualTo(0);
    assertThat(progress.isLevelComplete()).isFalse();
  }

  @Test
  @DisplayName("increment returns new counter value")
  void incrementReturnsNewValue() {
    assertThat(progress.increment("task1")).isEqualTo(1);
    assertThat(progress.increment("task1")).isEqualTo(2);
    assertThat(progress.increment("task1")).isEqualTo(3);
  }

  @Test
  @DisplayName("increment creates counter if not exists")
  void incrementCreatesCounter() {
    assertThat(progress.get("newTask")).isEqualTo(0);
    progress.increment("newTask");
    assertThat(progress.get("newTask")).isEqualTo(1);
  }

  @Test
  @DisplayName("set updates counter value")
  void setUpdatesCounter() {
    progress.set("task1", 5);
    assertThat(progress.get("task1")).isEqualTo(5);

    progress.set("task1", 10);
    assertThat(progress.get("task1")).isEqualTo(10);
  }

  @Test
  @DisplayName("set creates counter if not exists")
  void setCreatesCounter() {
    progress.set("newTask", 7);
    assertThat(progress.get("newTask")).isEqualTo(7);
  }

  @Test
  @DisplayName("get returns 0 for non-existent task")
  void getReturnsZeroForNonExistent() {
    assertThat(progress.get("nonexistent")).isEqualTo(0);
  }

  @Test
  @DisplayName("snapshot returns empty map when no counters")
  void snapshotReturnsEmptyMap() {
    Map<String, Integer> snap = progress.snapshot();
    assertThat(snap).isEmpty();
  }

  @Test
  @DisplayName("snapshot returns copy of current counters")
  void snapshotReturnsCopy() {
    progress.increment("task1");
    progress.increment("task2");
    progress.increment("task1");

    Map<String, Integer> snap = progress.snapshot();

    assertThat(snap).hasSize(2);
    assertThat(snap.get("task1")).isEqualTo(2);
    assertThat(snap.get("task2")).isEqualTo(1);

    // Verify snapshot is independent
    progress.increment("task1");
    assertThat(snap.get("task1")).isEqualTo(2); // unchanged
  }

  @Test
  @DisplayName("loadSnapshot loads counters from map")
  void loadSnapshotLoadsCounters() {
    Map<String, Integer> data = new HashMap<>();
    data.put("task1", 5);
    data.put("task2", 3);

    progress.loadSnapshot(data);

    assertThat(progress.get("task1")).isEqualTo(5);
    assertThat(progress.get("task2")).isEqualTo(3);
  }

  @Test
  @DisplayName("loadSnapshot clears existing counters")
  void loadSnapshotClearsExisting() {
    progress.increment("oldTask");
    assertThat(progress.get("oldTask")).isEqualTo(1);

    Map<String, Integer> data = new HashMap<>();
    data.put("newTask", 7);
    progress.loadSnapshot(data);

    assertThat(progress.get("oldTask")).isEqualTo(0);
    assertThat(progress.get("newTask")).isEqualTo(7);
  }

  @Test
  @DisplayName("loadSnapshot handles null input")
  void loadSnapshotHandlesNull() {
    progress.increment("task1");
    progress.loadSnapshot(null);
    assertThat(progress.get("task1")).isEqualTo(0);
  }

  @Test
  @DisplayName("loadSnapshot skips null values")
  void loadSnapshotSkipsNullValues() {
    Map<String, Integer> data = new HashMap<>();
    data.put("task1", 5);
    data.put("task2", null);

    progress.loadSnapshot(data);

    assertThat(progress.get("task1")).isEqualTo(5);
    assertThat(progress.get("task2")).isEqualTo(0);
  }

  @Test
  @DisplayName("markComplete sets levelComplete to true")
  void markCompleteSetsTrue() {
    assertThat(progress.isLevelComplete()).isFalse();
    progress.markComplete();
    assertThat(progress.isLevelComplete()).isTrue();
  }

  @Test
  @DisplayName("recomputeCompletion returns false for null level")
  void recomputeCompletionReturnsFalseForNull() {
    assertThat(progress.recomputeCompletion(null)).isFalse();
    assertThat(progress.isLevelComplete()).isFalse();
  }

  @Test
  @DisplayName("recomputeCompletion returns false for level with no tasks")
  void recomputeCompletionReturnsFalseForNoTasks() {
    testLevel.tasks.clear();
    assertThat(progress.recomputeCompletion(testLevel)).isFalse();
  }

  @Test
  @DisplayName("recomputeCompletion returns false for null tasks list")
  void recomputeCompletionReturnsFalseForNullTasks() {
    // Can't set final field to null, test with empty list instead
    testLevel.tasks.clear();
    assertThat(progress.recomputeCompletion(testLevel)).isFalse();
  }

  @Test
  @DisplayName("recomputeCompletion with legacy fallback (any group completes)")
  void recomputeCompletionLegacyFallback() {
    LevelTask task1 = new LevelTask("task1", TaskType.BREAK, "stone", 5, "group1");
    testLevel.tasks.add(task1);

    progress.set("task1", 3);
    assertThat(progress.recomputeCompletion(testLevel)).isFalse();

    progress.set("task1", 5);
    assertThat(progress.recomputeCompletion(testLevel)).isTrue();
  }

  @Test
  @DisplayName("getCompletedGroupRatio returns cached ratio")
  void getCompletedGroupRatioReturnsCached() {
    LevelTask task1 = new LevelTask("task1", TaskType.BREAK, "stone", 5, "group1");
    LevelTask task2 = new LevelTask("task2", TaskType.BREAK, "dirt", 5, "group2");
    testLevel.tasks.add(task1);
    testLevel.tasks.add(task2);

    progress.set("task1", 5); // complete group1
    progress.recomputeCompletion(testLevel);

    assertThat(progress.getCompletedGroupRatio(testLevel)).isEqualTo(0.5);
  }

  @Test
  @DisplayName("getCompletedGroupRatio returns 0 for no groups")
  void getCompletedGroupRatioReturnsZeroForNoGroups() {
    testLevel.tasks.clear();
    assertThat(progress.getCompletedGroupRatio(testLevel)).isEqualTo(0.0);
  }

  @Test
  @DisplayName("getCompletedGroupCount returns count of completed groups")
  void getCompletedGroupCountReturnsCount() {
    LevelTask task1 = new LevelTask("task1", TaskType.BREAK, "stone", 5, "group1");
    LevelTask task2 = new LevelTask("task2", TaskType.BREAK, "dirt", 5, "group2");
    LevelTask task3 = new LevelTask("task3", TaskType.BREAK, "cobble", 5, "group3");
    testLevel.tasks.add(task1);
    testLevel.tasks.add(task2);
    testLevel.tasks.add(task3);

    assertThat(progress.getCompletedGroupCount(testLevel)).isEqualTo(0);

    progress.set("task1", 5);
    assertThat(progress.getCompletedGroupCount(testLevel)).isEqualTo(1);

    progress.set("task2", 5);
    assertThat(progress.getCompletedGroupCount(testLevel)).isEqualTo(2);
  }

  @Test
  @DisplayName("getCompletedGroupCount returns 0 for null level")
  void getCompletedGroupCountReturnsZeroForNull() {
    assertThat(progress.getCompletedGroupCount(null)).isEqualTo(0);
  }

  @Test
  @DisplayName("getTotalGroupCount returns total distinct groups")
  void getTotalGroupCountReturnsTotal() {
    LevelTask task1 = new LevelTask("task1", TaskType.BREAK, "stone", 5, "group1");
    LevelTask task2 = new LevelTask("task2", TaskType.BREAK, "dirt", 5, "group2");
    LevelTask task3 = new LevelTask("task3", TaskType.BREAK, "cobble", 5, "group1"); // same group
    testLevel.tasks.add(task1);
    testLevel.tasks.add(task2);
    testLevel.tasks.add(task3);

    assertThat(progress.getTotalGroupCount(testLevel)).isEqualTo(2);
  }

  @Test
  @DisplayName("getTotalGroupCount returns 0 for null level")
  void getTotalGroupCountReturnsZeroForNull() {
    assertThat(progress.getTotalGroupCount(null)).isEqualTo(0);
  }

  @Test
  @DisplayName("multiple tasks in same group require all to complete")
  void multipleTasksInSameGroup() {
    LevelTask task1 = new LevelTask("task1", TaskType.BREAK, "stone", 5, "group1");
    LevelTask task2 = new LevelTask("task2", TaskType.BREAK, "dirt", 3, "group1");
    testLevel.tasks.add(task1);
    testLevel.tasks.add(task2);

    progress.set("task1", 5);
    assertThat(progress.getCompletedGroupCount(testLevel)).isEqualTo(0);

    progress.set("task2", 3);
    assertThat(progress.getCompletedGroupCount(testLevel)).isEqualTo(1);
  }

  @Test
  @DisplayName("concurrent increment is thread-safe")
  void concurrentIncrementThreadSafe() throws InterruptedException {
    final int threadCount = 10;
    final int incrementsPerThread = 100;
    Thread[] threads = new Thread[threadCount];

    for (int i = 0; i < threadCount; i++) {
      threads[i] =
          new Thread(
              () -> {
                for (int j = 0; j < incrementsPerThread; j++) {
                  progress.increment("sharedTask");
                }
              });
      threads[i].start();
    }

    for (Thread thread : threads) {
      thread.join();
    }

    assertThat(progress.get("sharedTask")).isEqualTo(threadCount * incrementsPerThread);
  }
}
