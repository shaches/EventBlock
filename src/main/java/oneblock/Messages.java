package oneblock;

import org.bukkit.ChatColor;

public final class Messages {
  public static class Config {
    public String help = "none";
    public String help_adm = "none";
    public String invite_usage = String.format("%sUsage: /ob invite <username>", ChatColor.RED);
    public String invite_yourself = String.format("%sYou can't invite yourself.", ChatColor.YELLOW);
    public String invite_no_island =
        String.format("%sPlease create an island before you do this.", ChatColor.YELLOW);
    public String invite_team =
        String.format("%sMaximum number of seats on the island: %s", ChatColor.YELLOW, "%d");
    public String invited =
        String.format(
            "%sYou were invited by player %s.%n%s/ob accept to accept).",
            ChatColor.GREEN, "%s", ChatColor.RED);
    public String invited_success =
        String.format("%sSuccessfully invited %s.", ChatColor.GREEN, "%s");
    public String kicked = String.format("%s has been kicked off your island!", ChatColor.YELLOW);
    public String kick_usage = String.format("%sUsage: /ob kick <username>", ChatColor.RED);
    public String kick_yourself = String.format("%sYou can't kick yourself.", ChatColor.YELLOW);
    public String accept_success =
        String.format("%sSuccessfully accepted the invitation.", ChatColor.GREEN);
    public String accept_none =
        String.format("%s[There is no Pending invitations for you.]", ChatColor.RED);
    public String idreset =
        String.format(
            "%sNow your data has been reset. You can create a new island /ob join.",
            ChatColor.GREEN);
    public String protection =
        String.format(
            "%sare you trying to go %soutside the island?", ChatColor.YELLOW, ChatColor.RED);
    public String leave_not_set =
        String.format("%sSorry, but the position was not set.", ChatColor.YELLOW);
    public String not_allow_visit =
        String.format("%sThe player did not allow visits.", ChatColor.YELLOW);
    public String allowed_visit =
        String.format("%sYou have allowed other players to visit your island!", ChatColor.GREEN);
    public String forbidden_visit =
        String.format(
            "%sYou have forbidden other players from visiting your island!", ChatColor.YELLOW);

    // GUI
    public String baseGUI = "";
    public String acceptGUI = "", acceptGUIignore = "", acceptGUIjoin = "%s";
    public String topGUI = "";
    public String visitGUI = "";
    public String idresetGUI = "";
  }

  private static Config config = new Config();

  public static Config getConfig() {
    return config;
  }

  public static void setConfig(Config newConfig) {
    config = newConfig;
  }

  // Legacy static field accessors for backward compatibility during Phase 3 migration
  public static final String bool_format =
      String.format("%sEnter a valid value true or false", ChatColor.YELLOW);
  public static final String invalid_value = String.format("%sinvalid value", ChatColor.RED);

  public static String help() {
    return config.help;
  }

  public static String help_adm() {
    return config.help_adm;
  }

  public static String invite_usage() {
    return config.invite_usage;
  }

  public static String invite_yourself() {
    return config.invite_yourself;
  }

  public static String invite_no_island() {
    return config.invite_no_island;
  }

  public static String invite_team() {
    return config.invite_team;
  }

  public static String invited() {
    return config.invited;
  }

  public static String invited_success() {
    return config.invited_success;
  }

  public static String kicked() {
    return config.kicked;
  }

  public static String kick_usage() {
    return config.kick_usage;
  }

  public static String kick_yourself() {
    return config.kick_yourself;
  }

  public static String accept_success() {
    return config.accept_success;
  }

  public static String accept_none() {
    return config.accept_none;
  }

  public static String idreset() {
    return config.idreset;
  }

  public static String protection() {
    return config.protection;
  }

  public static String leave_not_set() {
    return config.leave_not_set;
  }

  public static String not_allow_visit() {
    return config.not_allow_visit;
  }

  public static String allowed_visit() {
    return config.allowed_visit;
  }

  public static String forbidden_visit() {
    return config.forbidden_visit;
  }

  public static String baseGUI() {
    return config.baseGUI;
  }

  public static String acceptGUI() {
    return config.acceptGUI;
  }

  public static String acceptGUIignore() {
    return config.acceptGUIignore;
  }

  public static String acceptGUIjoin() {
    return config.acceptGUIjoin;
  }

  public static String topGUI() {
    return config.topGUI;
  }

  public static String visitGUI() {
    return config.visitGUI;
  }

  public static String idresetGUI() {
    return config.idresetGUI;
  }
}
