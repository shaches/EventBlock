package oneblock;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link LevelGraph}. */
class LevelGraphTest {

  private LevelRegistry.Snapshot savedSnapshot;

  @BeforeEach
  void setUp() {
    savedSnapshot = LevelRegistry.snapshot();
    LevelRegistry.replaceAll(new ArrayList<>());
  }

  @AfterEach
  void tearDown() {
    LevelRegistry.replaceAll(savedSnapshot.allOrdered());
  }

  @Test
  @DisplayName("getOutgoing returns empty list for null level")
  void getOutgoingNullLevel() {
    assertThat(LevelGraph.getOutgoing("nonexistent")).isEmpty();
  }

  @Test
  @DisplayName("getOutgoing returns empty list for level with empty nextThemes")
  void getOutgoingEmptyNextThemes() {
    Level level = new Level("test_level", "Test Level");
    level.nextThemes.clear();
    LevelRegistry.replaceAll(List.of(level));

    assertThat(LevelGraph.getOutgoing("test_level")).isEmpty();
  }

  @Test
  @DisplayName("getOutgoing returns unmodifiable list of nextThemes")
  void getOutgoingReturnsUnmodifiableList() {
    Level level = new Level("test_level", "Test Level");
    level.nextThemes.clear();
    level.nextThemes.add("next_level_1");
    level.nextThemes.add("next_level_2");
    LevelRegistry.replaceAll(List.of(level));

    List<String> outgoing = LevelGraph.getOutgoing("test_level");
    assertThat(outgoing).containsExactly("next_level_1", "next_level_2");

    // Should throw if caller tries to mutate
    org.assertj.core.api.Assertions.assertThatThrownBy(() -> outgoing.add("new"))
        .isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  @DisplayName("getOutgoing returns unmodifiable view (live reference)")
  void getOutgoingReturnsUnmodifiableView() {
    Level level = new Level("test_level", "Test Level");
    level.nextThemes.clear();
    level.nextThemes.add("next_level");
    LevelRegistry.replaceAll(List.of(level));

    List<String> outgoing = LevelGraph.getOutgoing("test_level");
    assertThat(outgoing).hasSize(1);

    // Modifying the underlying list is reflected in the unmodifiable view
    level.nextThemes.add("another_next");
    assertThat(outgoing).hasSize(2);
  }

  @Test
  @DisplayName("hasChoices returns false for null level")
  void hasChoicesNullLevel() {
    assertThat(LevelGraph.hasChoices("nonexistent")).isFalse();
  }

  @Test
  @DisplayName("hasChoices returns false for level with empty nextThemes")
  void hasChoicesEmptyNextThemes() {
    Level level = new Level("test_level", "Test Level");
    level.nextThemes.clear();
    LevelRegistry.replaceAll(List.of(level));

    assertThat(LevelGraph.hasChoices("test_level")).isFalse();
  }

  @Test
  @DisplayName("hasChoices returns true for level with outgoing edges")
  void hasChoicesWithOutgoing() {
    Level level = new Level("test_level", "Test Level");
    level.nextThemes.clear();
    level.nextThemes.add("next_level");
    LevelRegistry.replaceAll(List.of(level));

    assertThat(LevelGraph.hasChoices("test_level")).isTrue();
  }

  @Test
  @DisplayName("isSuccessor returns false for null source")
  void isSuccessorNullSource() {
    assertThat(LevelGraph.isSuccessor(null, "dest")).isFalse();
  }

  @Test
  @DisplayName("isSuccessor returns false for null destination")
  void isSuccessorNullDestination() {
    assertThat(LevelGraph.isSuccessor("source", null)).isFalse();
  }

  @Test
  @DisplayName("isSuccessor returns false for null source level")
  void isSuccessorNullSourceLevel() {
    assertThat(LevelGraph.isSuccessor("nonexistent", "dest")).isFalse();
  }

  @Test
  @DisplayName("isSuccessor returns true when destination is in source's nextThemes")
  void isSuccessorReturnsTrueWhenInNextThemes() {
    Level level = new Level("test_level", "Test Level");
    level.nextThemes.clear();
    level.nextThemes.add("next_level_1");
    level.nextThemes.add("next_level_2");
    LevelRegistry.replaceAll(List.of(level));

    assertThat(LevelGraph.isSuccessor("test_level", "next_level_1")).isTrue();
    assertThat(LevelGraph.isSuccessor("test_level", "next_level_2")).isTrue();
  }

  @Test
  @DisplayName("isSuccessor returns false when destination not in source's nextThemes")
  void isSuccessorReturnsFalseWhenNotInNextThemes() {
    Level level = new Level("test_level", "Test Level");
    level.nextThemes.clear();
    level.nextThemes.add("next_level_1");
    LevelRegistry.replaceAll(List.of(level));

    assertThat(LevelGraph.isSuccessor("test_level", "other_level")).isFalse();
  }

  @Test
  @DisplayName("isSuccessor returns false for level with empty nextThemes")
  void isSuccessorEmptyNextThemes() {
    Level level = new Level("test_level", "Test Level");
    level.nextThemes.clear();
    LevelRegistry.replaceAll(List.of(level));

    assertThat(LevelGraph.isSuccessor("test_level", "any")).isFalse();
  }
}
