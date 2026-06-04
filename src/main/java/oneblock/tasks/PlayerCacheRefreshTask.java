package oneblock.tasks;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import oneblock.Oneblock;
import org.bukkit.World;

/**
 * Async-scheduled refresh of {@link oneblock.PlayerCache} contents from the live online-player
 * snapshot of the island world. Runs every six seconds (120 ticks) so the slower per-tick block-gen
 * and particle runners can iterate the cache instead of re-querying Bukkit each time.
 *
 * <p>Renamed from the inner class {@code Oneblock.TaskUpdatePlayers} in Phase 3.4.
 */
public final class PlayerCacheRefreshTask implements Runnable {
  private final Oneblock plugin;

  @SuppressFBWarnings(
      value = "EI_EXPOSE_REP2",
      justification =
          "Intentional dependency injection pattern. The plugin reference is stored "
              + "for use in the run() method. The task is a final class with controlled lifecycle, "
              + "and the plugin reference is never modified after construction.")
  public PlayerCacheRefreshTask(Oneblock plugin) {
    this.plugin = plugin;
  }

  @Override
  public void run() {
    World w = Oneblock.getWorld();
    if (w != null) plugin.cache.updateCache(w.getPlayers());
  }
}
