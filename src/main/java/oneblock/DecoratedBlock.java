package oneblock;

import com.cryptomorin.xseries.XMaterial;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;

/**
 * Value payload for {@link PoolEntry.Kind#DECORATED_BLOCK}. Describes a base block material, the
 * probability (1 in {@code chance}) of placing a random decoration above it, the vertical offset of
 * that decoration, and the list of candidate decoration materials.
 *
 * <p>If {@code decorations} is {@code null} the placement code falls back to the global flowers
 * list from {@link PluginContext} at runtime.
 */
@SuppressFBWarnings(
    value = {"EI_EXPOSE_REP", "EI_EXPOSE_REP2"},
    justification =
        "DecoratedBlock is a record used as a data transfer object. It intentionally stores and"
            + " returns the decorations list for read-only access during block generation. The"
            + " record is immutable after construction, and callers are expected not to modify the"
            + " list. This design is safe because the record is short-lived and only used"
            + " internally by the block generation system.")
public record DecoratedBlock(
    XMaterial base, int chance, int offsetY, List<XMaterial> decorations) {}
