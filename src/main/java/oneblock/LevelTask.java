package oneblock;

/**
 * Immutable descriptor for a single task within a level.
 *
 * <p>Tasks are grouped by {@code group}. All tasks in the same group are AND-ed (all must be
 * satisfied); completion of any group satisfies the level (OR logic across groups).
 */
public final class LevelTask {
  public final String id;
  public final TaskType type;
  public final String target;
  public final int amount;
  public final String group;

  public LevelTask(String id, TaskType type, String target, int amount, String group) {
    if (id == null || id.isEmpty()) throw new IllegalArgumentException("task id must not be empty");
    if (type == null) throw new IllegalArgumentException("task type must not be null");
    if (target == null || target.isEmpty())
      throw new IllegalArgumentException("task target must not be empty");
    if (amount <= 0) throw new IllegalArgumentException("task amount must be positive");
    if (group == null || group.isEmpty())
      throw new IllegalArgumentException("task group must not be empty");
    this.id = id;
    this.type = type;
    this.target = target;
    this.amount = amount;
    this.group = group;
  }

  @Override
  public String toString() {
    return "LevelTask{id='"
        + id
        + "', type="
        + type
        + ", target='"
        + target
        + "', amount="
        + amount
        + ", group='"
        + group
        + "'}";
  }
}
