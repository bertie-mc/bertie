package io.github.bertie_mc.bertieprogression.fountain;

import static org.junit.jupiter.api.Assertions.*;

import java.util.EnumMap;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.Blocks;
import org.junit.jupiter.api.Test;

class FountainRulesTest {
    @Test
    void successfulSpecialRollNeverAttemptsALaterStage() {
        AtomicInteger rolls = new AtomicInteger();
        assertEquals(FountainRules.Result.CORE, FountainRules.roll(1, 1, () -> {
            assertEquals(1, rolls.incrementAndGet());
            return 0;
        }));
        assertEquals(1, rolls.get());
        assertEquals(FountainRules.Result.ORE, FountainRules.roll(0, 0, () -> 0.49));
        assertEquals(FountainRules.Result.STURDY, FountainRules.roll(0, 0, () -> 0.5));
    }

    @Test
    void measuredDistributionMatchesTheConditionalCascade() {
        Random random = new Random(76133);
        var counts = new EnumMap<FountainRules.Result, Integer>(FountainRules.Result.class);
        int samples = 400000;
        for (int i = 0; i < samples; i++)
            counts.merge(FountainRules.roll(0.07, 0.04, random::nextDouble), 1, Integer::sum);
        assertEquals(0.07, counts.get(FountainRules.Result.CORE) / (double) samples, 0.002);
        assertEquals(0.93 * 0.04, counts.get(FountainRules.Result.VOID) / (double) samples, 0.002);
        assertEquals(0.93 * 0.96 * 0.5, counts.get(FountainRules.Result.ORE) / (double) samples, 0.002);
        assertEquals(0.93 * 0.96 * 0.5, counts.get(FountainRules.Result.STURDY) / (double) samples, 0.002);
    }

    @Test
    void oreAndSpecialBonusesHaveDifferentUnitsAndAreAdditive() {
        assertEquals(5, FountainRules.oreWeight(2, 2));
        assertEquals(0.07, FountainRules.specialChance(0.01, 2), 0.000001);
        assertEquals(1, FountainRules.specialChance(0.99, 4));
    }

    @Test
    void neighborCacheDoesOnlyFourReadsPerRefresh() {
        FountainNeighbors cache = new FountainNeighbors();
        AtomicInteger reads = new AtomicInteger();
        var reader = (java.util.function.Function<
                        net.minecraft.core.Direction, net.minecraft.world.level.block.state.BlockState>)
                direction -> {
                    reads.incrementAndGet();
                    return Blocks.STONE.defaultBlockState();
                };
        for (int tick = 100; tick < 300; tick++) cache.get(tick, reader);
        assertEquals(4, reads.get());
        assertTrue(FountainNeighbors.common(cache.get(299, reader)).is(Blocks.STONE));
        cache.get(300, reader);
        assertEquals(8, reads.get());
        cache.get(0, reader);
        assertEquals(12, reads.get());
    }

    @Test
    void nativeSturdyHardnessAndLootLookupAreActuallyOverridden() {
        var sturdy = BuiltInRegistries.BLOCK.get(SturdyDeepslateRules.ID);
        assertFalse(sturdy.defaultBlockState().isAir());
        assertEquals(
                ResourceLocation.parse("anvilcraft:blocks/sturdy_deepslate"),
                sturdy.getLootTable().location());
        float nativeHardness = Blocks.REINFORCED_DEEPSLATE
                .defaultBlockState()
                .getDestroySpeed(EmptyBlockGetter.INSTANCE, BlockPos.ZERO);
        float newHardness = sturdy.defaultBlockState().getDestroySpeed(EmptyBlockGetter.INSTANCE, BlockPos.ZERO);
        assertEquals(1.4 / nativeHardness / 30, 9.0 / newHardness / 30, 0.00000001);
        assertEquals(55, nativeHardness);
    }
}
