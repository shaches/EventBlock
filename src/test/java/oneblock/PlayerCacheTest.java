package oneblock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import oneblock.context.PluginContext;
import oneblock.context.PluginContextTestUtil;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PlayerCacheTest {

  @BeforeEach
  void reset() {
    PlayerInfo.replaceAll(java.util.Collections.emptyList());
    PluginContextTestUtil.reset();
  }

  @AfterEach
  void tearDown() {
    PluginContextTestUtil.reset();
  }

  @Test
  @DisplayName("getEntries exposes players and coordinates from the same cache snapshot")
  void getEntriesUsesStableSnapshotValues() {
    UUID uuid = UUID.randomUUID();
    Player player = mock(Player.class);
    when(player.getUniqueId()).thenReturn(uuid);

    PlayerInfo.set(0, new PlayerInfo(uuid));
    Oneblock plugin = mock(Oneblock.class);
    when(plugin.getIslandCoordinates(0)).thenReturn(new int[] {10, 20, 0});
    PluginContext.initialize(plugin);

    PlayerCache cache = new PlayerCache();
    cache.updateCache(List.of(player));

    Map.Entry<Player, int[]> entry = cache.getEntries().iterator().next();

    assertThat(entry.getKey()).isSameAs(player);
    assertThat(entry.getValue()).containsExactly(10, 20, 0);
  }
}
