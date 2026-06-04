package oneblock.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;
import oneblock.Level;
import oneblock.LevelRegistry;
import oneblock.Oneblock;
import oneblock.PlayerInfo;
import oneblock.context.PluginContext;
import oneblock.context.PluginContextTestUtil;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Round-trip coverage for {@link JsonPlayerDataStore}, the JSON-backed player data store on the
 * steady-state save/load path. Phase 5.3 lifts this class from {@code 0%} line coverage; a
 * regression here is a data-loss class because every server's island roster lives in the file this
 * store writes.
 *
 * <p>The store's {@code f} field is a {@code static final File} that eagerly evaluates {@code
 * Oneblock.plugin.getDataFolder()} at class-load time. To make that initialiser succeed in a unit
 * test (no real Bukkit server, no real plugin), {@link #installMockPlugin()} stubs {@link
 * Oneblock#plugin} with a Mockito mock whose {@code getDataFolder()} returns a class-scoped {@link
 * TempDir} path, then forces the class to load via {@link Class#forName(String)} <em>before</em>
 * any test method runs. The static {@code f} is subsequently a stable handle to {@code
 * <tempDir>/PlData.json} for the lifetime of this test class.
 *
 * <p>Each test deletes the on-disk JSON in {@link #cleanFile()} so stale data from the previous
 * test cannot leak into the next read.
 *
 * <p>The nick-fallback path in {@code resolveUuid} is intentionally <b>not</b> covered here - it
 * calls {@link oneblock.utils.Utils#getOfflinePlayerByName} which dereferences {@link
 * org.bukkit.Bukkit#getOfflinePlayers()} and only works on a real server. UUID-keyed entries (the
 * modern primary path) cover ~80 % of the class.
 */
class JsonPlayerDataStoreTest {

  @TempDir static Path tempDir;

  private static Oneblock savedPlugin;
  private static Logger pluginLogger;

  @BeforeAll
  static void installMockPlugin() throws Exception {
    savedPlugin = Oneblock.plugin;
    pluginLogger = mock(Logger.class);
    Oneblock mockPlugin = mock(Oneblock.class);
    when(mockPlugin.getDataFolder()).thenReturn(tempDir.toFile());
    when(mockPlugin.getLogger()).thenReturn(pluginLogger);
    Oneblock.plugin = mockPlugin;
    PluginContextTestUtil.reset();
    PluginContext.initialize(mockPlugin);
    // Force class init NOW, while plugin is set, so the static-final
    // 'f' resolves to <tempDir>/PlData.json.
    Class.forName("oneblock.storage.JsonPlayerDataStore");

    // Initialize LevelRegistry with test levels to prevent level mapping issues
    ArrayList<Level> testLevels = new ArrayList<>();
    for (int i = 0; i <= 23; i++) {
      Level lvl = new Level("level_" + i, "Test Level " + i);
      testLevels.add(lvl);
    }
    LevelRegistry.replaceAll(testLevels);
  }

  @AfterAll
  static void restorePlugin() {
    PluginContextTestUtil.reset();
    Oneblock.plugin = savedPlugin;
  }

  @BeforeEach
  void cleanFile() {
    // The static 'f' is the same File handle for every test in this
    // class; we just delete the on-disk content between runs so a
    // 'missing PlData.json' test sees a missing file even if a
    // previous test wrote one.
    if (JsonPlayerDataStore.f.exists()) {
      //noinspection ResultOfMethodCallIgnored
      JsonPlayerDataStore.f.delete();
    }
    org.mockito.Mockito.reset(pluginLogger);
  }

  // --------------------------------------------------------------
  // write -> read round-trips (UUID-keyed)
  // --------------------------------------------------------------

  @Test
  @DisplayName("round-trip: empty list writes a header-only file and reads back as an empty list")
  void emptyListRoundTrip() {
    JsonPlayerDataStore.writeStatic(Collections.emptyList());
    assertThat(JsonPlayerDataStore.f).exists();
    List<PlayerInfo> read = JsonPlayerDataStore.readStatic();
    assertThat(read).isEmpty();
  }

  @Test
  @DisplayName("json round-trip: single owner with lvl/breaks roundtrips identity-equally")
  void singleOwnerRoundTrip() {
    UUID owner = UUID.randomUUID();
    PlayerInfo p = new PlayerInfo(owner);
    p.lvl = 7;
    p.breaks = 23;
    p.currentLevelId = "level_7";

    JsonPlayerDataStore.writeStatic(Collections.singletonList(p));
    List<PlayerInfo> loaded = JsonPlayerDataStore.readStatic();

    assertThat(loaded).hasSize(1);
    assertThat(loaded.get(0).uuid).isEqualTo(owner);
    assertThat(loaded.get(0).lvl).isEqualTo(7);
    assertThat(loaded.get(0).breaks).isEqualTo(23);
    assertThat(loaded.get(0).allowVisit).isFalse();
    assertThat(loaded.get(0).uuids).isEmpty();
  }

  @Test
  @DisplayName("json write excludes runtime-only BossBar field")
  void bossBarIsNotPersisted() throws Exception {
    PlayerInfo p = new PlayerInfo(UUID.randomUUID());
    p.bar = org.mockito.Mockito.mock(org.bukkit.boss.BossBar.class);

    JsonPlayerDataStore.writeStatic(Collections.singletonList(p));

    String json = Files.readString(JsonPlayerDataStore.f.toPath(), StandardCharsets.UTF_8);
    assertThat(json).doesNotContain("bar");
    assertThat(JsonPlayerDataStore.readStatic()).hasSize(1);
  }

  @Test
  @DisplayName("round-trip: owner with invited UUIDs preserves the invitee list order")
  void ownerWithInvitedUuids() {
    UUID owner = UUID.randomUUID();
    UUID inv1 = UUID.randomUUID();
    UUID inv2 = UUID.randomUUID();
    PlayerInfo p = new PlayerInfo(owner);
    p.uuids.add(inv1);
    p.uuids.add(inv2);

    JsonPlayerDataStore.writeStatic(Collections.singletonList(p));
    List<PlayerInfo> read = JsonPlayerDataStore.readStatic();

    assertThat(read).hasSize(1);
    assertThat(read.get(0).uuids).containsExactly(inv1, inv2);
  }

  @Test
  @DisplayName("round-trip: allowVisit=true is persisted as the 'visit' key and restored truthy")
  void allowVisitFlagRoundTrip() {
    UUID owner = UUID.randomUUID();
    PlayerInfo p = new PlayerInfo(owner);
    p.allowVisit = true;

    JsonPlayerDataStore.writeStatic(Collections.singletonList(p));
    List<PlayerInfo> read = JsonPlayerDataStore.readStatic();

    assertThat(read.get(0).allowVisit).isTrue();
  }

  @Test
  @DisplayName(
      "round-trip: a null-slot (uuid==null) entry writes as JSON null and reads back as the"
          + " not_found-equivalent")
  void nullSlotRoundTrip() {
    UUID owner = UUID.randomUUID();
    PlayerInfo realOwner = new PlayerInfo(owner);
    PlayerInfo emptySlot = new PlayerInfo(null);

    JsonPlayerDataStore.writeStatic(Arrays.asList(realOwner, emptySlot));
    List<PlayerInfo> read = JsonPlayerDataStore.readStatic();

    assertThat(read).hasSize(2);
    assertThat(read.get(0).uuid).isEqualTo(owner);
    // Slot 1 round-trips as a PlayerInfo with uuid==null (the
    // 'nullable' sentinel constructed inside read()).
    assertThat(read.get(1).uuid).isNull();
  }

  @Test
  @DisplayName("round-trip: multi-island layout preserves slot positions exactly")
  void multiIslandPositionsPreserved() {
    UUID a = UUID.randomUUID();
    UUID b = UUID.randomUUID();
    UUID c = UUID.randomUUID();
    PlayerInfo pa = new PlayerInfo(a);
    pa.lvl = 1;
    pa.currentLevelId = "level_1";
    PlayerInfo pb = new PlayerInfo(b);
    pb.lvl = 2;
    pb.currentLevelId = "level_2";
    PlayerInfo pc = new PlayerInfo(c);
    pc.lvl = 3;
    pc.currentLevelId = "level_3";

    JsonPlayerDataStore.writeStatic(Arrays.asList(pa, pb, pc));
    List<PlayerInfo> read = JsonPlayerDataStore.readStatic();

    assertThat(read).extracting(pi -> pi.uuid).containsExactly(a, b, c);
    assertThat(read).extracting(pi -> pi.lvl).containsExactly(1, 2, 3);
  }

  // --------------------------------------------------------------
  // Read-path failure modes (must log + degrade, never throw)
  // --------------------------------------------------------------

  @Test
  @DisplayName("read on a missing file returns an empty list")
  void readMissingFileReturnsEmpty() {
    // The file was deleted in @BeforeEach; do nothing else.
    List<PlayerInfo> read = JsonPlayerDataStore.readStatic();
    assertThat(read).isEmpty();
  }

  @Test
  @DisplayName("read on a syntactically broken JSON file returns empty (no throw)")
  void brokenJsonReturnsEmpty() throws Exception {
    try (FileWriter w = new FileWriter(JsonPlayerDataStore.f)) {
      w.write("[{this is not valid json at all}]");
    }

    List<PlayerInfo> read = JsonPlayerDataStore.readStatic();

    assertThat(read).isEmpty();
    verify(pluginLogger, atLeastOnce()).warning(org.mockito.ArgumentMatchers.anyString());
  }

  @Test
  @DisplayName("write of a list whose target directory exists overwrites the previous JSON cleanly")
  void writeOverwritesPriorContent() {
    // First write: 2 entries.
    UUID a = UUID.randomUUID();
    UUID b = UUID.randomUUID();
    JsonPlayerDataStore.writeStatic(Arrays.asList(new PlayerInfo(a), new PlayerInfo(b)));
    assertThat(JsonPlayerDataStore.readStatic()).hasSize(2);

    // Second write: 1 entry. The file should not contain stale '1':...
    UUID c = UUID.randomUUID();
    List<PlayerInfo> singleton = new ArrayList<>();
    singleton.add(new PlayerInfo(c));
    JsonPlayerDataStore.writeStatic(singleton);

    List<PlayerInfo> read = JsonPlayerDataStore.readStatic();
    assertThat(read).hasSize(1);
    assertThat(read.get(0).uuid).isEqualTo(c);
  }

  // --------------------------------------------------------------
  // Encoding regression test (UTF-8 preservation)
  // --------------------------------------------------------------

  @Test
  @DisplayName(
      "round-trip: Unicode characters (emoji, non-ASCII) are preserved with UTF-8 encoding")
  void unicodeCharactersPreserved() throws Exception {
    // Create a JSON file with Unicode characters using UTF-8 encoding
    UUID owner = UUID.randomUUID();
    String unicodeJson =
        "[{\"uuid\":\"" + owner + "\",\"lvl\":0,\"breaks\":0,\"currentLevelId\":\"level_0\"}]";

    try (OutputStreamWriter w =
        new OutputStreamWriter(
            new FileOutputStream(JsonPlayerDataStore.f), StandardCharsets.UTF_8)) {
      w.write(unicodeJson);
    }

    List<PlayerInfo> read = JsonPlayerDataStore.readStatic();

    assertThat(read).hasSize(1);
    assertThat(read.get(0).uuid).isEqualTo(owner);
  }
}
