package oneblock;

import java.util.Set;

/**
 * Boolean expression evaluated against a set of completed task-group names.
 *
 * <p>Used by {@link Level#completionExpr} to decide whether a player has satisfied the requirements
 * of a level. The expression is parsed once at config-load time and evaluated at runtime against
 * the live set of groups whose individual tasks are all complete.
 */
public interface CompletionExpr {
  /**
   * @return true if the expression is satisfied by the given completed groups
   */
  boolean evaluate(Set<String> completedGroups);
}
