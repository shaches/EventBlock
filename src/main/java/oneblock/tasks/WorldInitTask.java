package oneblock.tasks;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import oneblock.Oneblock;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.scheduler.BukkitTask;

/**
 * Async-scheduled poll that resolves the configured island {@code world} once Bukkit has finished
 * loading worlds. On success it folds the resolved {@link World} into {@link Oneblock#origin()}
 * (preserving the already-loaded {@code x/y/z/offset}), kicks off the four steady-state runners via
 * {@link Oneblock#runMainTask()} and triggers a {@link Oneblock#reload()} so all dependent caches
 * see the new world.
 *
 * <p>Renamed from the inner class {@code Oneblock.Initialization} in Phase 3.4. The class is no
 * longer self-cancelling - {@code runMainTask} cancels the existing task pool, which transitively
 * cancels this one via the standard Bukkit scheduler API.
 */
public final class WorldInitTask implements Runnable {
  private final Oneblock plugin;
  private BukkitTask task;

  @SuppressFBWarnings(
      value = "EI_EXPOSE_REP2",
      justification =
          "Intentional dependency injection pattern. The plugin reference is stored "
              + "for use in the run() method. The task is a final class with controlled lifecycle, "
              + "and the plugin reference is never modified after construction.")
  public WorldInitTask(Oneblock plugin) {
    this.plugin = plugin;
  }

  public void setTask(BukkitTask task) {
    this.task = task;
  }

  @Override
  @SuppressFBWarnings(
      value = "ST_WRITE_TO_STATIC_FROM_INSTANCE_METHOD",
      justification =
          "Initialization task that intentionally sets global plugin state. The static"
              + " Oneblock.leavewor field is part of the plugin's global configuration that needs"
              + " to be set during plugin initialization. This task runs once during startup with"
              + " controlled lifecycle.")
  public void run() {
    if (Oneblock.getWorld() != null) return;
    String worldName = Oneblock.config.getString("world");
    String leaveWorldName = Oneblock.config.getString("leaveworld");
    final World w = worldName != null ? Bukkit.getWorld(worldName) : null;
    World leaveWorld = leaveWorldName != null ? Bukkit.getWorld(leaveWorldName) : null;
    if (leaveWorld != null) Oneblock.leavewor = leaveWorld;
    if (w != null) {
      // Atomic swap: fold the freshly-resolved world into ORIGIN while
      // preserving the existing x/y/z/offset loaded earlier by
      // ConfigManager.loadMainConfig(). Runs on the async scheduler thread.
      plugin.updateOriginWorld(w);
      plugin.getLogger().info("The initialization of the world was successful!");
      if (task != null) task.cancel();
      plugin.runMainTask();
      plugin.reload();
    } else {
      plugin
          .getLogger()
          .info(
              "Waiting for initialization of world '"
                  + Oneblock.config.getString("world")
                  + "'...");
    }
  }
}
