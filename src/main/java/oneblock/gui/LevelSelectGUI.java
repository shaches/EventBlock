package oneblock.gui;

import java.util.List;
import net.kyori.adventure.text.format.NamedTextColor;
import oneblock.Level;
import oneblock.LevelGraph;
import oneblock.LevelRegistry;
import oneblock.Oneblock;
import oneblock.PlayerInfo;
import oneblock.gui.dialog.DialogMenu;
import org.bukkit.entity.Player;

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
    inf.reconcileCurrentLevelId();
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
    DialogMenu.Builder menu =
        DialogMenu.builder("Select Next Theme")
            .body(
                "Your level is complete. Choose the next theme for your island.",
                NamedTextColor.GRAY)
            .canCloseWithEscape(false)
            .columns(Math.min(3, choices.size()));
    PlayerInfo inf = PlayerInfo.get(player.getUniqueId());
    String sourceLevelId = inf == null ? null : inf.currentLevelId;
    for (String levelId : choices) {
      Level level = LevelRegistry.get(levelId);
      String displayName = level != null && level.name != null ? level.name : levelId;
      String tooltip = previewTooltip(player, levelId, displayName);
      String selectedLevelId = levelId;
      menu.option(
          displayName,
          NamedTextColor.GREEN,
          tooltip,
          NamedTextColor.GRAY,
          selectedPlayer ->
              DialogGUI.openBranchDetails(selectedPlayer, sourceLevelId, selectedLevelId));
    }
    menu.open(player);
  }

  private static void select(Player player, String levelId) {
    if (player == null || !LevelRegistry.isValidLevelId(levelId)) return;
    PlayerInfo inf = PlayerInfo.get(player.getUniqueId());
    if (inf == null) return;
    Level next = inf.advanceToLevel(levelId);
    if (next != null) {
      Oneblock.configManager.reward.executeAdvanceReward(player, levelId, next.name);
    } else if (inf.waitingForThemeSelection) {
      openIfAvailable(player, inf);
    }
  }

  public static void confirmSelection(Player player, String levelId) {
    if (player == null || !LevelRegistry.isValidLevelId(levelId)) return;
    Level level = LevelRegistry.get(levelId);
    String displayName = level != null && level.name != null ? level.name : levelId;
    DialogMenu.builder("Confirm Theme")
        .body("Select " + displayName + " as your next island theme?", NamedTextColor.GRAY)
        .body("This choice cannot be changed after confirmation.", NamedTextColor.RED)
        .option("Confirm", NamedTextColor.GREEN, p -> select(p, levelId))
        .option(
            "Back",
            NamedTextColor.YELLOW,
            p -> {
              PlayerInfo inf = PlayerInfo.get(p.getUniqueId());
              if (inf != null) openIfAvailable(p, inf);
            })
        .open(player);
  }

  private static String previewTooltip(Player player, String levelId, String displayName) {
    Level level = LevelRegistry.get(levelId);
    String levelName = level != null && level.name != null ? level.name : displayName;
    List<String> rewards =
        Oneblock.configManager.reward.previewAdvanceRewards(player, levelId, levelName);
    if (rewards.isEmpty()) return "ID: " + levelId + " | No configured advance rewards";
    String first = rewards.get(0);
    return "ID: "
        + levelId
        + " | "
        + first
        + (rewards.size() == 1 ? "" : " +" + (rewards.size() - 1) + " more");
  }
}
