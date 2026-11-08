package io.github.bertie_mc.bertieprogression;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.dubhe.anvilcraft.api.world.load.LoadChuckData;
import dev.dubhe.anvilcraft.block.entity.OverseerBlockEntity;
import java.util.Arrays;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import org.junit.jupiter.api.Test;

class OverseerIntegrationTest {
    @Test
    void nativeBaseTiersLoadTheirFullChunkSquares() {
        assertEquals(
                0L,
                Arrays.stream(OverseerBlockEntity.class.getDeclaredMethods())
                        .filter(method -> method.getName().contains("singleChunkRadius"))
                        .count());
        List<BlockPos> positions = List.of(
                BlockPos.ZERO,
                new BlockPos(15, 64, 15),
                new BlockPos(16, 64, 16),
                new BlockPos(-1, 64, -1),
                new BlockPos(-17, 64, 31));
        for (int tier = 0; tier <= 3; tier++) {
            int radius = tier;
            for (BlockPos pos : positions) {
                for (boolean randomTicks : new boolean[] {false, true}) {
                    var load = LoadChuckData.createLoadChuckData(radius, pos, randomTicks, null);
                    assertEquals(
                            (2 * tier + 1) * (2 * tier + 1),
                            load.getChunkPosList().size(),
                            "base tier " + tier);
                    org.junit.jupiter.api.Assertions.assertTrue(
                            load.getChunkPosList().contains(new ChunkPos(pos)));
                    assertEquals(randomTicks, load.isNeedRandomTick());
                }
            }
        }
    }
}
