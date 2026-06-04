package oneblock.gui;

import java.util.List;
import oneblock.Level;
import oneblock.LevelGraph;
import oneblock.LevelRegistry;
import oneblock.Oneblock;
import oneblock.PlayerInfo;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * Phase 2 theme-selection GUI. Shown when a player completes all task groups in their current
 * level. Displays the outgoing edges ({@code next_themes}) of the current level as clickable items;
 * choosing one advances the player to that level and resets task progress.
 */
public final class LevelSelectGUI {

  private LevelSelectGUI() {}

  /**
   * Open the theme-selection GUI for the player if their current level has outgoing edges. If there
   * is exactly one choice, auto-advance to it without showing the GUI.
   */
  public static void openIfAvailable(Player player, PlayerInfo inf) {
    if (player == null || inf == null) return;
    List<String> choices = LevelGraph.getOutgoing(inf.currentLevelId);
    if (choices.isEmpty()) {
      // No outgoing edges — treat as leaf / max.
      inf.waitingForThemeSelection = false;
      return;
    }
    if (choices.size() == 1) {
      String levelId = choices.get(0);
      Level next = inf.advanceToLevel(levelId);
      if (next != null) {
        Oneblock.configManager.reward.executeAdvanceReward(player, levelId, next.name);
      }
      return;
    }
    open(player, choices);
  }

  public static void open(Player player, List<String> choices) {
    if (player == null || choices == null || choices.isEmpty()) return;
    int rows = Math.min(6, Math.max(1, (choices.size() + 8) / 9));
    Inventory inv =
        Bukkit.createInventory(
            new GUIHolder(GUIHolder.GUIType.LEVEL_SELECT),
            rows * 9,
            ChatColor.DARK_PURPLE + "Select Next Theme");

    for (int i = 0; i < choices.size(); i++) {
      String levelId = choices.get(i);
      Level level = LevelRegistry.get(levelId);
      String displayName =
          (level != null && level.name != null)
              ? ChatColor.GREEN + level.name
              : ChatColor.GREEN + levelId;
      ItemStack item = new ItemStack(Material.GRASS_BLOCK);
      ItemMeta meta = item.getItemMeta();
      if (meta != null) {
        meta.setDisplayName(displayName);
        if (level != null && level.id != null) {
          meta.setLore(java.util.Collections.singletonList(ChatColor.GRAY + "ID: " + level.id));
        }
        item.setItemMeta(meta);
      }
      inv.setItem(i, item);
    }
    player.openInventory(inv);
  }
}
