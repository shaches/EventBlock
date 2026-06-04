package oneblock.invitation;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import oneblock.PlayerInfo;

public class Guest extends AbstractInvitation {
  public static class Config {
    public final List<Guest> list = new CopyOnWriteArrayList<>();
  }

  private static Config config = new Config();

  public static Config getConfig() {
    return config;
  }

  public static void setConfig(Config newConfig) {
    config = new Config();
  }

  // Legacy static field accessor for backward compatibility during Phase 3 migration
  public static final List<Guest> list = config.list;

  public Guest(UUID inviting, UUID invited) {
    super(inviting, invited);
  }

  public static Guest check(UUID uuid) {
    for (Guest item : config.list) if (item.Invited.equals(uuid)) return item;
    return null;
  }

  public static PlayerInfo getPlayerInfo(UUID uuid) {
    Guest g = check(uuid);
    if (g == null) return null;
    if (PlayerInfo.getId(g.Inviting) == -1) return null;
    return PlayerInfo.get(g.Inviting);
  }

  public static boolean remove(UUID uuid) {
    Guest r = null;
    for (Guest g : config.list)
      if (g.Invited.equals(uuid)) {
        r = g;
        break;
      }
    return config.list.remove(r);
  }
}
