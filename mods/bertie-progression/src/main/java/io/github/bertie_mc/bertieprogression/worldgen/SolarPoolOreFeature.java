package io.github.bertie_mc.bertieprogression.worldgen;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

public final class SolarPoolOreFeature extends Feature<NoneFeatureConfiguration> {
    public SolarPoolOreFeature() {
        super(NoneFeatureConfiguration.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        if (!context.level().getLevel().dimension().equals(Level.NETHER)) {
            return false;
        }
        BlockState ore = BuiltInRegistries.BLOCK
                .get(ResourceLocation.parse("hazennstuff:solar_core_blackstone_ore"))
                .defaultBlockState();
        if (ore.isAir()) {
            return false;
        }
        WorldGenLevel level = context.level();
        RandomSource random = context.random();
        BlockPos anchor = null;
        int found = 0;
        // Search the pool floors in this column instead of spending attempts inside solid rock.
        for (int y = level.getMaxBuildHeight() - 2; y > 32; y--) {
            BlockPos pos =
                    new BlockPos(context.origin().getX(), y, context.origin().getZ());
            if (host(level.getBlockState(pos))
                    && level.getBiome(pos).is(Biomes.BASALT_DELTAS)
                    && level.getFluidState(pos.above()).is(FluidTags.LAVA)) {
                if (random.nextInt(++found) == 0) {
                    anchor = pos;
                }
            }
        }
        return anchor != null
                && placeVein(
                                level,
                                anchor,
                                random,
                                ore,
                                6,
                                pos -> level.getBiome(pos).is(Biomes.BASALT_DELTAS))
                        > 0;
    }

    public static int placeVein(WorldGenLevel level, BlockPos anchor, RandomSource random, BlockState ore, int size) {
        return placeVein(level, anchor, random, ore, size, pos -> true);
    }

    private static int placeVein(
            WorldGenLevel level,
            BlockPos anchor,
            RandomSource random,
            BlockState ore,
            int size,
            Predicate<BlockPos> allowed) {
        if (anchor.getY() <= 32
                || !host(level.getBlockState(anchor))
                || !level.getFluidState(anchor.above()).is(FluidTags.LAVA)) {
            return 0;
        }
        List<BlockPos> frontier = new ArrayList<>();
        frontier.add(anchor);
        int placed = 0;
        while (!frontier.isEmpty() && placed < size) {
            BlockPos pos = frontier.remove(random.nextInt(frontier.size()));
            if (pos.getY() <= 32 || !allowed.test(pos) || !host(level.getBlockState(pos))) {
                continue;
            }
            level.setBlock(pos, ore, 2);
            placed++;
            for (Direction direction : Direction.values()) {
                BlockPos next = pos.relative(direction);
                if (next.getY() > 32 && host(level.getBlockState(next)) && !frontier.contains(next)) {
                    frontier.add(next);
                }
            }
        }
        return placed;
    }

    private static boolean host(BlockState state) {
        return state.is(Blocks.BLACKSTONE) || state.is(Blocks.BASALT) || state.is(Blocks.SMOOTH_BASALT);
    }
}
