package oneblock.tasks;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import oneblock.Oneblock;

/**
 * Async-scheduled persistence pulse: every five minutes (6000 ticks) it snapshots {@code
 * PlayerInfo.list} and writes the snapshot through either the configured database (HikariCP,
 * thread-safe) or the fallback {@link oneblock.storage.JsonPlayerDataStore}.
 *
 * <p>Renamed from the inner class {@code Oneblock.TaskSaveData} in Phase 3.4. The body is
 * intentionally one delegating call to {@link Oneblock#saveData()} so the persistence path stays in
 * one place; {@code saveData} itself is the unit-tested seam.
 */
public final class PlayerDataSaveTask implements Runnable {
  private final Oneblock plugin;

  @SuppressFBWarnings(
      value = "EI_EXPOSE_REP2",
      justification =
          "Intentional dependency injection pattern. The plugin reference is stored "
              + "for use in the run() method. The task is a final class with controlled lifecycle, "
              + "and the plugin reference is never modified after construction.")
  public PlayerDataSaveTask(Oneblock plugin) {
    this.plugin = plugin;
  }

  @Override
  public void run() {
    plugin.saveData();
  }
}
