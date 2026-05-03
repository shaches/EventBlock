package oneblock;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Regression tests for the dual-write consistency tightened in Phase-2 polish: {@link
 * PlayerInfo#syncLegacyFields()} must keep {@code lvl}/{@code breaks} in sync with {@code
 * currentLevelId}/{@code taskProgress} after every mutation.
 */
class DualWriteConsistencyTest {

  private static final UUID U = UUID.fromString("00000000-0000-0000-0000-000000000001");

  @BeforeEach
  void reset() {
    // Wipe the global LevelRegistry and PlayerInfo so tests are hermetic.
    LevelRegistry.replaceAll(null);
    PlayerInfo.replaceAll(Collections.emptyList());
  }

  private static Level makeLevel(String id, int index) {
    Level l = new Level(id, "Test: " + id);
    l.id = id;
    // Give the level a task so task-based progression is detected.
    l.tasks.add(new LevelTask(id + "_task", TaskType.BREAK, "STONE", 1, "default"));
    return l;
  }

  private static void publishLevels(Level... levels) {
    List<Level> list = new ArrayList<>();
    Collections.addAll(list, levels);
    Level.replaceAll(list); // legacy positional list
    LevelRegistry.replaceAll(list); // identity map + ordered list
  }

  @Test
  @DisplayName("syncLegacyFields maps a known currentLevelId to the correct lvl index")
  void knownIdSetsLvl() {
    publishLevels(makeLevel("forest", 0), makeLevel("desert", 1), makeLevel("ocean", 2));

    PlayerInfo inf = new PlayerInfo(U);
    inf.currentLevelId = "desert";
    inf.lvl = 999; // stale
    inf.syncLegacyFields();

    assertThat(inf.lvl).isEqualTo(1);
  }

  @Test
  @DisplayName("syncLegacyFields derives lvl from level_N convention when registry is empty")
  void conventionDerivedWhenRegistryEmpty() {
    publishLevels(makeLevel("forest", 0));

    PlayerInfo inf = new PlayerInfo(U);
    inf.currentLevelId = "level_7";
    inf.lvl = 5;
    inf.syncLegacyFields();

    assertThat(inf.lvl).isEqualTo(7);
  }

  @Test
  @DisplayName("syncLegacyFields leaves lvl untouched for malformed ids")
  void malformedIdPreservesLvl() {
    publishLevels(makeLevel("forest", 0));

    PlayerInfo inf = new PlayerInfo(U);
    inf.currentLevelId = "level_foo";
    inf.lvl = 3;
    inf.syncLegacyFields();

    assertThat(inf.lvl).isEqualTo(3);
  }

  @Test
  @DisplayName("syncLegacyFields zeros breaks when current level has tasks")
  void taskBasedZerosBreaks() {
    publishLevels(makeLevel("forest", 0));

    PlayerInfo inf = new PlayerInfo(U);
    inf.currentLevelId = "forest";
    inf.breaks = 42;
    inf.syncLegacyFields();

    assertThat(inf.breaks).isZero();
  }

  @Test
  @DisplayName("syncLegacyFields preserves breaks when current level has no tasks")
  void noTasksPreservesBreaks() {
    Level plain = new Level("plain", "Test: plain");
    plain.id = "plain";
    // no tasks
    publishLevels(plain);

    PlayerInfo inf = new PlayerInfo(U);
    inf.currentLevelId = "plain";
    inf.breaks = 77;
    inf.syncLegacyFields();

    assertThat(inf.breaks).isEqualTo(77);
  }

  @Test
  @DisplayName("advanceToLevel resets breaks and syncs lvl")
  void advanceToLevelSyncs() {
    publishLevels(makeLevel("forest", 0), makeLevel("desert", 1));

    PlayerInfo inf = new PlayerInfo(U);
    inf.currentLevelId = "forest";
    inf.lvl = 0;
    inf.breaks = 10;
    PlayerInfo.set(0, inf);

    inf.advanceToLevel("desert");

    assertThat(inf.currentLevelId).isEqualTo("desert");
    assertThat(inf.lvl).isEqualTo(1);
    assertThat(inf.breaks).isZero();
  }

  @Test
  @DisplayName("lvlup bumps currentLevelId, resets taskProgress, and syncs lvl")
  void lvlupSyncs() {
    publishLevels(makeLevel("forest", 0), makeLevel("desert", 1));

    PlayerInfo inf = new PlayerInfo(U);
    inf.currentLevelId = "forest";
    inf.lvl = 0;
    inf.breaks = 5;
    inf.taskProgress.increment("forest_task");
    PlayerInfo.set(0, inf);

    inf.lvlup();

    assertThat(inf.lvl).isEqualTo(1);
    assertThat(inf.currentLevelId).isEqualTo("desert");
    assertThat(inf.breaks).isZero();
    assertThat(inf.taskProgress.isLevelComplete()).isFalse();
  }
}
