package oneblock.events;

import java.util.UUID;
import oneblock.Level;
import oneblock.LevelRegistry;
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
import org.bukkit.event.player.PlayerFishEvent;
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
    ItemStack result = e.getRecipe().getResult();
    if (result == null || result.getType() == Material.AIR) return;
    processTask(pl, TaskType.CRAFT, result.getType().name());
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
    if (player == null || targetName == null) return;
    UUID uuid = player.getUniqueId();
    int plID = PlayerInfo.getId(uuid);
    if (plID == -1) return;

    PlayerInfo inf = PlayerInfo.get(plID);
    if (inf == null || inf.waitingForThemeSelection) return;

    Level level = LevelRegistry.get(inf.currentLevelId);
    if (level == null || level == Level.max) return;
    if (level.tasks == null || level.tasks.isEmpty()) return;

    boolean incremented = false;
    for (LevelTask task : level.tasks) {
      if (task.type != eventType) continue;
      if (targetName.equalsIgnoreCase(task.target)) {
        inf.taskProgress.increment(task.id);
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
