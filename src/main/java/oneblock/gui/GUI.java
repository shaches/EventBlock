package oneblock.gui;

import java.util.List;
import oneblock.ChestItems;
import oneblock.worldguard.OBWorldGuard;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class GUI {
  public static class Config {
    public boolean enabled = true;
    public boolean legacy = false;
  }

  private static Config config = new Config();

  public static Config getConfig() {
    return config;
  }

  public static void setConfig(Config newConfig) {
    config = newConfig;
  }

  public static void openGUI(Player p) {
    DialogGUI.openMain(p);
  }

  public static void acceptGUI(Player p, String name) {
    DialogGUI.openInvite(p, name);
  }

  public static void topGUI(Player p) {
    DialogGUI.openTop(p);
  }

  public static void visitGUI(Player p, OfflinePlayer[] offlinePlayers) {
    DialogGUI.openVisit(p, offlinePlayers);
  }

  /**
   * Opens a 54-slot editable inventory for the given chest alias. Players can freely add/remove
   * items; on close the contents are persisted back to {@link ChestItems} via the {@link
   * ChestEditListener}.
   *
   * @param p the player editing the chest
   * @param chestType the chest alias name (must exist in {@link ChestItems#getChestNames()})
   */
  public static void chestGUI(Player p, String chestType) {
    if (p == null) return;
    List<ItemStack> list = ChestItems.getItems(chestType);
    if (list == null) return;
    Inventory chestGUI =
        Bukkit.createInventory(
            new ChestHolder(chestType),
            54,
            String.format(
                "%sEdit: %s%s %s",
                ChatColor.BLACK,
                ChatColor.DARK_GRAY,
                chestType,
                OBWorldGuard.canUse ? "" : "[Edit only in premium]"));
    for (ItemStack itm : list) {
      if (itm != null) chestGUI.addItem(itm);
    }
    p.openInventory(chestGUI);
  }
}
