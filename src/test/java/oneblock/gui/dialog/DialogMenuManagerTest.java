package oneblock.gui.dialog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.UUID;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import net.kyori.adventure.key.Key;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DialogMenuManagerTest {

  @BeforeEach
  void resetSessions() throws Exception {
    Field sessionsField = DialogMenuManager.class.getDeclaredField("SESSIONS");
    sessionsField.setAccessible(true);
    ((ConcurrentMap<?, ?>) sessionsField.get(null)).clear();

    Field nextSessionIdField = DialogMenuManager.class.getDeclaredField("NEXT_SESSION_ID");
    nextSessionIdField.setAccessible(true);
    ((AtomicLong) nextSessionIdField.get(null)).set(0);
  }

  @Test
  void handleExecutesRegisteredActionAndClearsDialog() {
    UUID uuid = UUID.randomUUID();
    Player player = mock(Player.class);
    when(player.getUniqueId()).thenReturn(uuid);
    AtomicBoolean executed = new AtomicBoolean(false);

    DialogMenu.builder("Choose")
        .option("Desert", null, p -> executed.set(p == player))
        .open(player);

    boolean handled = DialogMenuManager.handle(player, Key.key("eventblock", "dialog/1/0"));

    assertThat(handled).isTrue();
    assertThat(executed).isTrue();
    verify(player).closeDialog();
  }

  @Test
  void handleIgnoresUnknownAction() {
    UUID uuid = UUID.randomUUID();
    Player player = mock(Player.class);
    when(player.getUniqueId()).thenReturn(uuid);

    DialogMenu.builder("Choose").option("Desert", null, p -> {}).open(player);

    boolean handled = DialogMenuManager.handle(player, Key.key("eventblock", "dialog/999/0"));

    assertThat(handled).isFalse();
  }
}
