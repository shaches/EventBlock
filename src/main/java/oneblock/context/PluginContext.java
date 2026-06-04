package oneblock.context;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import oneblock.Messages;
import oneblock.Oneblock;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Simple dependency injection context for EventBlock. Holds references to core plugin components
 * that were previously static global state. This enables testability by allowing test code to
 * inject mock instances without reflection.
 *
 * <p>Phase 3 introduction: replaces the pattern of {@code Oneblock.plugin} static access with a
 * context that can be swapped in tests. The context is set during plugin initialization and
 * accessed via {@link #get()}.
 */
public final class PluginContext {
  private static volatile PluginContext instance;

  private final Oneblock plugin;
  private final DataFolderProvider dataFolderProvider;
  private final MessagesProvider messagesProvider;

  private PluginContext(
      Oneblock plugin, DataFolderProvider dataFolderProvider, MessagesProvider messagesProvider) {
    this.plugin = plugin;
    this.dataFolderProvider = dataFolderProvider;
    this.messagesProvider = messagesProvider;
  }

  /**
   * Initialize the context with the plugin instance. Called once during {@link
   * JavaPlugin#onEnable()}.
   */
  public static void initialize(Oneblock plugin) {
    if (instance != null) {
      throw new IllegalStateException("PluginContext already initialized");
    }
    instance = new PluginContext(plugin, plugin::getDataFolder, new StaticMessagesProvider());
  }

  /**
   * Get the current context instance. Returns {@code null} if not yet initialized (e.g. during unit
   * tests that don't call {@link #initialize(Oneblock)}).
   */
  @SuppressFBWarnings(
      value = "SING_SINGLETON_GETTER_NOT_SYNCHRONIZED",
      justification =
          "The singleton instance is initialized once during plugin startup on the main thread via"
              + " Oneblock.onEnable() before any concurrent access. The reset() method is only"
              + " called from tests. This initialization pattern is safe because the instance is"
              + " set before any multi-threaded access occurs.")
  public static PluginContext get() {
    return instance;
  }

  /** Reset the context instance. Only for use in tests to clean up between test runs. */
  static void reset() {
    instance = null;
  }

  /** Get the plugin instance. */
  @SuppressFBWarnings(
      value = "EI_EXPOSE_REP",
      justification =
          "PluginContext uses intentional dependency injection pattern. The plugin reference is"
              + " meant to be exposed to provide access to core plugin components. The context is a"
              + " singleton with controlled initialization, making this exposure safe.")
  public Oneblock plugin() {
    return plugin;
  }

  /** Get the plugin's data folder. Used by storage classes to locate data files. */
  public java.io.File dataFolder() {
    return dataFolderProvider.getDataFolder();
  }

  /** Get the messages provider for accessing localized message strings. */
  public MessagesProvider messages() {
    return messagesProvider;
  }

  /** Functional interface for data folder provider to enable test mocking. */
  @FunctionalInterface
  public interface DataFolderProvider {
    java.io.File getDataFolder();
  }

  /**
   * Provider interface for accessing message strings. Phase 3.5: replaces direct static {@link
   * oneblock.Messages} field access with a provider that can be mocked in tests.
   */
  public interface MessagesProvider {
    String help();

    String helpAdm();

    String inviteUsage();

    String inviteYourself();

    String inviteNoIsland();

    String inviteTeam();

    String invited();

    String invitedSuccess();

    String kicked();

    String kickUsage();

    String kickYourself();

    String acceptSuccess();

    String acceptNone();

    String idreset();

    String protection();

    String leaveNotSet();

    String notAllowVisit();

    String allowedVisit();

    String forbiddenVisit();

    String baseGUI();

    String acceptGUI();

    String acceptGUIignore();

    String acceptGUIjoin();

    String topGUI();

    String visitGUI();

    String idresetGUI();

    String boolFormat();

    String invalidValue();
  }

  /**
   * Default implementation that delegates to the static {@link oneblock.Messages} fields. This is a
   * transitional implementation during Phase 3.5; future phases will make Messages non-static and
   * hold the message data directly in this provider.
   */
  private static class StaticMessagesProvider implements MessagesProvider {
    @Override
    public String help() {
      return Messages.help();
    }

    @Override
    public String helpAdm() {
      return Messages.help_adm();
    }

    @Override
    public String inviteUsage() {
      return Messages.invite_usage();
    }

    @Override
    public String inviteYourself() {
      return Messages.invite_yourself();
    }

    @Override
    public String inviteNoIsland() {
      return Messages.invite_no_island();
    }

    @Override
    public String inviteTeam() {
      return Messages.invite_team();
    }

    @Override
    public String invited() {
      return Messages.invited();
    }

    @Override
    public String invitedSuccess() {
      return Messages.invited_success();
    }

    @Override
    public String kicked() {
      return Messages.kicked();
    }

    @Override
    public String kickUsage() {
      return Messages.kick_usage();
    }

    @Override
    public String kickYourself() {
      return Messages.kick_yourself();
    }

    @Override
    public String acceptSuccess() {
      return Messages.accept_success();
    }

    @Override
    public String acceptNone() {
      return Messages.accept_none();
    }

    @Override
    public String idreset() {
      return Messages.idreset();
    }

    @Override
    public String protection() {
      return Messages.protection();
    }

    @Override
    public String leaveNotSet() {
      return Messages.leave_not_set();
    }

    @Override
    public String notAllowVisit() {
      return Messages.not_allow_visit();
    }

    @Override
    public String allowedVisit() {
      return Messages.allowed_visit();
    }

    @Override
    public String forbiddenVisit() {
      return Messages.forbidden_visit();
    }

    @Override
    public String baseGUI() {
      return Messages.baseGUI();
    }

    @Override
    public String acceptGUI() {
      return Messages.acceptGUI();
    }

    @Override
    public String acceptGUIignore() {
      return Messages.acceptGUIignore();
    }

    @Override
    public String acceptGUIjoin() {
      return Messages.acceptGUIjoin();
    }

    @Override
    public String topGUI() {
      return Messages.topGUI();
    }

    @Override
    public String visitGUI() {
      return Messages.visitGUI();
    }

    @Override
    public String idresetGUI() {
      return Messages.idresetGUI();
    }

    @Override
    public String boolFormat() {
      return Messages.bool_format;
    }

    @Override
    public String invalidValue() {
      return Messages.invalid_value;
    }
  }
}
