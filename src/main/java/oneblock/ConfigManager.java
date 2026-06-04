package oneblock;

import com.cryptomorin.xseries.XMaterial;
import com.nexomc.nexo.api.NexoBlocks;
import dev.lone.itemsadder.api.CustomBlock;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.th0rgal.oraxen.api.OraxenItems;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import oneblock.context.PluginContext;
import oneblock.gui.GUI;
import oneblock.migration.LegacyBlocksMigrator;
import oneblock.migration.LegacyLevelMapper;
import oneblock.storage.DatabaseManager;
import oneblock.utils.Compat;
import oneblock.utils.LowerCaseYaml;
import oneblock.utils.Utils;
import oneblock.worldguard.OBWorldGuard;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

public final class ConfigManager {
  @SuppressFBWarnings(
      value = "PA_PUBLIC_PRIMITIVE_ATTRIBUTE",
      justification =
          "This field is intentionally public as a temporary configuration holder during the config"
              + " loading process. It is used internally by ConfigManager methods and is not part"
              + " of the public API. The field is reused across multiple config loading operations"
              + " for efficiency.")
  public YamlConfiguration config_temp;

  public RewardManager reward = new RewardManager();

  public void loadConfigFiles() {
    loadMainConfig();
    loadAdditionalConfigFiles();
  }

  public void loadAdditionalConfigFiles() {
    loadChests();
    loadBlocks();
    loadFlowers();
    loadMessages();
    reward.loadRewards();
  }

  @SuppressFBWarnings(
      value = "ST_WRITE_TO_STATIC_FROM_INSTANCE_METHOD",
      justification =
          "ConfigManager.loadMainConfig() is an instance method that intentionally updates static "
              + "configuration fields. This is part of the plugin's configuration system where the "
              + "ConfigManager instance is responsible for loading and applying configuration to "
              + "static fields. This is a controlled design pattern.")
  public void loadMainConfig() {
    File con = getFile("config.yml");
    Oneblock.config = LowerCaseYaml.loadAndFixConfig(con);

    PluginContext ctx = PluginContext.get();
    if (ctx == null) {
      throw new IllegalStateException("PluginContext not initialized");
    }
    ctx.plugin()
        .setPosition(
            Bukkit.getWorld(readOrDefault("world", "world")),
            (int) readOrDefault("x", (double) Oneblock.getX()),
            (int) readOrDefault("y", (double) Oneblock.getY()),
            (int) readOrDefault("z", (double) Oneblock.getZ()));

    ctx.plugin()
        .setLeave(
            Bukkit.getWorld(readOrDefault("leaveworld", "world")),
            readOrDefault("xleave", .0),
            readOrDefault("yleave", .0),
            readOrDefault("zleave", .0),
            (float) readOrDefault("yawleave", .0));

    // Single-call cache so we don't dereference Oneblock.settings() once per field.
    oneblock.config.Settings s = Oneblock.settings();
    s.progressBar = readOrDefault("progress_bar", true);
    Level.Config levelConfig = Level.getConfig();
    levelConfig.max.color = BarColor.valueOf(readOrDefault("progress_bar_color", "GREEN"));
    levelConfig.max.style = BarStyle.valueOf(readOrDefault("progress_bar_style", "SOLID"));
    s.phText = Utils.translateColorCodes(readOrDefault("progress_bar_text", "level"));
    s.lvlBarMode = s.phText.equals("level");
    s.islandForNewPlayers = readOrDefault("island_for_new_players", true);
    levelConfig.multiplier = readOrDefault("level_multiplier", levelConfig.multiplier);
    Level.multiplier = levelConfig.multiplier;
    s.maxPlayersTeam = readOrDefault("max_players_team", s.maxPlayersTeam);
    s.mobSpawnChance = readOrDefault("mob_spawn_chance", s.mobSpawnChance);
    s.mobSpawnChance = s.mobSpawnChance < 2 ? 9 : s.mobSpawnChance;
    updateBoolParameters();
    OBWorldGuard.setEnabled(readOrDefault("worldguard", OBWorldGuard.canUse));
    OBWorldGuard.flags = readOrDefault("wgflags", OBWorldGuard.flags);
    ctx.plugin().setOffset(readOrDefault("set", 100));
    if (Oneblock.config.isSet("custom_island")) Island.read(Oneblock.config);

    DatabaseConfig();

    LegacyConfigSaver.save(Oneblock.config, con);
  }

  public void updateBoolParameters() {
    oneblock.config.Settings s = Oneblock.settings();
    s.circleMode = readOrDefault("circlemode", s.circleMode);
    s.useEmptyIslands = readOrDefault("useemptyislands", s.useEmptyIslands);
    s.savePlayerInventory = readOrDefault("saveplayerinventory", s.savePlayerInventory);
    s.protection = readOrDefault("protection", s.protection);
    s.autojoin = readOrDefault("autojoin", s.autojoin);
    s.dropTossUp = readOrDefault("droptossup", s.dropTossUp);
    s.physics = readOrDefault("physics", s.physics);
    s.particle = readOrDefault("particle", s.particle);
    s.allowNether = readOrDefault("allow_nether", s.allowNether);
    GUI.getConfig().enabled = readOrDefault("gui", GUI.getConfig().enabled);
    s.rebirth = readOrDefault("rebirth_on_the_island", s.rebirth);
    if (Compat.isBorderSupported) s.border = readOrDefault("border", s.border);
  }

  private void DatabaseConfig() {
    DatabaseManager.Config dbConfig = DatabaseManager.getConfig();
    dbConfig.dbType = readOrDefault("database.type", dbConfig.dbType).toLowerCase(Locale.ROOT);
    dbConfig.host = readOrDefault("database.host", dbConfig.host);
    dbConfig.port = readOrDefault("database.port", dbConfig.port);
    dbConfig.database = readOrDefault("database.name", dbConfig.database);
    dbConfig.username = readOrDefault("database.username", dbConfig.username);
    dbConfig.password = readOrDefault("database.password", dbConfig.password);
    dbConfig.useSSL = readOrDefault("database.useSSL", dbConfig.useSSL);
    dbConfig.autoReconnect = readOrDefault("database.autoReconnect", dbConfig.autoReconnect);
  }

  public void loadBlocks() {
    Level.Config levelConfig = Level.getConfig();
    levelConfig.max.resetPools();
    File block = getFile("blocks.yml");

    // Migrate legacy (cumulative-list) blocks.yml in-place before parsing.
    LegacyBlocksMigrator.migrateBlocks(block, ChestItems.getConfig().chest);

    config_temp = YamlConfiguration.loadConfiguration(block);

    // MaxLevel parsing (unchanged semantics)
    if (config_temp.isString("MaxLevel"))
      levelConfig.max.name = Utils.translateColorCodes(config_temp.getString("MaxLevel"));
    else if (config_temp.isList("MaxLevel"))
      parseLevelFromList(config_temp.getList("MaxLevel"), levelConfig.max, 1);

    List<Level> stagedLevels = new ArrayList<>();

    if (config_temp.isConfigurationSection("levels")) {
      // ---- New schema (Phase 1): string-keyed level map ----
      org.bukkit.configuration.ConfigurationSection levelsSection =
          config_temp.getConfigurationSection("levels");
      if (levelsSection != null) {
        for (String levelId : levelsSection.getKeys(false)) {
          org.bukkit.configuration.ConfigurationSection sec =
              levelsSection.getConfigurationSection(levelId);
          if (sec == null) continue;
          Level level = parseNewLevel(sec, levelId);
          if (level != null) stagedLevels.add(level);
        }
      }
    } else {
      // ---- Legacy schema: integer-indexed lists at root ----
      for (int i = 0; config_temp.isList(String.format("%d", i)); i++) {
        List<?> bl_temp = config_temp.getList(String.format("%d", i));
        if (bl_temp == null || bl_temp.isEmpty()) continue;
        Object first = bl_temp.get(0);
        Level level =
            new Level(
                "level_" + i,
                first instanceof String ? Utils.translateColorCodes((String) first) : "Level " + i);
        stagedLevels.add(level);
        parseLevelFromList(bl_temp, level, i);
      }
    }

    if (levelConfig.max.mobPoolSize() == 0) {
      int totalMobs = 0;
      for (Level lvl : stagedLevels) totalMobs += lvl.mobPoolSize();
      if (totalMobs == 0) {
        PluginContext ctx = PluginContext.get();
        if (ctx != null) {
          ctx.plugin().getLogger().warning("Mobs are not set in the blocks.yml");
        }
      }
    }

    // Load optional legacy_mapping (integer -> string id overrides)
    if (config_temp.isConfigurationSection("legacy_mapping")) {
      org.bukkit.configuration.ConfigurationSection mappingSection =
          config_temp.getConfigurationSection("legacy_mapping");
      if (mappingSection != null) {
        java.util.Map<String, String> raw = new java.util.HashMap<>();
        for (String key : mappingSection.getKeys(false)) {
          raw.put(key, mappingSection.getString(key));
        }
        LegacyLevelMapper.initialize(raw);
      }
    }

    // Publish to both the legacy list and the new registry
    Level.replaceAll(stagedLevels);
    LevelRegistry.replaceAll(stagedLevels);

    setupProgressBar();
  }

  /**
   * Parse a level list (header + pool entries) into the given {@link Level}. Header positions 0..3
   * are name/color/style/length (best-effort, same semantics as the legacy parser). Pool entries
   * beyond the header may be plain strings (legacy, weight=1) or maps with {@code
   * block|mob|loot_table|command} + optional {@code weight}. Unresolved / malformed entries are
   * skipped with a warning.
   */
  void parseLevelFromList(List<?> bl_temp, Level level, int idx) {
    if (bl_temp == null || bl_temp.isEmpty()) return;
    int q = 0;
    if (q < bl_temp.size() && bl_temp.get(q) instanceof String) {
      level.name = Utils.translateColorCodes((String) bl_temp.get(q));
      q++;
    }
    // Duck-type probe: the string at position q may be a BarColor, a BarStyle
    // OR the next header field (length) OR the first pool entry. We attempt
    // each shape in turn; on failure we DO NOT advance q, leaving the string
    // for the next parser. This is legacy config compatibility, not a bug.
    if (q < bl_temp.size() && bl_temp.get(q) instanceof String) {
      try {
        level.color = BarColor.valueOf(((String) bl_temp.get(q)).toUpperCase(Locale.ROOT));
        q++;
      } catch (Exception e) {
        level.color = Level.max.color;
      }
      if (q < bl_temp.size() && bl_temp.get(q) instanceof String) {
        try {
          level.style = BarStyle.valueOf(((String) bl_temp.get(q)).toUpperCase(Locale.ROOT));
          q++;
        } catch (Exception e) {
          level.style = Level.max.style;
        }
      }
    }
    if (q < bl_temp.size()) {
      Object lenItem = bl_temp.get(q);
      if (lenItem instanceof Number) {
        level.length = Math.max(1, ((Number) lenItem).intValue());
        q++;
      } else if (lenItem instanceof String) {
        // Duck-type probe (see above): if the string can't be parsed as an
        // int, it's the first pool-entry token; leave q unchanged so the
        // pool-entry loop below picks it up.
        try {
          level.length = Math.max(1, Integer.parseInt((String) lenItem));
          q++;
        } catch (Exception e) {
          level.length = 16 + idx * Level.multiplier();
        }
      } else {
        level.length = 16 + idx * Level.multiplier();
      }
    }
    while (q < bl_temp.size()) {
      Object raw = bl_temp.get(q++);
      parsePoolEntry(raw, level);
    }
  }

  @SuppressWarnings("unchecked")
  void parsePoolEntry(Object raw, Level level) {
    if (raw == null) return;
    int weight = 1;
    String kind;
    Object payload;

    if (raw instanceof Map) {
      Map<String, Object> m = (Map<String, Object>) raw;
      if (m.containsKey("weight")) {
        Object w = m.get("weight");
        if (w instanceof Number) weight = Math.max(1, ((Number) w).intValue());
        else if (w != null) {
          try {
            weight = Math.max(1, Integer.parseInt(w.toString()));
          } catch (NumberFormatException nfe) {
            PluginContext ctx = PluginContext.get();
            if (ctx != null) {
              ctx.plugin()
                  .getLogger()
                  .warning(
                      "[Oneblock] blocks.yml: non-numeric weight '"
                          + w
                          + "' in entry "
                          + m
                          + "; defaulting to 1.");
            }
          }
        }
      }
      if (m.containsKey("block")) {
        kind = "block";
        payload = m.get("block");
      } else if (m.containsKey("mob")) {
        kind = "mob";
        payload = m.get("mob");
      } else if (m.containsKey("loot_table")) {
        kind = "loot_table";
        payload = m.get("loot_table");
      } else if (m.containsKey("chest")) {
        kind = "chest";
        payload = m.get("chest");
      } else if (m.containsKey("command")) {
        kind = "command";
        payload = m.get("command");
      } else if (m.containsKey("decorated")) {
        kind = "decorated";
        payload = m;
      } else {
        PluginContext ctx = PluginContext.get();
        if (ctx != null) {
          ctx.plugin()
              .getLogger()
              .warning(
                  "[Oneblock] blocks.yml: entry has no recognized kind (expected one of"
                      + " block/mob/loot_table/chest/command/decorated): "
                      + m);
        }
        return;
      }
    } else if (raw instanceof String) {
      String text = (String) raw;
      if (text.isEmpty()) return;
      if (text.charAt(0) == '/') {
        kind = "command";
        payload = text;
      } else if (text.toUpperCase(Locale.ROOT).startsWith("CHEST:")) {
        kind = "chest";
        payload = text.substring(6);
      } else {
        try {
          EntityType.valueOf(text.toUpperCase(Locale.ROOT));
          kind = "mob";
          payload = text.toUpperCase(Locale.ROOT);
        } catch (Exception ignore) {
          kind = "block";
          payload = text;
        }
      }
    } else {
      return;
    }

    if (payload == null) return;
    switch (kind) {
      case "block":
        level.blockPool.add(resolveBlock(payload.toString()), weight);
        break;
      case "mob":
        EntityType et;
        try {
          et = EntityType.valueOf(payload.toString().toUpperCase(Locale.ROOT));
        } catch (Exception e) {
          PluginContext ctx = PluginContext.get();
          if (ctx != null) {
            ctx.plugin()
                .getLogger()
                .warning("[Oneblock] blocks.yml: unknown mob '" + payload + "'");
          }
          return;
        }
        level.mobPool.add(et, weight);
        break;
      case "loot_table":
        NamespacedKey key = ChestItems.parseKey(payload.toString());
        if (key == null) {
          PluginContext ctx = PluginContext.get();
          if (ctx != null) {
            ctx.plugin()
                .getLogger()
                .warning("[Oneblock] blocks.yml: invalid loot table key '" + payload + "'");
          }
          return;
        }
        level.blockPool.add(PoolEntry.lootTable(key), weight);
        break;
      case "chest":
        level.blockPool.add(PoolEntry.chest(payload.toString()), weight);
        break;
      case "command":
        level.blockPool.add(PoolEntry.command(payload.toString()), weight);
        break;
      case "decorated":
        level.blockPool.add(resolveDecorated((Map<String, Object>) payload), weight);
        break;
      default:
        break;
    }
  }

  PoolEntry resolveDecorated(Map<String, Object> m) {
    Object baseObj = m.get("decorated");
    XMaterial base = Compat.GRASS_BLOCK;
    if (baseObj != null) {
      String baseName = baseObj.toString();
      base = XMaterial.matchXMaterial(baseName).orElse(null);
      if (base == null) {
        PluginContext ctx = PluginContext.get();
        if (ctx != null) {
          ctx.plugin()
              .getLogger()
              .warning(
                  "[Oneblock] blocks.yml: decorated base '"
                      + baseName
                      + "' is not a recognised material; falling back to GRASS_BLOCK.");
        }
        base = Compat.GRASS_BLOCK;
      }
    }

    int chance = 3;
    if (m.containsKey("chance")) {
      Object c = m.get("chance");
      if (c instanceof Number) chance = Math.max(0, ((Number) c).intValue());
      else {
        try {
          chance = Math.max(0, Integer.parseInt(c.toString()));
        } catch (NumberFormatException nfe) {
          // keep default
        }
      }
    }

    int offsetY = 1;
    if (m.containsKey("offset_y")) {
      Object o = m.get("offset_y");
      if (o instanceof Number) offsetY = ((Number) o).intValue();
      else {
        try {
          offsetY = Integer.parseInt(o.toString());
        } catch (NumberFormatException nfe) {
          // keep default
        }
      }
    }

    List<XMaterial> decorations = null;
    if (m.containsKey("decorations")) {
      decorations = new ArrayList<>();
      Object d = m.get("decorations");
      if (d instanceof List) {
        for (Object item : (List<?>) d) {
          String decoName = item.toString();
          java.util.Optional<XMaterial> deco = XMaterial.matchXMaterial(decoName);
          if (deco.isPresent()) {
            decorations.add(deco.get());
          } else {
            PluginContext ctx = PluginContext.get();
            if (ctx != null) {
              ctx.plugin()
                  .getLogger()
                  .warning(
                      "[Oneblock] blocks.yml: decoration material '"
                          + decoName
                          + "' is not recognised; skipping.");
            }
          }
        }
      }
      if (decorations.isEmpty()) decorations = null;
    }

    return PoolEntry.decorated(new DecoratedBlock(base, chance, offsetY, decorations));
  }

  /**
   * Resolve a block-name string to a {@link PoolEntry}. Mirrors the legacy resolver chain: Material
   * → custom block (ItemsAdder / Oraxen / Nexo / CraftEngine) → XMaterial (legacy servers).
   * Unresolved names fall back to {@link PoolEntry#GRASS} which renders as grass + chance of flower
   * at runtime.
   */
  PoolEntry resolveBlock(String text) {
    if (text == null || text.isEmpty()) return PoolEntry.GRASS;
    Object mt = Material.matchMaterial(text);
    // Compare against the vanilla Material constant, NOT
    // Compat.GRASS_BLOCK which is an XMaterial: Material vs XMaterial
    // '==' widens to Object and is always false at runtime, silently
    // suppressing the DECORATED_BLOCK routing for every "GRASS_BLOCK"
    // pool entry on modern (1.13+) servers (the placement code at
    // Oneblock.java:248 only adds the 1/3 flower decoration on the
    // DECORATED_BLOCK sentinel, not on a literal Material.GRASS_BLOCK).
    if (mt == null || mt == Material.GRASS_BLOCK || !((Material) mt).isBlock())
      mt = getCustomBlock(text);
    if (mt == null) return PoolEntry.GRASS;
    return PoolEntry.block(mt);
  }

  private Object getCustomBlock(String text) {
    PluginContext ctx = PluginContext.get();
    if (ctx == null) return null;
    switch (ctx.plugin().placetype) {
      case ItemsAdder:
        return CustomBlock.getInstance(text);
      case Oraxen:
        return OraxenItems.exists(text) ? text : null;
      case Nexo:
        return NexoBlocks.isCustomBlock(text) ? text : null;
      case CraftEngine:
        String[] pcid = text.split(":", 2);
        return pcid.length == 2 ? text : null;
      default:
        return null;
    }
  }

  public void setupProgressBar() {
    Level.Config levelConfig = Level.getConfig();
    if (PlayerInfo.size() == 0) return;

    if (levelConfig.max.color == null) levelConfig.max.color = BarColor.GREEN;
    if (levelConfig.max.style == null) levelConfig.max.style = BarStyle.SOLID;

    PlayerInfo.getList()
        .forEach(
            inf -> {
              if (inf.uuid != null) {
                Player p = Bukkit.getPlayer(inf.uuid);
                if (p == null) inf.createBar();
                else inf.createBar(Oneblock.getBarTitle(p, inf));

                inf.bar.setVisible(Oneblock.settings().progressBar);
              }
            });
  }

  private void loadMessages() {
    File message = getFile("messages.yml");
    config_temp = YamlConfiguration.loadConfiguration(message);

    Messages.Config msgConfig = Messages.getConfig();
    msgConfig.help = checkMessage("help", msgConfig.help);
    msgConfig.help_adm = checkMessage("help_adm", msgConfig.help_adm);
    msgConfig.invite_usage = checkMessage("invite_usage", msgConfig.invite_usage);
    msgConfig.invite_yourself = checkMessage("invite_yourself", msgConfig.invite_yourself);
    msgConfig.invite_no_island = checkMessage("invite_no_island", msgConfig.invite_no_island);
    msgConfig.invite_team = checkMessage("invite_team", msgConfig.invite_team);
    msgConfig.invited = checkMessage("invited", msgConfig.invited);
    msgConfig.invited_success = checkMessage("invited_success", msgConfig.invited_success);
    msgConfig.kicked = checkMessage("kicked", msgConfig.kicked);
    msgConfig.kick_usage = checkMessage("kick_usage", msgConfig.kick_usage);
    msgConfig.kick_yourself = checkMessage("kick_yourself", msgConfig.kick_yourself);
    msgConfig.accept_success = checkMessage("accept_success", msgConfig.accept_success);
    msgConfig.accept_none = checkMessage("accept_none", msgConfig.accept_none);
    msgConfig.idreset = checkMessage("idreset", msgConfig.idreset);
    msgConfig.protection = checkMessage("protection", msgConfig.protection);
    msgConfig.leave_not_set = checkMessage("leave_not_set", msgConfig.leave_not_set);
    msgConfig.not_allow_visit = checkMessage("not_allow_visit", msgConfig.not_allow_visit);
    msgConfig.allowed_visit = checkMessage("allowed_visit", msgConfig.allowed_visit);
    msgConfig.forbidden_visit = checkMessage("forbidden_visit", msgConfig.forbidden_visit);

    File gui = getFile("gui.yml");
    config_temp = YamlConfiguration.loadConfiguration(gui);

    msgConfig.baseGUI = checkMessage("baseGUI", msgConfig.baseGUI);
    msgConfig.acceptGUI = checkMessage("acceptGUI", msgConfig.acceptGUI);
    msgConfig.acceptGUIignore = checkMessage("acceptGUIignore", msgConfig.acceptGUIignore);
    msgConfig.acceptGUIjoin = checkMessage("acceptGUIjoin", msgConfig.acceptGUIjoin);
    msgConfig.topGUI = checkMessage("topGUI", msgConfig.topGUI);
    msgConfig.visitGUI = checkMessage("visitGUI", msgConfig.visitGUI);
    msgConfig.idresetGUI = checkMessage("idresetGUI", msgConfig.idresetGUI);
  }

  private String checkMessage(String name, String def_message) {
    if (config_temp.isString(name)) return Utils.translateColorCodes(config_temp.getString(name));
    return def_message;
  }

  private void loadFlowers() {
    PluginContext ctx = PluginContext.get();
    if (ctx == null) return;
    ctx.plugin().flowers.clear();
    File flower = getFile("flowers.yml");
    config_temp = YamlConfiguration.loadConfiguration(flower);
    ctx.plugin().flowers.add(Compat.GRASS);
    for (String list : config_temp.getStringList("flowers"))
      ctx.plugin().flowers.add(XMaterial.matchXMaterial(list).orElse(Compat.GRASS));
  }

  private void loadChests() {
    ChestItems.getConfig().chest = getFile("chests.yml");
    LegacyBlocksMigrator.migrateChests(ChestItems.getConfig().chest);
    ChestItems.load();
  }

  Level parseNewLevel(org.bukkit.configuration.ConfigurationSection sec, String levelId) {
    Level.Config levelConfig = Level.getConfig();
    Level level = new Level(levelId, Utils.translateColorCodes(sec.getString("name", levelId)));

    String colorName = sec.getString("color");
    if (colorName != null) {
      try {
        level.color = BarColor.valueOf(colorName.toUpperCase(Locale.ROOT));
      } catch (Exception e) {
        level.color = levelConfig.max.color;
      }
    } else {
      level.color = levelConfig.max.color;
    }
    String styleName = sec.getString("style");
    if (styleName != null) {
      try {
        level.style = BarStyle.valueOf(styleName.toUpperCase(Locale.ROOT));
      } catch (Exception e) {
        level.style = levelConfig.max.style;
      }
    } else {
      level.style = levelConfig.max.style;
    }

    level.length = Math.max(1, sec.getInt("length", 16));

    // Parse next_themes
    List<String> nextThemes = sec.getStringList("next_themes");
    if (nextThemes != null && !nextThemes.isEmpty()) {
      level.nextThemes.addAll(nextThemes);
    }

    // Parse tasks
    List<Map<?, ?>> taskList = sec.getMapList("tasks");
    java.util.Set<String> groups = new java.util.HashSet<>();
    for (Map<?, ?> m : taskList) {
      try {
        String id = (String) m.get("id");
        TaskType type = TaskType.valueOf(((String) m.get("type")).toUpperCase(Locale.ROOT));
        String target = (String) m.get("target");
        int amount = ((Number) m.get("amount")).intValue();
        String group = (String) m.get("group");
        if (group == null || group.isEmpty()) group = "default";
        level.tasks.add(new LevelTask(id, type, target, amount, group));
        groups.add(group);
      } catch (Exception e) {
        PluginContext ctx = PluginContext.get();
        if (ctx != null) {
          ctx.plugin()
              .getLogger()
              .warning("[Oneblock] blocks.yml: invalid task entry in '" + levelId + "': " + m);
        }
      }
    }

    // Parse completion requirement (boolean expression over task groups)
    String completionReq = sec.getString("completion_requirement");
    try {
      level.completionExpr = CompletionExprParser.parse(completionReq, groups);
    } catch (IllegalArgumentException e) {
      PluginContext ctx = PluginContext.get();
      if (ctx != null) {
        ctx.plugin()
            .getLogger()
            .warning(
                "[Oneblock] blocks.yml: invalid completion_requirement for level '"
                    + levelId
                    + "', defaulting to ANY: "
                    + e.getMessage());
      }
      level.completionExpr = CompletionExprParser.parse(null, groups);
    }

    // Parse pool entries
    List<?> pool = sec.getList("pool");
    if (pool != null) {
      for (Object raw : pool) {
        parsePoolEntry(raw, level);
      }
    }

    return level;
  }

  File getFile(String name) {
    PluginContext ctx = PluginContext.get();
    if (ctx == null) {
      throw new IllegalStateException("PluginContext not initialized");
    }
    File file = new File(ctx.dataFolder(), name);
    if (!file.exists()) ctx.plugin().saveResource(name, false);
    return file;
  }

  /**
   * The canonical {@code config.yml} {@link File} inside the plugin data folder. Callers that need
   * to persist changes via {@link LegacyConfigSaver#save} (e.g. the admin-command path in {@code
   * CommandHandler}) use this instead of the hidden static {@code LegacyConfigSaver.file} field
   * that Phase 3 removed.
   */
  public File getMainConfigFile() {
    return getFile("config.yml");
  }

  String readOrDefault(String type, String data) {
    if (!Oneblock.config.isString(type)) Oneblock.config.set(type, data);
    return Oneblock.config.getString(type);
  }

  int readOrDefault(String type, int data) {
    if (!Oneblock.config.isInt(type)) Oneblock.config.set(type, data);
    return Oneblock.config.getInt(type);
  }

  double readOrDefault(String type, double data) {
    if (!Oneblock.config.isDouble(type)) Oneblock.config.set(type, data);
    return Oneblock.config.getDouble(type);
  }

  boolean readOrDefault(String type, boolean data) {
    if (!Oneblock.config.isBoolean(type)) Oneblock.config.set(type, data);
    return Oneblock.config.getBoolean(type);
  }

  List<String> readOrDefault(String type, List<String> data) {
    if (!Oneblock.config.isList(type)) Oneblock.config.set(type, data);
    return Oneblock.config.getStringList(type);
  }
}
