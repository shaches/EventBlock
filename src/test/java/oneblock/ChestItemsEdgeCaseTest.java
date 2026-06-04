package oneblock;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.nio.file.Files;
import java.util.Collections;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Edge case coverage for {@link ChestItems} methods not covered in the dual-mode and parse-key
 * tests. Focuses on null safety, exception handling, and boundary conditions.
 */
class ChestItemsEdgeCaseTest {

  private File tempChests;

  @BeforeEach
  void reset() throws Exception {
    tempChests = Files.createTempFile("chests", ".yml").toFile();
    tempChests.deleteOnExit();
    ChestItems.chest = tempChests;
    ChestItems.load();
  }

  @Test
  @DisplayName("resolve returns null for null input")
  void resolveNullInput() {
    assertThat(ChestItems.resolve(null)).isNull();
  }

  @Test
  @DisplayName("resolve returns null for non-existent alias")
  void resolveNonExistent() {
    assertThat(ChestItems.resolve("nonexistent")).isNull();
  }

  @Test
  @DisplayName("getItems returns null for null input")
  void getItemsNullInput() {
    assertThat(ChestItems.getItems(null)).isNull();
  }

  @Test
  @DisplayName("getItems returns null for non-existent alias")
  void getItemsNonExistent() {
    assertThat(ChestItems.getItems("nonexistent")).isNull();
  }

  @Test
  @DisplayName("hasChest returns false for null input")
  void hasChestNullInput() {
    assertThat(ChestItems.hasChest(null)).isFalse();
  }

  @Test
  @DisplayName("hasChest returns false for non-existent alias")
  void hasChestNonExistent() {
    assertThat(ChestItems.hasChest("nonexistent")).isFalse();
  }

  @Test
  @DisplayName("setAlias with null name is a no-op")
  void setAliasNullName() {
    ChestItems.setAlias(null, NamespacedKey.minecraft("test"));
    assertThat(ChestItems.hasChest("test")).isFalse();
  }

  @Test
  @DisplayName("setAlias with null key is a no-op")
  void setAliasNullKey() {
    ChestItems.setAlias("test", null);
    assertThat(ChestItems.hasChest("test")).isFalse();
  }

  @Test
  @DisplayName("removeAlias returns false for non-existent alias")
  void removeAliasNonExistent() {
    assertThat(ChestItems.removeAlias("nonexistent")).isFalse();
  }

  @Test
  @DisplayName("removeAlias returns false for null input")
  void removeAliasNull() {
    assertThat(ChestItems.removeAlias(null)).isFalse();
  }

  @Test
  @DisplayName("load with null chest file is a no-op")
  void loadNullChest() {
    ChestItems.chest = null;
    ChestItems.setAlias("test", NamespacedKey.minecraft("chests/simple_dungeon"));
    ChestItems.load(); // Should clear and not throw
    assertThat(ChestItems.hasChest("test")).isFalse();
  }

  @Test
  @DisplayName("load with non-existent chest file is a no-op")
  void loadNonExistentFile() throws Exception {
    File nonExistent = new File(tempChests.getParentFile(), "does_not_exist.yml");
    ChestItems.chest = nonExistent;
    ChestItems.setAlias("test", NamespacedKey.minecraft("chests/simple_dungeon"));
    ChestItems.load(); // Should clear and not throw
    assertThat(ChestItems.hasChest("test")).isFalse();
  }

  @Test
  @DisplayName("save with null chest file handles gracefully")
  void saveNullChest() {
    ChestItems.chest = null;
    ChestItems.setAlias("test", NamespacedKey.minecraft("chests/simple_dungeon"));
    // Should not throw even though chest is null
    ChestItems.save();
  }

  @Test
  @DisplayName("load clears existing aliases before loading")
  void loadClearsExisting() throws Exception {
    ChestItems.setAlias("old", NamespacedKey.minecraft("chests/simple_dungeon"));
    ChestItems.setItems("old_items", Collections.singletonList(new ItemStack(Material.DIAMOND)));
    assertThat(ChestItems.hasChest("old")).isTrue();
    assertThat(ChestItems.hasChest("old_items")).isTrue();

    // Load empty file
    ChestItems.load();
    assertThat(ChestItems.hasChest("old")).isFalse();
    assertThat(ChestItems.hasChest("old_items")).isFalse();
  }

  @Test
  @DisplayName("getChestNames returns unmodifiable set")
  void getChestNamesUnmodifiable() {
    ChestItems.setAlias("test", NamespacedKey.minecraft("chests/simple_dungeon"));
    var names = ChestItems.getChestNames();
    org.assertj.core.api.Assertions.assertThatThrownBy(() -> names.add("new"))
        .isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  @DisplayName("getChestNames returns empty set when no aliases")
  void getChestNamesEmpty() {
    assertThat(ChestItems.getChestNames()).isEmpty();
  }

  @Test
  @DisplayName("getChestNames merges both alias types")
  void getChestNamesMergesTypes() {
    ChestItems.setAlias("loot", NamespacedKey.minecraft("chests/simple_dungeon"));
    ChestItems.setItems("items", Collections.singletonList(new ItemStack(Material.DIAMOND)));
    assertThat(ChestItems.getChestNames()).containsExactlyInAnyOrder("loot", "items");
  }

  @Test
  @DisplayName("setItems creates shallow copy of list (not deep copy of items)")
  void setItemsShallowCopy() {
    ItemStack item = new ItemStack(Material.DIAMOND);
    var originalList = Collections.singletonList(item);
    ChestItems.setItems("test", originalList);

    // Modifying the original list doesn't affect the stored list
    // (shallow copy of list, not deep copy of ItemStack objects)
    var loaded = ChestItems.getItems("test");
    assertThat(loaded).isNotSameAs(originalList);
    assertThat(loaded).hasSize(1);
  }

  @Test
  @DisplayName("setAlias overwrites existing alias")
  void setAliasOverwrites() {
    ChestItems.setAlias("test", NamespacedKey.minecraft("chests/simple_dungeon"));
    ChestItems.setAlias("test", NamespacedKey.minecraft("chests/abandoned_mineshaft"));
    assertThat(ChestItems.resolve("test"))
        .isEqualTo(NamespacedKey.minecraft("chests/abandoned_mineshaft"));
  }

  @Test
  @DisplayName("setItems overwrites existing items")
  void setItemsOverwrites() {
    ChestItems.setItems("test", Collections.singletonList(new ItemStack(Material.DIAMOND)));
    ChestItems.setItems("test", Collections.singletonList(new ItemStack(Material.GOLD_INGOT)));
    var loaded = ChestItems.getItems("test");
    assertThat(loaded).hasSize(1);
    assertThat(loaded.get(0).getType()).isEqualTo(Material.GOLD_INGOT);
  }

  @Test
  @DisplayName("removeAlias removes from loot table map")
  void removeAliasFromLootTable() {
    ChestItems.setAlias("test", NamespacedKey.minecraft("chests/simple_dungeon"));
    assertThat(ChestItems.removeAlias("test")).isTrue();
    assertThat(ChestItems.resolve("test")).isNull();
  }

  @Test
  @DisplayName("removeAlias removes from legacy items map")
  void removeAliasFromLegacy() {
    ChestItems.setItems("test", Collections.singletonList(new ItemStack(Material.DIAMOND)));
    assertThat(ChestItems.removeAlias("test")).isTrue();
    assertThat(ChestItems.getItems("test")).isNull();
  }

  @Test
  @DisplayName("removeAlias returns true when removing from either map")
  void removeAliasEitherMap() {
    ChestItems.setAlias("loot", NamespacedKey.minecraft("chests/simple_dungeon"));
    ChestItems.setItems("items", Collections.singletonList(new ItemStack(Material.DIAMOND)));
    assertThat(ChestItems.removeAlias("loot")).isTrue();
    assertThat(ChestItems.removeAlias("items")).isTrue();
  }
}
