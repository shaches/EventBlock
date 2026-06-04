package oneblock.storage;

import java.util.List;
import oneblock.PlayerInfo;

/**
 * Shared interface for player data persistence implementations. Phase 5: Extracted to enable
 * dependency injection and testability.
 */
public interface PlayerDataStore {
  /**
   * Write all player info records to persistent storage.
   *
   * @param pls List of PlayerInfo to persist
   */
  void write(List<PlayerInfo> pls);

  /**
   * Read all player info records from persistent storage.
   *
   * @return List of PlayerInfo, or empty list if storage is unavailable/empty
   */
  List<PlayerInfo> read();
}
