package oneblock.command.sub;

import java.util.UUID;
import oneblock.LevelRegistry;
import oneblock.Oneblock;
import oneblock.PlayerInfo;
import oneblock.command.AdminPrelude;
import oneblock.command.CommandContext;
import oneblock.command.Subcommand;
import oneblock.migration.LegacyLevelMapper;
import oneblock.utils.Utils;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

/**
 * {@code /ob setlevel <player> <level>} - admin. Force-sets a player's island level to the given
 * integer (0..10000), zeroes their breaks counter, and re-renders the BossBar if {@code
 * progress_bar} is on and the player is online.
 *
 * <p>Behaviour-equivalent to the legacy {@code "setlevel"} admin case extracted in Phase 3.5b. Used
 * primarily for testing / loadout gifting; the matching {@code clear} command resets levels to
 * zero, not arbitrary values.
 */
public final class SetLevelCommand implements Subcommand {
  @Override
  public String name() {
    return "setlevel";
  }

  @Override
  public String permission() {
    return "Oneblock.set";
  }

  @Override
  public boolean execute(CommandContext ctx) {
    AdminPrelude.run(ctx);
    String[] args = ctx.args();
    if (args.length <= 2) {
      ctx.sender()
          .sendMessage(
              String.format(
                  "%sinvalid format. try: /ob setlevel 'nickname' 'level'", ChatColor.RED));
      return true;
    }
    OfflinePlayer offpl = Utils.getOfflinePlayerByName(args[1]);
    UUID uuid = offpl.getUniqueId();
    int plID = PlayerInfo.getId(uuid);
    if (plID != -1) {
      PlayerInfo inf = PlayerInfo.get(plID);
      String argLevel = args[2];
      if (LevelRegistry.contains(argLevel)) {
        // String level id path
        int idx = LevelRegistry.getIndex(argLevel);
        inf.currentLevelId = argLevel;
        inf.lvl = idx >= 0 ? idx : 0;
      } else {
        int setlvl = 0;
        try {
          setlvl = Integer.parseInt(argLevel);
          if (setlvl < 0 || setlvl > 10000) throw new NumberFormatException();
        } catch (NumberFormatException nfe) {
          ctx.sender()
              .sendMessage(
                  String.format("%sinvalid level value or unknown level id.", ChatColor.RED));
          return true;
        }
        inf.lvl = setlvl;
        String mappedId = LegacyLevelMapper.fromInt(setlvl);
        if (LevelRegistry.contains(mappedId)) {
          inf.currentLevelId = mappedId;
        } else {
          inf.currentLevelId = "level_" + setlvl;
        }
      }
      inf.breaks = 0;
      inf.taskProgress.reset();
      inf.waitingForThemeSelection = false;
      inf.syncLegacyFields();
      if (Oneblock.settings().progressBar && offpl instanceof Player) {
        inf.createBar(Oneblock.getBarTitle((Player) offpl, inf));
        inf.bar.setProgress(inf.getPercent());
      }
      ctx.sender()
          .sendMessage(
              String.format(
                  "%sfor player %s, level %s is set.", ChatColor.GREEN, args[1], args[2]));
      return true;
    }
    ctx.sender()
        .sendMessage(String.format("%sa player named %s was not found.", ChatColor.RED, args[1]));
    return true;
  }
}
