package oneblock.gui.dialog;

import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;
import net.kyori.adventure.key.Key;
import org.bukkit.entity.Player;

public final class DialogMenuManager {
  private static final String NAMESPACE = "eventblock";
  private static final AtomicLong NEXT_SESSION_ID = new AtomicLong();
  private static final ConcurrentMap<UUID, Session> SESSIONS = new ConcurrentHashMap<>();

  private DialogMenuManager() {}

  public static void open(Player player, DialogMenu menu) {
    if (player == null || menu == null || menu.options().isEmpty()) return;
    long sessionId = NEXT_SESSION_ID.incrementAndGet();
    Session session = new Session(new LinkedHashMap<>());
    List<ActionButton> buttons = new ArrayList<>();
    List<DialogMenu.Option> options = menu.options();
    for (int i = 0; i < options.size(); i++) {
      DialogMenu.Option option = options.get(i);
      Key key = Key.key(NAMESPACE, "dialog/" + sessionId + "/" + i);
      session.actions().put(key, option.action());
      ActionButton button =
          ActionButton.builder(DialogMenu.copy(option.label()))
              .tooltip(option.tooltip() == null ? null : DialogMenu.copy(option.tooltip()))
              .action(DialogAction.customClick(key, null))
              .build();
      buttons.add(button);
    }
    SESSIONS.put(player.getUniqueId(), session);
    player.showDialog(createDialog(menu, buttons));
  }

  static boolean handle(Player player, Key key) {
    if (player == null || key == null) return false;
    Session session = SESSIONS.get(player.getUniqueId());
    if (session == null) return false;
    DialogMenuAction action = session.actions().get(key);
    if (action == null) return false;
    SESSIONS.remove(player.getUniqueId());
    player.closeDialog();
    action.execute(player);
    return true;
  }

  public static void clear(Player player) {
    if (player != null) SESSIONS.remove(player.getUniqueId());
  }

  private static Dialog createDialog(DialogMenu menu, List<ActionButton> buttons) {
    DialogBase.Builder baseBuilder =
        DialogBase.builder(DialogMenu.copy(menu.title()))
            .canCloseWithEscape(menu.canCloseWithEscape());
    List<DialogBody> body = new ArrayList<>();
    for (DialogMenu.BodyLine line : menu.body()) {
      body.add(DialogBody.plainMessage(DialogMenu.copy(line.contents())));
    }
    if (!body.isEmpty()) {
      baseBuilder.body(body);
    }
    DialogBase base = baseBuilder.build();
    return Dialog.create(
        builder -> builder.empty().base(base).type(DialogType.multiAction(buttons).build()));
  }

  private record Session(Map<Key, DialogMenuAction> actions) {}
}
