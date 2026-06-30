package oneblock.gui;

import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.format.NamedTextColor;
import oneblock.Level;
import oneblock.LevelGraph;
import oneblock.LevelRegistry;
import oneblock.LevelTask;
import oneblock.Messages;
import oneblock.Oneblock;
import oneblock.PlayerInfo;
import oneblock.gui.dialog.DialogMenu;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

public final class DialogGUI {
  private static final int VISIT_PAGE_SIZE = 8;
  private static final int TOP_SIZE = 10;

  private DialogGUI() {}

  public static void openMain(Player player) {
    if (!GUI.getConfig().enabled || player == null) return;
    PlayerInfo inf = PlayerInfo.get(player.getUniqueId());
    String levelName = "No island";
    String progress = "Join an island to start progressing.";
    if (inf != null && inf != PlayerInfo.not_found && inf.uuid != null) {
      Level level = inf.reconcileCurrentLevelId();
      if (level != null && level != Level.max) levelName = plain(level.name);
      progress =
          String.format("Progress: %.0f%%", Math.min(1.0, Math.max(0.0, inf.getPercent())) * 100.0);
    }
    DialogMenu.Builder menu =
        DialogMenu.builder(title(Messages.baseGUI(), "EventBlock"))
            .body("Level: " + levelName, NamedTextColor.GRAY)
            .body(progress, NamedTextColor.GRAY)
            .option("Join island", NamedTextColor.GREEN, p -> p.performCommand("ob join"))
            .option("Leave island", NamedTextColor.YELLOW, p -> p.performCommand("ob leave"))
            .option("Progress", NamedTextColor.AQUA, DialogGUI::openProgress)
            .option("Top islands", NamedTextColor.GOLD, DialogGUI::openTop)
            .option("Island", NamedTextColor.GREEN, DialogGUI::openIsland);
    if (player.hasPermission("oneblock.visit")) {
      menu.option("Visit islands", NamedTextColor.BLUE, p -> openVisit(p, 0));
    }
    if (player.hasPermission("oneblock.idreset")) {
      menu.option("Reset island", NamedTextColor.RED, DialogGUI::openIdResetConfirm);
    }
    if (player.hasPermission("oneblock.set")) {
      menu.option(
          "Admin diagnostics", NamedTextColor.LIGHT_PURPLE, p -> p.performCommand("ob admin"));
    }
    menu.open(player);
  }

  public static void openInvite(Player player, String inviterName) {
    if (!GUI.getConfig().enabled || player == null) return;
    DialogMenu.builder(title(Messages.acceptGUI(), "Island invitation"))
        .body("You were invited by " + inviterName + ".", NamedTextColor.GRAY)
        .option("Accept", NamedTextColor.GREEN, p -> p.performCommand("ob accept"))
        .option("Ignore", NamedTextColor.RED, p -> {})
        .open(player);
  }

  public static void openTop(Player player) {
    if (!GUI.getConfig().enabled || player == null) return;
    DialogMenu.Builder menu = DialogMenu.builder(title(Messages.topGUI(), "Top islands"));
    List<PlayerInfo> toplist = Oneblock.getTopList();
    int rendered = 0;
    for (int i = 0; i < Math.min(TOP_SIZE, toplist.size()); i++) {
      PlayerInfo inf = Oneblock.getTop(i, toplist);
      if (inf == null || inf == PlayerInfo.not_found || inf.uuid == null) continue;
      menu.body(
          (i + 1)
              + ". "
              + playerName(inf.uuid)
              + " - "
              + levelDisplay(inf)
              + " - "
              + teamDisplay(inf),
          NamedTextColor.GRAY);
      rendered++;
    }
    if (rendered == 0) menu.body("No islands are ranked yet.", NamedTextColor.GRAY);
    menu.option("Back", NamedTextColor.YELLOW, DialogGUI::openMain).open(player);
  }

  public static void openVisit(Player player, OfflinePlayer[] ignored) {
    openVisit(player, 0);
  }

  private static void openVisit(Player player, int page) {
    if (!GUI.getConfig().enabled || player == null) return;
    List<PlayerInfo> visitable = visitableIslands(player);
    int maxPage = visitable.isEmpty() ? 0 : (visitable.size() - 1) / VISIT_PAGE_SIZE;
    int safePage = Math.max(0, Math.min(page, maxPage));
    DialogMenu.Builder menu =
        DialogMenu.builder(title(Messages.visitGUI(), "Visit islands"))
            .body("Page " + (safePage + 1) + "/" + (maxPage + 1), NamedTextColor.GRAY);
    if (visitable.isEmpty()) {
      menu.body("No islands are open for visits.", NamedTextColor.GRAY);
    } else {
      int start = safePage * VISIT_PAGE_SIZE;
      int end = Math.min(start + VISIT_PAGE_SIZE, visitable.size());
      for (int i = start; i < end; i++) {
        PlayerInfo inf = visitable.get(i);
        String owner = playerName(inf.uuid);
        menu.option(
            owner,
            NamedTextColor.GREEN,
            levelDisplay(inf) + " | " + teamDisplay(inf),
            NamedTextColor.GRAY,
            p -> p.performCommand("ob visit " + owner));
      }
    }
    if (safePage > 0)
      menu.option("Previous", NamedTextColor.YELLOW, p -> openVisit(p, safePage - 1));
    if (safePage < maxPage)
      menu.option("Next", NamedTextColor.YELLOW, p -> openVisit(p, safePage + 1));
    menu.option("Back", NamedTextColor.YELLOW, DialogGUI::openMain).open(player);
  }

  static void openProgress(Player player) {
    if (!GUI.getConfig().enabled || player == null) return;
    PlayerInfo inf = PlayerInfo.get(player.getUniqueId());
    if (inf == null || inf == PlayerInfo.not_found || inf.uuid == null) {
      DialogMenu.builder("Progress")
          .body("Create an island first with /ob join.", NamedTextColor.GRAY)
          .option("Join island", NamedTextColor.GREEN, p -> p.performCommand("ob join"))
          .option("Back", NamedTextColor.YELLOW, DialogGUI::openMain)
          .open(player);
      return;
    }
    Level level = inf.reconcileCurrentLevelId();
    DialogMenu.Builder menu =
        DialogMenu.builder("Progress")
            .body(
                "Current: " + (level != null ? plain(level.name) : inf.currentLevelId),
                NamedTextColor.GRAY)
            .body(String.format("Overall: %.0f%%", inf.getPercent() * 100.0), NamedTextColor.GRAY);
    if (level != null && level.tasks != null && !level.tasks.isEmpty()) {
      for (LevelTask task : level.tasks) {
        int current = Math.min(inf.taskProgress.get(task.id), task.amount);
        menu.body(
            task.group + ": " + task.type + " " + task.target + " " + current + "/" + task.amount,
            current >= task.amount ? NamedTextColor.GREEN : NamedTextColor.GRAY);
      }
    } else {
      menu.body("Blocks: " + inf.breaks + "/" + inf.getRequiredBreaks(), NamedTextColor.GRAY);
    }
    menu.option("Rewards", NamedTextColor.GOLD, p -> openRewards(p, inf.currentLevelId));
    List<String> choices = LevelGraph.getOutgoing(inf.currentLevelId);
    if (!choices.isEmpty()) {
      menu.option(
          "Preview branches", NamedTextColor.AQUA, p -> openBranchPreview(p, inf.currentLevelId));
    }
    if (inf.waitingForThemeSelection) {
      menu.option(
          "Choose theme", NamedTextColor.GREEN, p -> LevelSelectGUI.openIfAvailable(p, inf));
    }
    menu.option("Back", NamedTextColor.YELLOW, DialogGUI::openMain).open(player);
  }

  static void openRewards(Player player, String levelId) {
    if (player == null) return;
    Level level = LevelRegistry.get(levelId);
    String levelName = level != null && level != Level.max ? level.name : levelId;
    List<String> rewards =
        Oneblock.configManager.reward.previewCompletionRewards(player, levelId, levelName);
    DialogMenu.Builder menu = DialogMenu.builder("Rewards");
    if (rewards.isEmpty()) {
      menu.body("No configured rewards for this level.", NamedTextColor.GRAY);
    } else {
      for (String reward : rewards) menu.body(plain(reward), NamedTextColor.GRAY);
    }
    List<String> choices = LevelGraph.getOutgoing(levelId);
    if (!choices.isEmpty()) {
      menu.option("Future branches", NamedTextColor.AQUA, p -> openBranchPreview(p, levelId));
    }
    menu.option("Back", NamedTextColor.YELLOW, DialogGUI::openProgress).open(player);
  }

  static void openBranchPreview(Player player, String sourceLevelId) {
    if (player == null) return;
    List<String> choices = LevelGraph.getOutgoing(sourceLevelId);
    DialogMenu.Builder menu = DialogMenu.builder("Branch rewards");
    if (choices.isEmpty()) {
      menu.body("No future branches are configured.", NamedTextColor.GRAY);
    } else {
      for (String levelId : choices) {
        Level level = LevelRegistry.get(levelId);
        String levelName = level != null && level != Level.max ? level.name : levelId;
        String preview =
            previewSummary(
                Oneblock.configManager.reward.previewAdvanceRewards(player, levelId, levelName));
        menu.option(
            plain(levelName),
            NamedTextColor.GREEN,
            preview,
            NamedTextColor.GRAY,
            p -> openBranchDetails(p, sourceLevelId, levelId));
      }
    }
    menu.option("Back", NamedTextColor.YELLOW, DialogGUI::openProgress).open(player);
  }

  static void openBranchDetails(Player player, String sourceLevelId, String levelId) {
    if (player == null) return;
    Level level = LevelRegistry.get(levelId);
    String levelName = level != null && level != Level.max ? level.name : levelId;
    List<String> rewards =
        Oneblock.configManager.reward.previewAdvanceRewards(player, levelId, levelName);
    DialogMenu.Builder menu = DialogMenu.builder("Preview: " + plain(levelName));
    if (level != null && level != Level.max) {
      menu.body("Length: " + level.length, NamedTextColor.GRAY);
      if (level.tasks != null && !level.tasks.isEmpty()) {
        menu.body("Tasks: " + level.tasks.size(), NamedTextColor.GRAY);
      }
    }
    if (rewards.isEmpty()) {
      menu.body("No configured advance rewards.", NamedTextColor.GRAY);
    } else {
      for (String reward : rewards) menu.body(plain(reward), NamedTextColor.GRAY);
    }
    PlayerInfo inf = PlayerInfo.get(player.getUniqueId());
    if (inf != null
        && inf.waitingForThemeSelection
        && LevelGraph.isSuccessor(sourceLevelId, levelId)) {
      menu.option(
          "Select this theme",
          NamedTextColor.GREEN,
          p -> LevelSelectGUI.confirmSelection(p, levelId));
    }
    menu.option("Back", NamedTextColor.YELLOW, p -> openBranchPreview(p, sourceLevelId))
        .open(player);
  }

  private static void openIsland(Player player) {
    PlayerInfo inf = PlayerInfo.get(player.getUniqueId());
    DialogMenu.Builder menu = DialogMenu.builder("Island");
    if (inf == null || inf == PlayerInfo.not_found || inf.uuid == null) {
      menu.body("You do not have an island yet.", NamedTextColor.GRAY)
          .option("Join island", NamedTextColor.GREEN, p -> p.performCommand("ob join"));
    } else {
      menu.body("Owner: " + playerName(inf.uuid), NamedTextColor.GRAY)
          .body("Members: " + inf.uuids.size(), NamedTextColor.GRAY)
          .body("Visits: " + (inf.allowVisit ? "open" : "closed"), NamedTextColor.GRAY)
          .option("Toggle visits", NamedTextColor.GREEN, p -> p.performCommand("ob allow_visit"))
          .option(
              "Invite with /ob invite <player>",
              NamedTextColor.AQUA,
              p -> p.sendMessage("Use /ob invite <player> to invite a teammate."));
    }
    menu.option("Back", NamedTextColor.YELLOW, DialogGUI::openMain).open(player);
  }

  private static void openIdResetConfirm(Player player) {
    DialogMenu.builder("Reset island?")
        .body(plain(Messages.idresetGUI()), NamedTextColor.RED)
        .body("This cannot be undone.", NamedTextColor.GRAY)
        .option("Confirm reset", NamedTextColor.RED, p -> p.performCommand("ob idreset"))
        .option("Cancel", NamedTextColor.GREEN, DialogGUI::openMain)
        .open(player);
  }

  private static List<PlayerInfo> visitableIslands(Player viewer) {
    List<PlayerInfo> result = new ArrayList<>();
    for (PlayerInfo inf : PlayerInfo.getList()) {
      if (inf == null || inf.uuid == null || !inf.allowVisit) continue;
      if (inf.uuid.equals(viewer.getUniqueId())) continue;
      OfflinePlayer owner = Bukkit.getOfflinePlayer(inf.uuid);
      if (owner instanceof Player && !((Player) owner).hasPermission("oneblock.allow_visit"))
        continue;
      result.add(inf);
    }
    return result;
  }

  private static String levelDisplay(PlayerInfo inf) {
    Level level = inf.reconcileCurrentLevelId();
    if (level != null && level != Level.max) return plain(level.name);
    int idx = LevelRegistry.getIndex(inf.currentLevelId);
    return "Level " + (idx >= 0 ? idx : inf.lvl);
  }

  private static String teamDisplay(PlayerInfo inf) {
    int members = inf.uuids.size();
    if (members == 0) return "solo";
    return members + " member" + (members == 1 ? "" : "s");
  }

  private static String playerName(java.util.UUID uuid) {
    if (uuid == null) return "Unknown";
    String name = Bukkit.getOfflinePlayer(uuid).getName();
    return name == null ? "Unknown" : name;
  }

  private static String title(String configured, String fallback) {
    String plain = plain(configured);
    return plain == null || plain.isBlank() ? fallback : plain;
  }

  private static String plain(String text) {
    if (text == null) return "";
    String stripped = ChatColor.stripColor(text);
    return stripped == null ? text : stripped;
  }

  private static String previewSummary(List<String> rewards) {
    if (rewards.isEmpty()) return "No configured advance rewards";
    String first = plain(rewards.get(0));
    if (rewards.size() == 1) return first;
    return first + " +" + (rewards.size() - 1) + " more";
  }
}
