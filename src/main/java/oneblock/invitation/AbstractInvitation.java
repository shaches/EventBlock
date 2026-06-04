package oneblock.invitation;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.UUID;

public abstract class AbstractInvitation {
  @SuppressFBWarnings(
      value = "NM_FIELD_NAMING_CONVENTION",
      justification =
          "Field names use PascalCase for consistency with the invitation API. These are public"
              + " fields used throughout the codebase and renaming would be a breaking change. The"
              + " naming convention is intentional for the invitation system.")
  public UUID Inviting;

  @SuppressFBWarnings(
      value = "NM_FIELD_NAMING_CONVENTION",
      justification =
          "Field names use PascalCase for consistency with the invitation API. These are public"
              + " fields used throughout the codebase and renaming would be a breaking change. The"
              + " naming convention is intentional for the invitation system.")
  public UUID Invited;

  public AbstractInvitation(UUID inviting, UUID invited) {
    Inviting = inviting;
    Invited = invited;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof AbstractInvitation) {
      AbstractInvitation inv = (AbstractInvitation) obj;
      return equals(inv.Inviting, inv.Invited);
    }
    return false;
  }

  public boolean equals(UUID inviting, UUID invited) {
    return Inviting.equals(inviting) && Invited.equals(invited);
  }

  @Override
  public int hashCode() {
    // Pair-based hash, consistent with the {@link #equals(Object)} contract
    // above: two invitations are equal iff both endpoint UUIDs match, so the
    // hash must blend both. Phase 3.7 added this so {@code Invitation.list}
    // containment checks work via {@code HashSet}/{@code HashMap} should a
    // future caller need O(1) membership instead of the current
    // {@code ArrayList} scan.
    return java.util.Objects.hash(Inviting, Invited);
  }
}
