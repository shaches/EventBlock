package oneblock;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import oneblock.context.PluginContext;
import org.bukkit.entity.Player;

public final class PlayerCache {
  private volatile ConcurrentHashMap<Player, int[]> players = new ConcurrentHashMap<>();

  public void updateCache(Collection<? extends Player> onlinePlayers) {
    ConcurrentHashMap<Player, int[]> newMap = new ConcurrentHashMap<>();
    onlinePlayers.forEach(
        player -> {
          final UUID uuid = player.getUniqueId();
          final int plID = PlayerInfo.getId(uuid);
          if (plID == -1) return;
          PluginContext ctx = PluginContext.get();
          if (ctx != null) {
            newMap.put(player, ctx.plugin().getIslandCoordinates(plID));
          }
        });
    players = newMap;
  }

  public Collection<Player> getPlayers() {
    return java.util.Collections.unmodifiableCollection(players.keySet());
  }

  public Collection<Map.Entry<Player, int[]>> getEntries() {
    return java.util.Collections.unmodifiableCollection(players.entrySet());
  }

  public int[] getIslandCoordinates(Player player) {
    return players.get(player);
  }

  public boolean removePlayer(Player player) {
    return players.remove(player) != null;
  }
}
