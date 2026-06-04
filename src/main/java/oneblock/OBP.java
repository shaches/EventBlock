package oneblock;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.TreeMap;
import java.util.UUID;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import oneblock.context.PluginContext;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

@SuppressFBWarnings(
    value = "HE_INHERITS_EQUALS_USE_HASHCODE",
    justification =
        "OBP extends PlaceholderExpansion which does not implement hashCode(). This class does not"
            + " override equals() either, so the inherited Object.hashCode() is appropriate. The"
            + " class is used as a singleton PlaceholderAPI expansion and is not used in hash-based"
            + " collections.")
public final class OBP extends PlaceholderExpansion {

  private static final TreeMap<Double, String> SCALE;
  private static final String SCALE_CHAR = "█";
  private static final String NONE_PLACEHOLDER = "[None]";

  static {
    SCALE = new TreeMap<>();
    SCALE.put(.0, "&c╍╍╍╍╍╍╍╍");
    SCALE.put(.125, "&a╍&c╍╍╍╍╍╍╍");
    SCALE.put(.25, "&a╍╍&c╍╍╍╍╍╍");
    SCALE.put(.375, "&a╍╍╍&c╍╍╍╍╍");
    SCALE.put(.5, "&a╍╍╍╍&c╍╍╍╍");
    SCALE.put(.625, "&a╍╍╍╍╍&c╍╍╍");
    SCALE.put(.75, "&a╍╍╍╍╍╍&c╍╍");
    SCALE.put(.875, "&a╍╍╍╍╍╍╍&c╍");
    SCALE.put(1., "&a╍╍╍╍╍╍╍╍");
  }

  @Override
  public boolean canRegister() {
    return true;
  }

  @Override
  public String getAuthor() {
    return "MrMarL";
  }

  @Override
  public String getIdentifier() {
    return "OB";
  }

  @Override
  public String getPlugin() {
    return null;
  }

  @Override
  public String getVersion() {
    PluginContext ctx = PluginContext.get();
    if (ctx != null) {
      return ctx.plugin().version;
    }
    return "unknown";
  }

  @Override
  public String onRequest(OfflinePlayer p, String identifier) {
    if (p == null) return null;

    if (identifier.endsWith("_by_position")) {
      if (!(p instanceof Player)) return NONE_PLACEHOLDER;
      PluginContext ctx = PluginContext.get();
      if (ctx == null) return NONE_PLACEHOLDER;
      UUID ownerUUID =
          PlayerInfo.get(ctx.plugin().findNearestRegionId(((Player) p).getLocation())).uuid;
      if (ownerUUID == null) return NONE_PLACEHOLDER;

      return onRequest(
          Bukkit.getOfflinePlayer(ownerUUID),
          identifier.substring(0, identifier.length() - "_by_position".length()));
    }

    switch (identifier) {
      case "current_level_id":
        return PlayerInfo.get(p.getUniqueId()).currentLevelId;

      case "task_groups_done":
        PlayerInfo infTask = PlayerInfo.get(p.getUniqueId());
        return Integer.toString(
            infTask.taskProgress.getCompletedGroupCount(LevelRegistry.get(infTask.currentLevelId)));

      case "task_groups_total":
        PlayerInfo infTaskTotal = PlayerInfo.get(p.getUniqueId());
        return Integer.toString(
            infTaskTotal.taskProgress.getTotalGroupCount(
                LevelRegistry.get(infTaskTotal.currentLevelId)));

      case "next_themes":
        List<String> outgoing =
            LevelGraph.getOutgoing(PlayerInfo.get(p.getUniqueId()).currentLevelId);
        return outgoing.isEmpty() ? "N/A" : String.join(", ", outgoing);

      case "lvl":
        return Integer.toString(Oneblock.getLevel(p.getUniqueId()));

      case "lvl_name":
        return Oneblock.getLevelName(p.getUniqueId());

      case "next_lvl":
        {
          PlayerInfo infNext = PlayerInfo.get(p.getUniqueId());
          Level curNext = LevelRegistry.get(infNext.currentLevelId);
          if (curNext != null && curNext.nextThemes != null && !curNext.nextThemes.isEmpty()) {
            int idx = LevelRegistry.getIndex(curNext.nextThemes.get(0));
            if (idx >= 0) return Integer.toString(idx);
          }
          return Integer.toString(Oneblock.getLevel(p.getUniqueId()) + 1);
        }

      case "next_lvl_name":
        {
          PlayerInfo infNextName = PlayerInfo.get(p.getUniqueId());
          Level curNextName = LevelRegistry.get(infNextName.currentLevelId);
          if (curNextName != null
              && curNextName.nextThemes != null
              && !curNextName.nextThemes.isEmpty()) {
            Level nxt = LevelRegistry.get(curNextName.nextThemes.get(0));
            if (nxt != null && nxt != Level.max) return nxt.name;
          }
          return Level.get(Oneblock.getLevel(p.getUniqueId()) + 1).name;
        }

      case "break_on_this_lvl":
        return Integer.toString(getSyntheticBreaks(PlayerInfo.get(p.getUniqueId())));

      case "lvl_length":
        return Integer.toString(getSyntheticLevelLength(PlayerInfo.get(p.getUniqueId())));

      case "need_to_lvl_up":
        {
          PlayerInfo infRem = PlayerInfo.get(p.getUniqueId());
          return Integer.toString(getSyntheticLevelLength(infRem) - getSyntheticBreaks(infRem));
        }

      case "player_count":
        PluginContext ctxCount = PluginContext.get();
        if (ctxCount != null) {
          return Integer.toString(ctxCount.plugin().cache.getPlayers().size());
        }
        return "0";

      case "online_players":
        PluginContext ctxOnline = PluginContext.get();
        if (ctxOnline != null) {
          return Integer.toString(ctxOnline.plugin().cache.getPlayers().size());
        }
        return "0";

      case "visit_allowed":
        return Boolean.toString(Oneblock.isVisitAllowed(p.getUniqueId()));

      case "visits":
        return Integer.toString(Oneblock.countVisitors(p.getUniqueId()));

      case "percent":
        PlayerInfo inf0 = PlayerInfo.get(p.getUniqueId());
        return Integer.toString((int) (inf0.getPercent() * 100)) + "%";

      case "scale":
        PlayerInfo inf1 = PlayerInfo.get(p.getUniqueId());
        return SCALE.floorEntry(inf1.getPercent()).getValue().replace("╍", SCALE_CHAR);

      case "number_of_invited":
        return Integer.toString(PlayerInfo.get(p.getUniqueId()).uuids.size());

      case "owner_name":
        return getOwnerName(p.getUniqueId());

      case "owner_online":
        return getOwnerOnlineStatus(p.getUniqueId());

      case "top_position":
        PlayerInfo playerInfo = PlayerInfo.get(p.getUniqueId());
        int position = Oneblock.getTopPosition(playerInfo);
        return position == -1 ? NONE_PLACEHOLDER : Integer.toString(position + 1);
    }

    // %OB_top_%d_...%
    if (identifier.startsWith("top_")) {
      return handleTopPlaceholder(identifier);
    }

    return null;
  }

  private String getOwnerName(UUID playerUUID) {
    PlayerInfo playerInfo = PlayerInfo.get(playerUUID);
    UUID ownerUUID = playerInfo.uuid;

    if (ownerUUID == null) return NONE_PLACEHOLDER;

    OfflinePlayer owner = Bukkit.getOfflinePlayer(ownerUUID);
    return owner.getName() != null ? owner.getName() : NONE_PLACEHOLDER;
  }

  private String getOwnerOnlineStatus(UUID playerUUID) {
    UUID ownerUUID = PlayerInfo.get(playerUUID).uuid;

    if (ownerUUID == null || Bukkit.getPlayer(ownerUUID) == null) return "offline";

    return "online";
  }

  private static int getSyntheticBreaks(PlayerInfo inf) {
    Level cur = LevelRegistry.get(inf.currentLevelId);
    if (cur != null && cur.tasks != null && !cur.tasks.isEmpty()) {
      int sum = 0;
      for (LevelTask task : cur.tasks) sum += inf.taskProgress.get(task.id);
      return sum;
    }
    return inf.breaks;
  }

  private static int getSyntheticLevelLength(PlayerInfo inf) {
    Level cur = LevelRegistry.get(inf.currentLevelId);
    if (cur != null && cur.tasks != null && !cur.tasks.isEmpty()) {
      int sum = 0;
      for (LevelTask task : cur.tasks) sum += task.amount;
      return sum;
    }
    return inf.getRequiredBreaks();
  }

  private String handleTopPlaceholder(String identifier) {
    String[] parts = identifier.split("_", 3);
    if (parts.length != 3) return null;

    try {
      int position = Integer.parseInt(parts[1]) - 1;
      if (position < 0 || position >= 10) return null;

      PlayerInfo topPlayer = Oneblock.getTop(position);

      if (topPlayer.uuid == null) return NONE_PLACEHOLDER;

      switch (parts[2]) {
        case "name":
          OfflinePlayer player = Bukkit.getOfflinePlayer(topPlayer.uuid);
          return player.getName() != null ? player.getName() : NONE_PLACEHOLDER;

        case "lvl":
          {
            int topIdx = LevelRegistry.getIndex(topPlayer.currentLevelId);
            return Integer.toString(topIdx >= 0 ? topIdx : topPlayer.lvl);
          }
      }
    } catch (NumberFormatException e) {
      return null;
    }

    return null;
  }
}
