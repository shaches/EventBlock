package oneblock;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import oneblock.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

public final class RewardManager {
  // Reject anything that is not a Mojang-legal player name; blocks command-injection
  // attempts via offline-mode / proxy-forwarded nicknames being substituted into
  // reward templates that are dispatched as console.
  private static final Pattern SAFE_PLAYER_NAME = Pattern.compile("^[A-Za-z0-9_]{1,16}$");

  private List<String> allRewards = new ArrayList<>();
  private Map<Integer, List<String>> levelRewards = new HashMap<>();
  private Map<String, List<String>> levelIdRewards = new HashMap<>();

  public void loadRewards() {
    allRewards.clear();
    levelRewards.clear();
    levelIdRewards.clear();

    File rewardsFile = new File(Oneblock.plugin.getDataFolder(), "rewards.yml");
    if (!rewardsFile.exists()) {
      Oneblock.plugin.saveResource("rewards.yml", false);
    }

    YamlConfiguration config = YamlConfiguration.loadConfiguration(rewardsFile);

    // Load general rewards
    if (config.isList("all")) {
      List<String> rawRewards = config.getStringList("all");
      allRewards = new ArrayList<>();
      for (String reward : rawRewards) allRewards.add(Utils.translateColorCodes(reward));
    }

    // Load level-specific rewards
    org.bukkit.configuration.ConfigurationSection levelsSection =
        config.getConfigurationSection("levels");
    if (levelsSection != null) {
      for (String levelStr : levelsSection.getKeys(false)) {
        List<String> rawRewards = config.getStringList("levels." + levelStr);
        List<String> processedRewards = new ArrayList<>();
        for (String reward : rawRewards) processedRewards.add(Utils.translateColorCodes(reward));

        try {
          int level = Integer.parseInt(levelStr);
          levelRewards.put(level, processedRewards);
        } catch (NumberFormatException e) {
          Oneblock.plugin
              .getLogger()
              .warning(
                  "[Oneblock] Invalid level number '"
                      + levelStr
                      + "' in rewards.yml; treating as string-id reward set.");
          levelIdRewards.put(levelStr, processedRewards);
        }
      }
    }

    Oneblock.plugin
        .getLogger()
        .info(
            "Loaded "
                + allRewards.size()
                + " general rewards, "
                + levelRewards.size()
                + " integer-keyed reward sets, and "
                + levelIdRewards.size()
                + " string-id reward sets");
  }

  public void executeRewards(Player player, int level, String levelName) {
    _executeRewards(player, String.valueOf(level), levelName, levelRewards.get(level));
  }

  public void executeRewards(Player player, String levelId, String levelName) {
    _executeRewards(player, levelId, levelName, levelIdRewards.get(levelId));
  }

  private void _executeRewards(
      Player player, String levelIdentifier, String levelName, List<String> specificRewards) {
    String playerName = player.getName();
    if (playerName == null || !SAFE_PLAYER_NAME.matcher(playerName).matches()) {
      Oneblock.plugin
          .getLogger()
          .warning(
              "Skipping reward dispatch for player with unsafe name: '"
                  + playerName
                  + "'. Expected "
                  + SAFE_PLAYER_NAME.pattern()
                  + ".");
      return;
    }

    Map<String, String> placeholders = new HashMap<>();
    placeholders.put("%nick%", playerName);
    placeholders.put("%lvl_number%", levelIdentifier);
    placeholders.put("%lvl_name%", levelName);

    executeCommandList(player, allRewards, placeholders);
    if (specificRewards != null) {
      executeCommandList(player, specificRewards, placeholders);
    }
  }

  public void executeCompletionReward(Player player, String currentLevelId, String levelName) {
    // Tag the placeholders so reward configs can differentiate completion vs advance
    String playerName = player.getName();
    if (playerName == null || !SAFE_PLAYER_NAME.matcher(playerName).matches()) return;
    List<String> specific = levelIdRewards.get(currentLevelId + ":complete");
    if (specific == null) specific = levelIdRewards.get(currentLevelId);
    _executeRewards(player, currentLevelId, levelName, specific);
  }

  public void executeAdvanceReward(Player player, String nextLevelId, String levelName) {
    String playerName = player.getName();
    if (playerName == null || !SAFE_PLAYER_NAME.matcher(playerName).matches()) return;
    List<String> specific = levelIdRewards.get(nextLevelId + ":advance");
    if (specific == null) specific = levelIdRewards.get(nextLevelId);
    _executeRewards(player, nextLevelId, levelName, specific);
  }

  private void executeCommandList(
      Player player, List<String> commands, Map<String, String> placeholders) {
    for (String command : commands) {
      // Replace placeholders
      String finalCommand = command;
      for (Map.Entry<String, String> entry : placeholders.entrySet()) {
        finalCommand = finalCommand.replace(entry.getKey(), entry.getValue());
      }

      // Execute command as console
      Bukkit.dispatchCommand(Bukkit.getConsoleSender(), finalCommand);
    }
  }

  public void reload() {
    loadRewards();
  }
}
