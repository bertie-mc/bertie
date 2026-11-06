package io.github.bertie_mc.bertieprogression;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.dubhe.anvilcraft.api.world.load.LoadChuckData;
import dev.dubhe.anvilcraft.block.entity.OverseerBlockEntity;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;
import org.junit.jupiter.api.Test;

class OverseerIntegrationTest {
    @Test
    void everyBaseTierLoadsOnlyTheOverseersOwnChunk() throws Exception {
        var block = BuiltInRegistries.BLOCK.get(ResourceLocation.parse("anvilcraft:overseer"));
        var overseer = new OverseerBlockEntity(BlockPos.ZERO, block.defaultBlockState());
        Method radiusHook = Arrays.stream(OverseerBlockEntity.class.getDeclaredMethods())
                .filter(method -> method.getName().contains("singleChunkRadius"))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Overseer radius mixin was not applied"));
        radiusHook.setAccessible(true);
        List<BlockPos> positions = List.of(
                BlockPos.ZERO,
                new BlockPos(15, 64, 15),
                new BlockPos(16, 64, 16),
                new BlockPos(-1, 64, -1),
                new BlockPos(-17, 64, 31));
        for (int tier = 0; tier <= 3; tier++) {
            int radius = (int) radiusHook.invoke(overseer, tier);
            for (BlockPos pos : positions) {
                for (boolean randomTicks : new boolean[] {false, true}) {
                    var load = LoadChuckData.createLoadChuckData(radius, pos, randomTicks, null);
                    assertEquals(List.of(new ChunkPos(pos)), load.getChunkPosList(), "base tier " + tier);
                    assertEquals(randomTicks, load.isNeedRandomTick());
                }
            }
        }
    }
}
