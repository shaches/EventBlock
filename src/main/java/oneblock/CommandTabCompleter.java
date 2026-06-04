package oneblock;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import oneblock.context.PluginContext;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

public final class CommandTabCompleter implements TabCompleter {
  private final List<String> VISIT_COMMANDS = Arrays.asList("v", "visit");

  @Override
  public List<String> onTabComplete(
      CommandSender sender, Command cmd, String label, String[] args) {
    List<String> commands = new ArrayList<>();

    if (args.length == 1) {
      // Auto-generate from CommandHandler registry
      for (String subcommand : CommandHandler.getRegisteredSubcommands()) {
        commands.add(subcommand);
      }
      Collections.sort(commands);
    } else if (args.length == 2) {
      String arg = args[0].toLowerCase(Locale.ROOT);

      if ("invite".equals(arg) || "kick".equals(arg) || VISIT_COMMANDS.contains(arg)) {
        addOnlinePlayers(commands);
      } else if (sender.hasPermission("oneblock.set")) {
        switch (arg) {
          case ("chest"):
            commands.addAll(ChestItems.getChestNames());
            break;
          case ("clear"):
          case ("idreset"):
          case ("setlevel"):
            addOnlinePlayers(commands);
            break;
          case ("progress_bar"):
            commands.add("true");
            commands.add("false");
            commands.add("level");
            commands.add("settext");
            commands.add("color");
            commands.add("style");
            break;
          case ("islands"):
            commands.add("set_my_by_def");
            commands.add("default");
            break;
          case ("useemptyislands"):
          case ("allow_nether"):
          case ("rebirth_on_the_island"):
          case ("saveplayerinventory"):
          case ("protection"):
          case ("circlemode"):
          case ("worldguard"):
          case ("border"):
          case ("autojoin"):
          case ("droptossup"):
          case ("physics"):
          case ("gui"):
            commands.add("true");
            commands.add("false");
            break;
          case ("listlvl"):
            for (int i = 0; i < Level.size(); i++) commands.add(String.valueOf(i));
            break;
          case ("lvl_mult"):
          case ("max_players_team"):
            for (int i = 0; i < 4; i++) commands.add(String.valueOf(i));
            break;
          case ("set"):
            commands.add("100");
            commands.add("500");
            commands.add("100 0 64 0");
            commands.add("500 0 64 0");
            break;
          default:
            break;
        }
      }
    } else if (sender.hasPermission("oneblock.set") && args.length == 3) {
      String arg0 = args[0].toLowerCase(Locale.ROOT);
      String arg1 = args[1].toLowerCase(Locale.ROOT);

      if ("progress_bar".equals(arg0)) {
        if ("color".equals(arg1)) for (BarColor bc : BarColor.values()) commands.add(bc.name());
        if ("style".equals(arg1)) for (BarStyle bc : BarStyle.values()) commands.add(bc.name());
        if ("settext".equals(arg1)) {
          commands.add("...");
          commands.add(
              "%OB_lvl% &8- %OB_lvl_name% &8| &fProgress:"
                  + " &e%OB_break_on_this_lvl%/%OB_lvl_length%");
          commands.add(
              "%OB_lvl_name% &8| &fProgress: &e%OB_break_on_this_lvl%/%OB_lvl_length%"
                  + " &8(&b%OB_need_to_lvl_up% left&8)");
        }
      } else if ("setlevel".equals(arg0)) {
        for (int i = 0; i < Level.size(); i++) commands.add(String.valueOf(i));
        for (Level l : LevelRegistry.snapshot().allOrdered()) {
          if (l.id != null) commands.add(l.id);
        }
      } else if ("chest".equals(arg0)) commands.add("set");
    } else if (sender.hasPermission("oneblock.set")
        && args.length == 4
        && "chest".equalsIgnoreCase(args[0])
        && "set".equalsIgnoreCase(args[2])) {
      commands.add("minecraft:chests/simple_dungeon");
      commands.add("minecraft:chests/abandoned_mineshaft");
      commands.add("minecraft:chests/end_city_treasure");
      commands.add("minecraft:chests/buried_treasure");
      commands.add("minecraft:chests/nether_bridge");
    }
    Collections.sort(commands);
    return commands;
  }

  // Auxiliary methods
  private void addOnlinePlayers(List<String> completions) {
    PluginContext ctx = PluginContext.get();
    if (ctx != null) {
      for (Player ponl : ctx.plugin().cache.getPlayers()) completions.add(ponl.getName());
    }
  }
}
