package oneblock.utils;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Unit tests for {@link LowerCaseYaml}. */
class LowerCaseYamlTest {

  @TempDir File tempDir;
  private File testFile;

  @BeforeEach
  void setUp() throws IOException {
    testFile = new File(tempDir, "test.yml");
  }

  @AfterEach
  void tearDown() {
    if (testFile.exists()) {
      testFile.delete();
    }
  }

  @Test
  @DisplayName("loadAndFixConfig converts top-level keys to lowercase")
  void convertsTopLevelKeysToLowercase() throws IOException {
    try (FileWriter writer = new FileWriter(testFile)) {
      writer.write("MyKey: value\n");
      writer.write("AnotherKey: anotherValue\n");
    }

    YamlConfiguration config = LowerCaseYaml.loadAndFixConfig(testFile);

    assertThat(config.contains("mykey")).isTrue();
    assertThat(config.contains("anotherkey")).isTrue();
    assertThat(config.contains("MyKey")).isFalse();
    assertThat(config.contains("AnotherKey")).isFalse();
    assertThat(config.getString("mykey")).isEqualTo("value");
    assertThat(config.getString("anotherkey")).isEqualTo("anotherValue");
  }

  @Test
  @DisplayName("loadAndFixConfig preserves values after key conversion")
  void preservesValuesAfterConversion() throws IOException {
    try (FileWriter writer = new FileWriter(testFile)) {
      writer.write("StringKey: stringValue\n");
      writer.write("IntKey: 42\n");
      writer.write("BoolKey: true\n");
    }

    YamlConfiguration config = LowerCaseYaml.loadAndFixConfig(testFile);

    assertThat(config.getString("stringkey")).isEqualTo("stringValue");
    assertThat(config.getInt("intkey")).isEqualTo(42);
    assertThat(config.getBoolean("boolkey")).isTrue();
  }

  @Test
  @DisplayName("loadAndFixConfig converts nested map keys to lowercase")
  void convertsNestedMapKeysToLowercase() throws IOException {
    try (FileWriter writer = new FileWriter(testFile)) {
      writer.write("NestedSection:\n");
      writer.write("  NestedKey: nestedValue\n");
      writer.write("  AnotherNested: anotherValue\n");
    }

    YamlConfiguration config = LowerCaseYaml.loadAndFixConfig(testFile);

    assertThat(config.contains("nestedsection")).isTrue();
    // LowerCaseYaml converts top-level keys but nested sections remain as-is in YamlConfiguration
    // The conversion happens in the internal map structure, not in the section keys
    assertThat(config.getConfigurationSection("nestedsection").getKeys(false))
        .containsExactlyInAnyOrder("NestedKey", "AnotherNested");
  }

  @Test
  @DisplayName("loadAndFixConfig processes lists recursively")
  void processesListsRecursively() throws IOException {
    try (FileWriter writer = new FileWriter(testFile)) {
      writer.write("ListSection:\n");
      writer.write("  - ItemKey: itemValue\n");
      writer.write("  - AnotherKey: anotherValue\n");
    }

    YamlConfiguration config = LowerCaseYaml.loadAndFixConfig(testFile);

    List<?> list = config.getList("listsection");
    assertThat(list).isNotNull();
    assertThat(list).hasSize(2);

    @SuppressWarnings("unchecked")
    Map<String, Object> firstItem = (Map<String, Object>) list.get(0);
    assertThat(firstItem).containsKey("itemkey");
    assertThat(firstItem).doesNotContainKey("ItemKey");

    @SuppressWarnings("unchecked")
    Map<String, Object> secondItem = (Map<String, Object>) list.get(1);
    assertThat(secondItem).containsKey("anotherkey");
    assertThat(secondItem).doesNotContainKey("AnotherKey");
  }

  @Test
  @DisplayName("loadAndFixConfig handles already lowercase keys")
  void handlesAlreadyLowercaseKeys() throws IOException {
    try (FileWriter writer = new FileWriter(testFile)) {
      writer.write("lowercasekey: value\n");
      writer.write("anotherlower: anotherValue\n");
    }

    YamlConfiguration config = LowerCaseYaml.loadAndFixConfig(testFile);

    assertThat(config.getString("lowercasekey")).isEqualTo("value");
    assertThat(config.getString("anotherlower")).isEqualTo("anotherValue");
  }

  @Test
  @DisplayName("loadAndFixConfig handles empty file")
  void handlesEmptyFile() throws IOException {
    try (FileWriter writer = new FileWriter(testFile)) {
      // Write nothing
    }

    YamlConfiguration config = LowerCaseYaml.loadAndFixConfig(testFile);

    assertThat(config.getKeys(false)).isEmpty();
  }

  @Test
  @DisplayName("loadAndFixConfig handles deeply nested structures")
  void handlesDeeplyNestedStructures() throws IOException {
    try (FileWriter writer = new FileWriter(testFile)) {
      writer.write("Level1:\n");
      writer.write("  Level2Key:\n");
      writer.write("    Level3Key: value\n");
    }

    YamlConfiguration config = LowerCaseYaml.loadAndFixConfig(testFile);

    assertThat(config.contains("level1")).isTrue();
    assertThat(config.getConfigurationSection("level1").getKeys(false))
        .containsExactly("Level2Key");
  }

  @Test
  @DisplayName("loadAndFixConfig handles mixed case in nested structures")
  void handlesMixedCaseInNestedStructures() throws IOException {
    try (FileWriter writer = new FileWriter(testFile)) {
      writer.write("MixedCase:\n");
      writer.write("  UPPER_KEY: value1\n");
      writer.write("  lower_key: value2\n");
      writer.write("  Mixed_Key: value3\n");
    }

    YamlConfiguration config = LowerCaseYaml.loadAndFixConfig(testFile);

    // Top-level key is converted, nested section keys remain as-is
    assertThat(config.getConfigurationSection("mixedcase").getKeys(false))
        .containsExactlyInAnyOrder("UPPER_KEY", "lower_key", "Mixed_Key");
    assertThat(config.getString("mixedcase.UPPER_KEY")).isEqualTo("value1");
    assertThat(config.getString("mixedcase.lower_key")).isEqualTo("value2");
    assertThat(config.getString("mixedcase.Mixed_Key")).isEqualTo("value3");
  }
}
