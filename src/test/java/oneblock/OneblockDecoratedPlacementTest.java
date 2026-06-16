package oneblock;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cryptomorin.xseries.XBlock;
import com.cryptomorin.xseries.XMaterial;
import java.lang.reflect.Method;
import java.util.List;
import org.bukkit.block.Block;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

class OneblockDecoratedPlacementTest {

  @Test
  @DisplayName("placeDecorated uses relative decoration block and configured physics")
  void placeDecoratedUsesRelativeBlockAndSettingsPhysics() throws Exception {
    boolean savedPhysics = Oneblock.settings().physics;
    Oneblock.settings().physics = false;
    try {
      Oneblock plugin = mock(Oneblock.class);
      Block baseBlock = mock(Block.class);
      Block decorationBlock = mock(Block.class);
      when(baseBlock.getRelative(0, 2, 0)).thenReturn(decorationBlock);
      DecoratedBlock decorated =
          new DecoratedBlock(XMaterial.STONE, 1, 2, List.of(XMaterial.RED_MUSHROOM));
      Method method =
          Oneblock.class.getDeclaredMethod("placeDecorated", Block.class, DecoratedBlock.class);
      method.setAccessible(true);

      try (MockedStatic<XBlock> xBlock = mockStatic(XBlock.class)) {
        method.invoke(plugin, baseBlock, decorated);

        xBlock.verify(() -> XBlock.setType(baseBlock, XMaterial.STONE, false));
        verify(baseBlock).getRelative(0, 2, 0);
        xBlock.verify(() -> XBlock.setType(decorationBlock, XMaterial.RED_MUSHROOM, false));
      }
    } finally {
      Oneblock.settings().physics = savedPhysics;
    }
  }
}
