package oneblock.gui.dialog;

import io.papermc.paper.connection.PlayerGameConnection;
import io.papermc.paper.event.player.PlayerCustomClickEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public final class DialogMenuListener implements Listener {
  @EventHandler
  public void onPlayerCustomClick(PlayerCustomClickEvent event) {
    if (!(event.getCommonConnection() instanceof PlayerGameConnection gameConn)) return;
    Player player = gameConn.getPlayer();
    if (player == null) return;
    DialogMenuManager.handle(player, event.getIdentifier());
  }

  @EventHandler
  public void onPlayerQuit(PlayerQuitEvent event) {
    DialogMenuManager.clear(event.getPlayer());
  }
}
