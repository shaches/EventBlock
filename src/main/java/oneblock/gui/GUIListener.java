package oneblock.gui;

import java.util.ArrayList;
import java.util.List;
import oneblock.ChestItems;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

public class GUIListener implements Listener {

  @EventHandler
  public void onInventoryClose(final InventoryCloseEvent e) {
    Inventory inv = e.getInventory();
    InventoryHolder holder = inv.getHolder();
    if (!(holder instanceof ChestHolder)) return;
    ChestHolder chestHolder = (ChestHolder) holder;
    String chestType = chestHolder.getType();
    List<ItemStack> items = new ArrayList<>();
    for (ItemStack item : inv.getContents()) {
      if (item != null) items.add(item);
    }
    ChestItems.setItems(chestType, items);
    ChestItems.save();
  }
}
