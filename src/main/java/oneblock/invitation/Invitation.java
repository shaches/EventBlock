package oneblock.invitation;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import oneblock.CommandHandler;
import oneblock.PlayerInfo;
import oneblock.context.PluginContext;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class Invitation extends AbstractInvitation {
  public static class Config {
    public final List<Invitation> list = new CopyOnWriteArrayList<>();
  }

  private static Config config = new Config();

  public static Config getConfig() {
    return config;
  }

  public static void setConfig(Config newConfig) {
    config = newConfig;
  }

  // Legacy static field accessor for backward compatibility during Phase 3 migration
  public static final List<Invitation> list = config.list;

  public Invitation(UUID inviting, UUID invited) {
    super(inviting, invited);
  }

  public static void add(UUID name, UUID to) {
    for (Invitation item : config.list) if (item.equals(name, to)) return;
    Invitation inv_ = new Invitation(name, to);
    config.list.add(inv_);
    PluginContext ctx = PluginContext.get();
    if (ctx != null) {
      Bukkit.getScheduler()
          .runTaskLater(
              ctx.plugin(),
              () -> {
                config.list.remove(inv_);
              },
              300L);
    }
  }

  public static Invitation check(UUID uuid) {
    for (Invitation item : config.list) if (item.Invited.equals(uuid)) return item;
    return null;
  }

  public static boolean check(Player pl) {
    if (pl == null) return false;
    UUID uuid = pl.getUniqueId();
    Invitation inv_ = check(uuid);
    if (inv_ == null) return false;
    if (PlayerInfo.getId(inv_.Inviting) == -1) return false;

    CommandHandler.idresetCommand(pl);

    PlayerInfo.get(inv_.Inviting).addInvite(uuid);
    pl.performCommand("ob j");
    config.list.remove(inv_);
    return true;
  }
}
