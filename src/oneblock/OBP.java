package oneblock;

import java.util.List;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

public final class OBP extends PlaceholderExpansion {

  private static final TreeMap<Double, String> SCALE;
  private static final String SCALE_CHAR = "█";
  private static final String NONE_PLACEHOLDER = "[None]";

  /**
   * Set of legacy / typo'd placeholder names that have already been deprecation-warned this
   * session, so each name is logged at most once per JVM. PAPI dispatches from any thread, so the
   * set is a concurrent-safe key set. Test code may clear it via reflection to verify the
   * once-per-session contract.
   */
  private static final Set<String> WARNED_DEPRECATED_PLACEHOLDERS = ConcurrentHashMap.newKeySet();

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
    return Oneblock.plugin.version;
  }

  @Override
  public String onRequest(OfflinePlayer p, String identifier) {
    if (p == null) return null;

    if (identifier.endsWith("_by_position")) {
      if (!(p instanceof Player)) return NONE_PLACEHOLDER;
      UUID ownerUUID =
          PlayerInfo.get(Oneblock.plugin.findNearestRegionId(((Player) p).getLocation())).uuid;
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
      case "lvl_lenght":
        // `lvl_lenght` was the original (typo'd) placeholder name shipped
        // in pre-Phase-6 releases; existing servers' scoreboards still
        // reference it, so we keep it as a backward-compat alias and
        // emit a once-per-session deprecation warning. New servers
        // should use `lvl_length`.
        if ("lvl_lenght".equals(identifier))
          warnDeprecatedPlaceholderOnce("lvl_lenght", "lvl_length");
        return Integer.toString(getSyntheticLevelLength(PlayerInfo.get(p.getUniqueId())));

      case "need_to_lvl_up":
        {
          PlayerInfo infRem = PlayerInfo.get(p.getUniqueId());
          return Integer.toString(getSyntheticLevelLength(infRem) - getSyntheticBreaks(infRem));
        }

      case "player_count":
        return Integer.toString(Oneblock.plugin.cache.getPlayers().size());

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

  /**
   * Log a deprecation warning for {@code legacy} the first time the legacy spelling is dispatched
   * in this JVM. Subsequent dispatches for the same legacy name are silent so the warning never
   * floods the console under steady-state placeholder traffic. Set membership is the once-flag;
   * insertion races (PAPI dispatches from any thread) are resolved by the underlying {@link
   * ConcurrentHashMap#newKeySet()} so at most one warning is logged.
   */
  private static void warnDeprecatedPlaceholderOnce(String legacy, String canonical) {
    if (!WARNED_DEPRECATED_PLACEHOLDERS.add(legacy)) return;
    if (Oneblock.plugin == null) return;
    Oneblock.plugin
        .getLogger()
        .warning(
            "[Oneblock] Placeholder %OB_"
                + legacy
                + "% is deprecated; please use %OB_"
                + canonical
                + "% instead. "
                + "The legacy spelling will be removed in a future release.");
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
