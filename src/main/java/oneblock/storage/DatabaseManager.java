package oneblock.storage;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.lang.reflect.Type;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import oneblock.LevelRegistry;
import oneblock.PlayerInfo;
import oneblock.context.PluginContext;
import oneblock.migration.LegacyLevelMapper;

public class DatabaseManager {
  private static HikariDataSource dataSource;
  private static final Gson gson = new GsonBuilder().create();
  private static final Type taskProgressType = new TypeToken<Map<String, Integer>>() {}.getType();

  // Allow-lists to prevent JDBC URL parameter injection via config values.
  private static final Pattern HOST_PATTERN = Pattern.compile("^[A-Za-z0-9._\\-]{1,255}$");
  private static final Pattern DB_NAME_PATTERN = Pattern.compile("^[A-Za-z0-9_]{1,64}$");

  public static class Config {
    public String dbType = "json"; // json, h2, mysql
    public String host = "localhost";
    public int port = 3306;
    public String database = "oneblock";
    public String username = "root";
    public String password = "";
    public boolean useSSL = false;
    public boolean autoReconnect = true;
  }

  private static Config config = new Config();

  public static Config getConfig() {
    return config;
  }

  public static void setConfig(Config newConfig) {
    config = newConfig;
  }

  @SuppressFBWarnings(
      value = "REC_CATCH_EXCEPTION",
      justification =
          "Catching Exception is intentional here because HikariDataSource constructor can throw"
              + " various runtime exceptions (SQLException, RuntimeException, etc.) during"
              + " initialization. We want to catch all exceptions to gracefully fall back to JSON"
              + " storage. The exception is logged and the dataSource is set to null to trigger"
              + " fallback.")
  public static void initialize() {
    if ("json".equals(config.dbType)) {
      PluginContext ctx = PluginContext.get();
      if (ctx != null) {
        ctx.plugin().getLogger().info("Database usage is turned off in the configuration");
        ctx.plugin().getLogger().info("Using JSON storage");
      }
      return;
    }

    try {
      HikariConfig hikariConfig = new HikariConfig();

      if ("mysql".equals(config.dbType)) {
        if (!HOST_PATTERN.matcher(config.host).matches()) {
          throw new IllegalArgumentException(
              "Invalid database.host value; expected hostname / IP matching "
                  + HOST_PATTERN.pattern());
        }
        if (config.port <= 0 || config.port > 65535) {
          throw new IllegalArgumentException("Invalid database.port value: " + config.port);
        }
        if (!DB_NAME_PATTERN.matcher(config.database).matches()) {
          throw new IllegalArgumentException(
              "Invalid database.name value; expected schema name matching "
                  + DB_NAME_PATTERN.pattern());
        }

        // Build URL without user-controlled query params; pass options via DataSource properties
        // below.
        hikariConfig.setJdbcUrl(
            String.format("jdbc:mysql://%s:%d/%s", config.host, config.port, config.database));
        hikariConfig.setDriverClassName("com.mysql.cj.jdbc.Driver");
        // Harden the driver against known JDBC gadget vectors.
        hikariConfig.addDataSourceProperty("useSSL", String.valueOf(config.useSSL));
        hikariConfig.addDataSourceProperty("autoReconnect", String.valueOf(config.autoReconnect));
        hikariConfig.addDataSourceProperty("autoDeserialize", "false");
        hikariConfig.addDataSourceProperty("allowLoadLocalInfile", "false");
        hikariConfig.addDataSourceProperty("allowUrlInLocalInfile", "false");
        hikariConfig.addDataSourceProperty("allowLoadLocalInfileInPath", "");
        hikariConfig.addDataSourceProperty("allowPublicKeyRetrieval", "false");
        if (!config.useSSL) {
          PluginContext ctx = PluginContext.get();
          if (ctx != null) {
            ctx.plugin()
                .getLogger()
                .warning(
                    "database.useSSL is false; MySQL credentials and data will traverse the network"
                        + " in plaintext.");
          }
        }
      } else { // h2
        PluginContext ctx = PluginContext.get();
        if (ctx == null) {
          throw new IllegalStateException("PluginContext not initialized");
        }
        String h2Path = ctx.dataFolder().getAbsolutePath() + "/PlData";
        if (h2Path.indexOf(';') >= 0) {
          throw new IllegalArgumentException(
              "Plugin data folder path contains ';' which is unsafe for the H2 JDBC URL: "
                  + h2Path);
        }
        hikariConfig.setJdbcUrl("jdbc:h2:" + h2Path);
        hikariConfig.setDriverClassName("org.h2.Driver");
      }

      hikariConfig.setUsername(config.username);
      hikariConfig.setPassword(config.password);
      hikariConfig.setMaximumPoolSize(10);
      hikariConfig.setConnectionTimeout(30000);
      hikariConfig.addDataSourceProperty("cachePrepStmts", "true");
      hikariConfig.addDataSourceProperty("prepStmtCacheSize", "250");
      hikariConfig.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");

      dataSource = new HikariDataSource(hikariConfig);

      PluginContext ctx = PluginContext.get();
      if (ctx != null) {
        ctx.plugin().getLogger().info("Database initialized successfully (" + config.dbType + ")");
      }
    } catch (Exception e) {
      PluginContext ctx = PluginContext.get();
      if (ctx != null) {
        ctx.plugin().getLogger().log(Level.SEVERE, "Failed to initialize Database", e);
        ctx.plugin().getLogger().info("Using JSON storage");
      }
      dataSource = null;
    }

    createTable();
  }

  private static void createTable() {
    if (!isConnected()) return;

    String sql =
        "CREATE TABLE IF NOT EXISTS player_data ("
            + "island_id INT PRIMARY KEY, "
            + "uuid VARCHAR(36) NULL, "
            + "level INT NOT NULL DEFAULT 1, "
            + "breaks INT NOT NULL DEFAULT 0, "
            + "allow_visit BOOLEAN DEFAULT FALSE, "
            + "invited_players TEXT"
            + ")";

    try (Connection conn = dataSource.getConnection();
        Statement stmt = conn.createStatement()) {
      stmt.execute(sql);
    } catch (SQLException e) {
      PluginContext ctx = PluginContext.get();
      if (ctx != null) {
        ctx.plugin().getLogger().log(Level.SEVERE, "Failed to create table", e);
      }
    }

    addColumnIfMissing("current_level_id", "VARCHAR(64)");
    addColumnIfMissing("task_progress", "TEXT");
  }

  @SuppressFBWarnings(
      value = "SQL_INJECTION_JDBC",
      justification =
          "Schema migration SQL uses hardcoded internal column names and definitions only.")
  private static void addColumnIfMissing(String columnName, String columnDef) {
    if (!isConnected()) return;
    try (Connection conn = dataSource.getConnection()) {
      DatabaseMetaData meta = conn.getMetaData();
      try (ResultSet rs =
          meta.getColumns(null, null, "PLAYER_DATA", columnName.toUpperCase(Locale.ROOT))) {
        if (rs.next()) return;
      }
      try (ResultSet rs = meta.getColumns(null, null, "player_data", columnName)) {
        if (rs.next()) return;
      }
      try (Statement stmt = conn.createStatement()) {
        stmt.execute("ALTER TABLE player_data ADD COLUMN " + columnName + " " + columnDef);
      }
    } catch (SQLException e) {
      PluginContext ctx = PluginContext.get();
      if (ctx != null) {
        ctx.plugin().getLogger().log(Level.SEVERE, "Failed to add column " + columnName, e);
      }
    }
  }

  public static List<PlayerInfo> load() {
    List<PlayerInfo> players = new ArrayList<>();
    if (!isConnected()) return players;

    String sql =
        "SELECT island_id, uuid, level, breaks, current_level_id, task_progress, allow_visit,"
            + " invited_players FROM player_data ORDER BY island_id";

    try (Connection conn = dataSource.getConnection();
        Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery(sql)) {

      while (rs.next()) {
        int islandId = rs.getInt("island_id");
        String uuidStr = rs.getString("uuid");
        UUID uuid = parseUuid(uuidStr, "owner UUID for island " + islandId);
        PlayerInfo player = new PlayerInfo(uuid);

        if (uuid != null) {
          String currentLevelId = rs.getString("current_level_id");
          String taskProgressJson = rs.getString("task_progress");
          int legacyLvl = rs.getInt("level");
          int legacyBreaks = rs.getInt("breaks");

          if (currentLevelId != null
              && !currentLevelId.isEmpty()
              && LevelRegistry.contains(currentLevelId)) {
            player.currentLevelId = currentLevelId;
          } else {
            player.lvl = legacyLvl;
            player.currentLevelId = LegacyLevelMapper.fromInt(legacyLvl);
          }

          if (taskProgressJson != null && !taskProgressJson.isEmpty()) {
            player.taskProgress.loadSnapshot(parseTaskProgress(taskProgressJson));
            oneblock.Level level = LevelRegistry.get(player.currentLevelId);
            if (level != null) player.taskProgress.recomputeCompletion(level);
          } else {
            player.breaks = legacyBreaks;
          }

          player.syncLegacyFields();
          player.allowVisit = rs.getBoolean("allow_visit");

          String invitedStr = rs.getString("invited_players");
          if (invitedStr != null && !invitedStr.trim().isEmpty()) {
            player.uuids.addAll(
                Arrays.stream(invitedStr.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .map(s -> parseUuid(s, "invited UUID for island " + islandId))
                    .filter(java.util.Objects::nonNull)
                    .collect(Collectors.toList()));
          }
        }
        players.add(player);
      }

      PluginContext ctx = PluginContext.get();
      if (ctx != null) {
        ctx.plugin().getLogger().info("Loaded " + players.size() + " island slots from database");
      }
    } catch (SQLException e) {
      PluginContext ctx = PluginContext.get();
      if (ctx != null) {
        ctx.plugin().getLogger().log(Level.SEVERE, "Failed to load player data from Database", e);
      }
    }

    return players;
  }

  public static boolean save(List<PlayerInfo> players) {
    if (!isConnected() || players == null) return false;

    String upsertSQL;
    if ("h2".equals(config.dbType)) {
      upsertSQL =
          "MERGE INTO player_data (island_id, uuid, level, breaks, current_level_id, task_progress,"
              + " allow_visit, invited_players) KEY (island_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
    } else {
      upsertSQL =
          "INSERT INTO player_data (island_id, uuid, level, breaks, current_level_id,"
              + " task_progress, allow_visit, invited_players) VALUES (?, ?, ?, ?, ?, ?, ?, ?) ON"
              + " DUPLICATE KEY UPDATE uuid = VALUES(uuid), level = VALUES(level), breaks ="
              + " VALUES(breaks), current_level_id = VALUES(current_level_id), task_progress ="
              + " VALUES(task_progress), allow_visit = VALUES(allow_visit), invited_players ="
              + " VALUES(invited_players)";
    }

    try (Connection conn = dataSource.getConnection();
        PreparedStatement pstmt = conn.prepareStatement(upsertSQL)) {

      conn.setAutoCommit(false);
      int savedCount = 0;

      for (int i = 0; i < players.size(); i++) {
        PlayerInfo player = players.get(i);
        if (player == null) continue;

        pstmt.setInt(1, i); // island_id

        if (player.uuid != null) {
          pstmt.setString(2, player.uuid.toString());
        } else {
          pstmt.setNull(2, java.sql.Types.VARCHAR);
        }

        pstmt.setInt(3, player.lvl);
        pstmt.setInt(4, player.breaks);
        pstmt.setString(5, player.currentLevelId);
        pstmt.setString(6, formatTaskProgress(player.taskProgress.snapshot()));
        pstmt.setBoolean(7, player.allowVisit);

        String invitedStr =
            player.uuids.stream().map(UUID::toString).collect(Collectors.joining(","));
        pstmt.setString(8, invitedStr.isEmpty() ? null : invitedStr);

        pstmt.addBatch();
        savedCount++;

        if (savedCount % 100 == 0) {
          pstmt.executeBatch();
        }
      }

      if (savedCount > 0) {
        pstmt.executeBatch();
      }
      conn.commit();

      PluginContext ctx = PluginContext.get();
      if (ctx != null) {
        ctx.plugin().getLogger().info("Saved " + savedCount + " islands to database");
      }
      return true;

    } catch (SQLException e) {
      PluginContext ctx = PluginContext.get();
      if (ctx != null) {
        ctx.plugin().getLogger().log(Level.SEVERE, "Failed to save player data", e);
      }
      return false;
    }
  }

  public static boolean isConnected() {
    return dataSource != null && !dataSource.isClosed();
  }

  private static Map<String, Integer> parseTaskProgress(String json) {
    if (json == null || json.isEmpty()) return new HashMap<>();
    try {
      Map<String, Integer> result = gson.fromJson(json, taskProgressType);
      return result != null ? result : new HashMap<>();
    } catch (Exception e) {
      PluginContext ctx = PluginContext.get();
      if (ctx != null) {
        ctx.plugin().getLogger().warning("Failed to parse task progress JSON: " + e.getMessage());
      }
      return new HashMap<>();
    }
  }

  private static UUID parseUuid(String raw, String context) {
    if (raw == null || raw.trim().isEmpty()) return null;
    try {
      return UUID.fromString(raw.trim());
    } catch (IllegalArgumentException e) {
      PluginContext ctx = PluginContext.get();
      if (ctx != null) {
        ctx.plugin().getLogger().warning("Skipping malformed " + context + ": " + raw);
      }
      return null;
    }
  }

  private static String formatTaskProgress(Map<String, Integer> snapshot) {
    if (snapshot == null || snapshot.isEmpty()) return null;
    return gson.toJson(snapshot, taskProgressType);
  }

  public static void close() {
    if (isConnected()) dataSource.close();
  }
}
