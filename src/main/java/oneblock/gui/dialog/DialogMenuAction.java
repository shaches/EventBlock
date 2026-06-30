package oneblock.gui.dialog;

import org.bukkit.entity.Player;

@FunctionalInterface
public interface DialogMenuAction {
  void execute(Player player);
}
