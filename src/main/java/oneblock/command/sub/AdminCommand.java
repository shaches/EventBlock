package oneblock.command.sub;

import oneblock.Level;
import oneblock.Oneblock;
import oneblock.PlayerInfo;
import oneblock.command.CommandContext;
import oneblock.command.Subcommand;
import oneblock.gui.GUI;
import oneblock.worldguard.OBWorldGuard;
import org.bukkit.ChatColor;

public final class AdminCommand implements Subcommand {
  @Override
  public String name() {
    return "admin";
  }

  @Override
  public String permission() {
    return "oneblock.set";
  }

  @Override
  public boolean execute(CommandContext ctx) {
    ctx.sender().sendMessage(ChatColor.GOLD + "EventBlock Admin Diagnostics");
    ctx.sender().sendMessage(ChatColor.GRAY + "World set: " + (Oneblock.getWorld() != null));
    ctx.sender().sendMessage(ChatColor.GRAY + "Offset: " + Oneblock.getOffset());
    ctx.sender().sendMessage(ChatColor.GRAY + "Levels: " + Level.size());
    ctx.sender().sendMessage(ChatColor.GRAY + "Islands: " + PlayerInfo.getListSize());
    ctx.sender().sendMessage(ChatColor.GRAY + "GUI enabled: " + GUI.getConfig().enabled);
    ctx.sender().sendMessage(ChatColor.GRAY + "Protection: " + Oneblock.settings().protection);
    ctx.sender().sendMessage(ChatColor.GRAY + "Progress bar: " + Oneblock.settings().progressBar);
    ctx.sender().sendMessage(ChatColor.GRAY + "WorldGuard: " + OBWorldGuard.isEnabled());
    ctx.sender().sendMessage(ChatColor.GRAY + "Placement type: " + ctx.plugin().placetype);
    ctx.sender()
        .sendMessage(
            ChatColor.YELLOW + "Shortcuts: /ob reload, /ob listlvl, /ob chest <name> edit");
    return true;
  }
}
