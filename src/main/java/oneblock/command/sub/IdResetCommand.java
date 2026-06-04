package oneblock.command.sub;

import oneblock.CommandHandler;
import oneblock.Messages;
import oneblock.command.CommandContext;
import oneblock.command.Subcommand;

/**
 * {@code /ob idreset} (no arg) - user-facing self-reset. With no arg, runs {@link
 * CommandHandler#idresetCommand} on the sender themselves and then chains to {@code /ob leave /n}
 * (the {@code /n} suffix suppresses the "leave not set" warning when a fresh server has no
 * leaveworld configured yet). Requires {@code oneblock.idreset}.
 *
 * <p>Behaviour-equivalent to the legacy {@code "idreset"} switch case extracted in Phase 3.5b. The
 * one-arg form is user-facing and requires only {@code oneblock.idreset} permission. The multi-arg
 * form (admin target override) requires {@code oneblock.set} permission and performs the same logic
 * as {@link AdminIdResetCommand}.
 */
public final class IdResetCommand implements Subcommand {
  private static final AdminIdResetCommand ADMIN_DELEGATE = new AdminIdResetCommand();

  @Override
  public String name() {
    return "idreset";
  }

  @Override
  public boolean execute(CommandContext ctx) {
    if (ctx.args().length == 1) {
      // User-facing self-reset path.
      if (ctx.player() == null) return false;
      if (!ctx.sender().hasPermission("oneblock.idreset")) {
        ctx.sender()
            .sendMessage(
                org.bukkit.ChatColor.RED + "You don't have permission [oneblock.idreset].");
        return true;
      }
      if (!CommandHandler.idresetCommand(ctx.player())) return true;
      ctx.sender().sendMessage(Messages.idreset());
      ctx.player().performCommand("ob leave /n");
      return true;
    }
    // Multi-arg: delegate to admin form, which checks oneblock.set.
    if (!ctx.sender().hasPermission("oneblock.set")) {
      ctx.sender()
          .sendMessage(org.bukkit.ChatColor.RED + "You don't have permission [oneblock.set].");
      return true;
    }
    return ADMIN_DELEGATE.execute(ctx);
  }
}
