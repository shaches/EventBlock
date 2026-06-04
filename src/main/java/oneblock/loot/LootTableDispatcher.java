package oneblock.loot;

import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.loot.LootTable;

/**
 * Places a vanilla chest at the given block and populates it with the contents of a {@link
 * LootTable} referenced by {@link NamespacedKey}.
 */
public class LootTableDispatcher {
  public static void populate(Block block, NamespacedKey key, java.util.Random rnd) {
    if (block.getState() instanceof Chest) {
      Chest chest = (Chest) block.getState();
      LootTable lootTable = Bukkit.getLootTable(key);
      chest.setLootTable(lootTable);
      chest.update();
    }
  }
}
