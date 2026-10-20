package io.github.bertie_mc.bertieprogression.worldgen;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.OreConfiguration;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockMatchTest;

public final class RisingTungstenOreFeature extends Feature<NoneFeatureConfiguration> {
    public RisingTungstenOreFeature() {
        super(NoneFeatureConfiguration.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        if (!context.level().getLevel().dimension().equals(Level.OVERWORLD)) {
            return false;
        }
        BlockState ore = BuiltInRegistries.BLOCK
                .get(ResourceLocation.parse("anvilcraft:deepslate_tungsten_ore"))
                .defaultBlockState();
        if (ore.isAir()) {
            return false;
        }
        int y = sampleHeight(context.random());
        BlockPos origin =
                new BlockPos(context.origin().getX(), y, context.origin().getZ());
        return placeVein(context.level(), context.chunkGenerator(), context.random(), origin, ore);
    }

    public static boolean placeVein(
            WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin, BlockState ore) {
        // Four placements use the same 2/3-block mixture as Ancient Debris' two native placements.
        int size = random.nextBoolean() ? 2 : 3;
        OreConfiguration config = new OreConfiguration(new BlockMatchTest(Blocks.DEEPSLATE), ore, size, 1.0F);
        return Feature.SCATTERED_ORE.place(config, level, generator, random, origin);
    }

    public static int sampleHeight(RandomSource random) {
        int y;
        do {
            y = random.nextIntBetweenInclusive(-64, 8);
        } while (random.nextInt(104) >= y + 96);
        return y;
    }
}
