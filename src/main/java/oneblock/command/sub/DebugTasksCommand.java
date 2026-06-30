package oneblock.command.sub;

import java.util.Map;
import oneblock.Level;
import oneblock.LevelTask;
import oneblock.PlayerInfo;
import oneblock.TaskType;
import oneblock.command.CommandContext;
import oneblock.command.Subcommand;
import org.bukkit.ChatColor;

/**
 * {@code /ob debugtasks} - debug command to check if broken blocks are being counted for task
 * completion. Shows current level, all tasks, and their progress counters. Useful for verifying
 * that BREAK tasks (e.g., SCULK) are incrementing when blocks are broken.
 */
public final class DebugTasksCommand implements Subcommand {

  @Override
  public String name() {
    return "debugtasks";
  }

  @Override
  public String permission() {
    return "oneblock.set";
  }

  @Override
  public boolean requiresPlayer() {
    return true;
  }

  @Override
  public boolean execute(CommandContext ctx) {
    if (ctx.player() == null) {
      ctx.sender().sendMessage(ChatColor.RED + "This command can only be run by a player.");
      return true;
    }

    int plID = PlayerInfo.getId(ctx.player().getUniqueId());
    if (plID == -1) {
      ctx.sender().sendMessage(ChatColor.RED + "You are not registered in the OneBlock system.");
      return true;
    }

    PlayerInfo inf = PlayerInfo.get(plID);
    if (inf == null) {
      ctx.sender().sendMessage(ChatColor.RED + "Player data not found.");
      return true;
    }

    Level level = inf.reconcileCurrentLevelId();
    if (level == null || level == Level.max) {
      ctx.sender()
          .sendMessage(ChatColor.YELLOW + "You are at max level or have no active level data.");
      return true;
    }

    ctx.sender()
        .sendMessage(
            ChatColor.GOLD
                + "=== Task Debug for "
                + ctx.player().getName()
                + " (Level: "
                + level.name
                + ") ===");

    if (level.tasks == null || level.tasks.isEmpty()) {
      ctx.sender().sendMessage(ChatColor.GRAY + "This level has no tasks configured.");
      return true;
    }

    Map<String, Integer> progress = inf.taskProgress.snapshot();
    boolean hasBreakTasks = false;

    for (LevelTask task : level.tasks) {
      int current = progress.getOrDefault(task.id, 0);
      String status = current >= task.amount ? ChatColor.GREEN + "✓" : ChatColor.RED + "✗";

      String taskInfo =
          String.format(
              "%s %s %s: %d/%d (%s)",
              status, ChatColor.WHITE, task.id, current, task.amount, task.type);

      if (task.type == TaskType.BREAK) {
        hasBreakTasks = true;
        taskInfo += ChatColor.AQUA + " [BREAK]";
      }

      ctx.sender().sendMessage(taskInfo);
    }

    if (!hasBreakTasks) {
      ctx.sender().sendMessage(ChatColor.YELLOW + "No BREAK tasks found in this level.");
    } else {
      ctx.sender()
          .sendMessage(
              ChatColor.GRAY
                  + "Break a block and run this command again to verify the counter increments.");
    }

    ctx.sender()
        .sendMessage(
            ChatColor.GRAY
                + "Level complete: "
                + (inf.taskProgress.isLevelComplete()
                    ? ChatColor.GREEN + "YES"
                    : ChatColor.RED + "NO"));

    return true;
  }
}
