package oneblock.events;

import java.util.UUID;
import oneblock.Level;
import oneblock.LevelTask;
import oneblock.Oneblock;
import oneblock.PlayerInfo;
import oneblock.TaskType;
import oneblock.gui.LevelSelectGUI;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.ItemStack;

/**
 * Event listener that increments {@link PlayerInfo#taskProgress} counters for active task types
 * ({@code KILL}, {@code CRAFT}, {@code PLACE}, {@code FISH}).
 *
 * <p>{@code BREAK} task increments are handled inside {@link Oneblock#updatePlayerProgression}
 * because they share the hot block- generation path.
 *
 * <p>Whenever a task counter pushes a group over its threshold, level completion is recomputed and
 * (if newly complete) the player is prompted with the theme-selection GUI.
 */
public class TaskEventListener implements Listener {

  @EventHandler
  public void onEntityDeath(EntityDeathEvent e) {
    if (e.getEntity().getKiller() == null) return;
    Player killer = e.getEntity().getKiller();
    EntityType type = e.getEntityType();
    processTask(killer, TaskType.KILL, type.name());
  }

  @EventHandler
  public void onCraftItem(CraftItemEvent e) {
    if (!(e.getWhoClicked() instanceof Player)) return;
    Player pl = (Player) e.getWhoClicked();

    InventoryAction action = e.getAction();

    // Creative clone does not consume ingredients.
    if (action == InventoryAction.NOTHING || action == InventoryAction.CLONE_STACK) {
      return;
    }

    ItemStack result = e.getRecipe().getResult();
    if (result == null || result.getType() == Material.AIR) return;

    int perCraft = result.getAmount();
    int amount;

    if (action == InventoryAction.MOVE_TO_OTHER_INVENTORY
        || action == InventoryAction.DROP_ALL_SLOT) {
      // Shift-click and Ctrl+Q on result slot craft the maximum possible.
      CraftingInventory inv = e.getInventory();
      if (inv != null) {
        ItemStack[] matrix = inv.getMatrix();
        int maxCrafts = Integer.MAX_VALUE;
        for (ItemStack item : matrix) {
          if (item != null && item.getType() != Material.AIR) {
            maxCrafts = Math.min(maxCrafts, item.getAmount());
          }
        }
        if (maxCrafts != Integer.MAX_VALUE && maxCrafts > 0) {
          amount = maxCrafts * perCraft;
        } else {
          amount = perCraft;
        }
      } else {
        amount = perCraft;
      }
    } else {
      // Single craft: PICKUP_*, HOTBAR_*, SWAP_WITH_CURSOR, DROP_ONE_SLOT.
      // DROP_ONE_SLOT (Q on result) crafts exactly one batch and drops it.
      amount = perCraft;
    }

    processTask(pl, TaskType.CRAFT, result.getType().name(), amount);
  }

  @EventHandler
  public void onBlockPlace(BlockPlaceEvent e) {
    Player pl = e.getPlayer();
    processTask(pl, TaskType.PLACE, e.getBlock().getType().name());
  }

  @EventHandler
  public void onPlayerFish(PlayerFishEvent e) {
    if (e.getState() != PlayerFishEvent.State.CAUGHT_FISH) return;
    Player pl = e.getPlayer();
    // Fish tasks target generic "fish" or specific item/entity names
    processTask(pl, TaskType.FISH, "FISH");
    org.bukkit.entity.Entity caught = e.getCaught();
    if (caught != null) {
      processTask(pl, TaskType.FISH, caught.getType().name());
    }
  }

  /* ---- shared helper ---- */

  private void processTask(Player player, TaskType eventType, String targetName) {
    processTask(player, eventType, targetName, 1);
  }

  private void processTask(Player player, TaskType eventType, String targetName, int amount) {
    if (player == null || targetName == null || amount <= 0) return;
    UUID uuid = player.getUniqueId();
    int plID = PlayerInfo.getId(uuid);
    if (plID == -1) return;

    PlayerInfo inf = PlayerInfo.get(plID);
    if (inf == null || inf.waitingForThemeSelection) return;

    Level level = inf.reconcileCurrentLevelId();
    if (level == null || level == Level.max) return;
    if (level.tasks == null || level.tasks.isEmpty()) return;

    boolean incremented = false;
    for (LevelTask task : level.tasks) {
      if (task.type != eventType) continue;
      if (targetName.equalsIgnoreCase(task.target)) {
        inf.taskProgress.increment(task.id, amount);
        incremented = true;
      }
    }

    if (!incremented) return;

    boolean wasComplete = inf.taskProgress.isLevelComplete();
    boolean nowComplete = inf.taskProgress.recomputeCompletion(level);
    if (nowComplete && !wasComplete) {
      inf.waitingForThemeSelection = true;
      if (Oneblock.settings().progressBar && inf.bar != null) {
        inf.bar.setTitle("Level Complete! Choose your next theme.");
        inf.bar.setProgress(1.0);
      }
      LevelSelectGUI.openIfAvailable(player, inf);
    }
  }
}
