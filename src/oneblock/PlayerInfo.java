package oneblock;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarFlag;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;

public final class PlayerInfo {
  /**
   * Island-slot storage. Thread-safe for async reads (iterators are snapshot copies) and
   * main-thread mutations. External callers MUST NOT mutate this list directly; go through {@link
   * #set(int, PlayerInfo)}, {@link #replaceAll(List)}, {@link #removeUUID(UUID)}, or the instance
   * {@link #addInvite(UUID)} / {@link #removeInvite(UUID)} methods so the companion {@link
   * #UUID_INDEX} and top-list version stay consistent.
   */
  public static final List<PlayerInfo> list = new CopyOnWriteArrayList<>();

  /** Reverse index: owner UUID OR invited-member UUID → island id (position in {@link #list}). */
  private static final ConcurrentMap<UUID, Integer> UUID_INDEX = new ConcurrentHashMap<>();

  /**
   * Monotonic counter bumped whenever the top-list's sort order could have changed (level-up, slot
   * assignment, bulk reload). Consumers of {@code getTopList} compare their cached version to this
   * to decide if a re-sort is needed.
   */
  private static final AtomicLong TOP_VERSION = new AtomicLong();

  public static final PlayerInfo not_found = new PlayerInfo(null);

  public UUID uuid;

  /**
   * Co-owner / invitee UUIDs for this island. Phase 4.4 swapped the previous {@code ArrayList} for
   * a {@link CopyOnWriteArrayList} because this list is iterated by the async {@code
   * PlayerDataSaveTask} (via {@code JsonPlayerDataStore.write} and {@code DatabaseManager.save})
   * every 6000 ticks while the main thread can concurrently call {@link #addInvite(UUID)}, {@link
   * #removeInvite(UUID)} or the invitee-promotion path inside {@link #removeUUID(UUID)} from any
   * /ob accept / kick / idreset command. Pre-Phase-4.4 this was a documented latent race - the
   * iterator would CME if a save happened to interleave a kick. The list is also read by the {@code
   * OBP} placeholder for {@code %OB_number_of_invited%}, which PlaceholderAPI may dispatch from any
   * thread depending on the requesting plugin.
   */
  public final List<UUID> uuids = new CopyOnWriteArrayList<>();

  public volatile String currentLevelId = "level_0";
  public final TaskProgress taskProgress = new TaskProgress();

  /**
   * Phase 2: when a player's current level task groups are all satisfied, auto-progression halts
   * and this flag is set to {@code true}. The player must use the theme-selection GUI ({@code
   * LevelSelectGUI}) to pick a {@code next_theme} before block generation resumes counting toward
   * the next level.
   */
  public volatile boolean waitingForThemeSelection = false;

  /**
   * Legacy integer level index, kept transiently for storage migration (Phase 1). Do not use in new
   * code; prefer {@link #currentLevelId}.
   */
  public int lvl = 0;

  /**
   * Legacy block-break counter, kept transiently for storage migration (Phase 1). Do not use in new
   * code; prefer {@link #taskProgress}.
   */
  public int breaks = 0;

  public BossBar bar = null;

  /**
   * Whether other players are allowed to {@code /ob visit} this island. Pre-Phase-6.3 this field
   * was named {@code allow_visit} (snake_case); the Java field name was renamed to {@code
   * allowVisit} per convention. The on-disk JSON key remains {@code visit} and the SQL column name
   * remains {@code allow_visit} - both are public persistence schemas and were intentionally NOT
   * renamed.
   */
  public boolean allowVisit = false;

  public PlayerInfo(UUID uuid) {
    this.uuid = uuid;
  }

  /**
   * Phase 2 polish: ensure the legacy integer {@code lvl} / {@code breaks} fields stay in sync with
   * the canonical {@code currentLevelId} / {@code taskProgress} state.
   *
   * <p>Call this after every mutation of level-related fields so that JSON/YAML dual-write and
   * integer-based consumers (top-list sort, placeholders, old commands) never observe stale values.
   */
  public void syncLegacyFields() {
    if (currentLevelId != null) {
      int idx = LevelRegistry.getIndex(currentLevelId);
      if (idx >= 0) {
        lvl = idx;
      } else if (currentLevelId.startsWith("level_")) {
        // Registry not yet populated (early load) but id follows the
        // legacy convention — derive the integer index directly so
        // storage round-trips stay consistent in tests and at startup.
        try {
          lvl = Integer.parseInt(currentLevelId.substring(6));
        } catch (NumberFormatException ignored) {
          // malformed id: keep existing lvl
        }
      }
      // For non-convention ids not in registry, keep existing lvl (came
      // from storage and will be reconciled after /ob reload).
    }
    // When task-based progression is active, breaks is a legacy ghost counter
    Level current = LevelRegistry.get(currentLevelId);
    if (current != null && current.tasks != null && !current.tasks.isEmpty()) {
      breaks = 0;
    }
  }

  public Level lvlup() {
    // Phase 1: legacy field sync for storage dual-write
    ++lvl;
    breaks = 0;
    // New path: auto-progression no longer automatically advances levels.
    // The player must pick a next theme from LevelGraph via GUI (Phase 2).
    // For now, bump the legacy index so existing callers don't break.
    Level next = Level.get(lvl);
    if (next != null && next.id != null) {
      currentLevelId = next.id;
    } else {
      currentLevelId = "level_" + lvl;
    }
    taskProgress.reset();
    syncLegacyFields();
    TOP_VERSION.incrementAndGet();
    return next;
  }

  public void createBar() {
    Level level = LevelRegistry.get(currentLevelId);
    if (level == Level.max) level = Level.get(lvl);
    createBar(level.name, level.color, level.style);
  }

  public void createBar(String title) {
    Level level = LevelRegistry.get(currentLevelId);
    if (level == Level.max) level = Level.get(lvl);
    createBar(title, level.color, level.style);
  }

  private void createBar(String text, BarColor color, BarStyle style) {
    if (color == null) color = BarColor.GREEN;
    if (style == null) style = BarStyle.SOLID;
    if (bar == null) {
      bar = Bukkit.createBossBar(text, color, style, BarFlag.DARKEN_SKY);
      return;
    }
    bar.setTitle(text);
    bar.setColor(color);
    bar.setStyle(style);
  }

  public void removeBar(OfflinePlayer p) {
    if (bar == null) return;
    if (!(p instanceof Player)) return;
    bar.removePlayer((Player) p);
  }

  public void removeUUID(UUID deleted) {
    if (deleted == null) return;
    if (uuid != null && uuid.equals(deleted)) {
      UUID_INDEX.remove(deleted);
      if (!uuids.isEmpty()) {
        uuid = uuids.remove(0);
        // New owner UUID is already mapped to this island id via the invite path.
      } else {
        uuid = null;
      }
    } else if (uuids.remove(deleted)) {
      UUID_INDEX.remove(deleted);
    }
  }

  /** Add an invited-member UUID to this island and keep the reverse index consistent. */
  public void addInvite(UUID inviteeUuid) {
    if (inviteeUuid == null) return;
    uuids.add(inviteeUuid);
    if (this.uuid != null) {
      Integer id = UUID_INDEX.get(this.uuid);
      if (id != null) UUID_INDEX.put(inviteeUuid, id);
    }
  }

  /** Remove an invited-member UUID from this island and keep the reverse index consistent. */
  public void removeInvite(UUID inviteeUuid) {
    if (inviteeUuid == null) return;
    if (uuids.remove(inviteeUuid)) {
      UUID_INDEX.remove(inviteeUuid);
    }
  }

  /**
   * The number of blocks the player has to break on the current level before {@link #lvlup()}
   * fires. Equivalent to {@code Level.get(lvl).length} but resolved through the published {@link
   * Level} list so the value stays consistent under {@code /ob reload} (Phase 4.1 publication-safe
   * snapshot).
   *
   * <p>Pre-Phase-6.5 this was named {@code getNeed()} — kept a vague abbreviation that the
   * placeholder dispatch ({@code OBP}) and the block-generator ({@code Oneblock.generateBlock}) had
   * to reverse-engineer from context. The new name is self-describing.
   */
  public int getRequiredBreaks() {
    Level level = LevelRegistry.get(currentLevelId);
    if (level == Level.max) level = Level.get(lvl);
    return level.length;
  }

  public double getPercent() {
    Level level = LevelRegistry.get(currentLevelId);
    if (level == Level.max) level = Level.get(lvl);
    if (level.tasks != null && !level.tasks.isEmpty()) {
      return taskProgress.getCompletedGroupRatio(level);
    }
    return (double) breaks / getRequiredBreaks();
  }

  /**
   * Phase 2: advance the player to the chosen next level, resetting task progress and clearing the
   * theme-selection wait state.
   *
   * @return the new Level, or {@code null} if the id was invalid.
   */
  public Level advanceToLevel(String nextLevelId) {
    Level next = LevelRegistry.get(nextLevelId);
    if (next == null || next == Level.max) return null;
    this.currentLevelId = nextLevelId;
    this.taskProgress.reset();
    this.waitingForThemeSelection = false;
    this.breaks = 0;
    syncLegacyFields();
    TOP_VERSION.incrementAndGet();
    return next;
  }

  public static void removeBarFor(Player p) {
    if (list.isEmpty()) return;
    get(p.getUniqueId()).removeBar(p);
  }

  /**
   * O(1) lookup of the island id that owns or has invited the given UUID. Returns -1 if the UUID is
   * not tracked.
   */
  public static int getId(UUID uuid) {
    if (uuid == null) return -1;
    Integer id = UUID_INDEX.get(uuid);
    return id == null ? -1 : id;
  }

  public static boolean existsAsOwner(UUID name) {
    if (name == null) return false;
    Integer id = UUID_INDEX.get(name);
    if (id == null) return false;
    PlayerInfo pl = list.get(id);
    return pl != null && name.equals(pl.uuid);
  }

  public static PlayerInfo get(int id) {
    return list.get(id);
  }

  public static PlayerInfo get(UUID uuid) {
    int plID = getId(uuid);
    if (plID == -1) return not_found;
    return list.get(plID);
  }

  /**
   * Set or append an island slot. Also registers the PlayerInfo's owner and invited-member UUIDs in
   * the reverse index under the given id.
   */
  public static void set(int id, PlayerInfo pInf) {
    if (id < list.size()) list.set(id, pInf);
    else list.add(pInf);
    registerInIndex(pInf, id);
    IslandCoordinateCalculator.invalidateCellIndex();
    TOP_VERSION.incrementAndGet();
  }

  /**
   * Replace the entire island list in one shot (e.g. on initial load from DB or legacy JSON/YAML).
   * Rebuilds the reverse index from scratch.
   */
  public static void replaceAll(List<PlayerInfo> newList) {
    list.clear();
    UUID_INDEX.clear();
    if (newList != null) {
      list.addAll(newList);
      for (int i = 0; i < newList.size(); i++) {
        registerInIndex(newList.get(i), i);
      }
    }
    IslandCoordinateCalculator.invalidateCellIndex();
    TOP_VERSION.incrementAndGet();
  }

  private static void registerInIndex(PlayerInfo inf, int id) {
    if (inf == null) return;
    if (inf.uuid != null) UUID_INDEX.put(inf.uuid, id);
    for (UUID u : inf.uuids) UUID_INDEX.put(u, id);
  }

  /** Current top-list version. Use with {@code getTopList} snapshot caching. */
  public static long topVersion() {
    return TOP_VERSION.get();
  }

  public static int getFreeId(boolean useEmptyIslands) {
    if (useEmptyIslands) return PlayerInfo.getNull();
    return PlayerInfo.size();
  }

  public static int size() {
    return list.size();
  }

  private static int getNull() {
    for (int i = 0; list.size() > i; i++) if (list.get(i).uuid == null) return i;
    return list.size();
  }

  public static final Comparator<PlayerInfo> COMPARE_BY_LVL =
      new Comparator<PlayerInfo>() {
        @Override
        public int compare(PlayerInfo lhs, PlayerInfo rhs) {
          if (rhs.uuid == null) return -1;
          int lhsIdx = levelIndex(lhs);
          int rhsIdx = levelIndex(rhs);
          if (lhsIdx != rhsIdx) return rhsIdx - lhsIdx;
          // Tie-breaker: task completion ratio (descending)
          Level lvl = LevelRegistry.get(lhs.currentLevelId);
          if (lvl != Level.max && lvl.tasks != null && !lvl.tasks.isEmpty()) {
            double lhsPct = lhs.taskProgress.getCompletedGroupRatio(lvl);
            double rhsPct = rhs.taskProgress.getCompletedGroupRatio(lvl);
            return Double.compare(rhsPct, lhsPct);
          }
          return rhs.lvl - lhs.lvl;
        }

        private int levelIndex(PlayerInfo inf) {
          LevelRegistry.Snapshot snap = LevelRegistry.snapshot();
          List<Level> ordered = snap.allOrdered();
          for (int i = 0; i < ordered.size(); i++) {
            if (inf.currentLevelId.equals(ordered.get(i).id)) return i;
          }
          return inf.lvl;
        }
      };

  /**
   * Identity equality keyed on {@link #uuid}. Two {@code PlayerInfo} instances represent the same
   * island slot iff they share an owner UUID; the level / breaks counters are mutable per-island
   * state and intentionally not part of the contract. {@code uuid == null} only matches another
   * {@code uuid == null} (the {@link #not_found} sentinel and freshly-allocated slots before
   * assignment).
   *
   * <p>Phase 3.7 added this so {@code Oneblock.getTopPosition} no longer relies on identity
   * comparison of two list members - if the top-list cache snapshot ever races a slot reassignment,
   * structural equality on UUID still works.
   */
  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof PlayerInfo)) return false;
    PlayerInfo that = (PlayerInfo) o;
    return java.util.Objects.equals(this.uuid, that.uuid);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hashCode(this.uuid);
  }
}
