package io.github.bertie_mc.bertieprogression.gametest;

import io.github.bertie_mc.bertieprogression.worldgen.RisingTungstenOreFeature;
import io.github.bertie_mc.bertieprogression.worldgen.SolarPoolOreFeature;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("bertieprogression")
@PrefixGameTestTemplate(false)
public final class OreProgressionGameTests {
    @GameTest(template = "empty")
    public static void solarVeinHasLavaAnchorAndConnectedBuriedOre(GameTestHelper h) {
        var level = h.getLevel();
        BlockPos anchor = h.absolutePos(new BlockPos(3, 3, 3)).atY(64);
        for (int x = -2; x <= 2; x++) {
            for (int y = -2; y <= 0; y++) {
                for (int z = -2; z <= 2; z++) {
                    level.setBlock(anchor.offset(x, y, z), Blocks.BASALT.defaultBlockState(), 2);
                }
            }
        }
        level.setBlock(anchor.above(), Blocks.LAVA.defaultBlockState(), 2);
        level.setBlock(anchor.east(), Blocks.BEDROCK.defaultBlockState(), 2);
        int placed = SolarPoolOreFeature.placeVein(
                level, anchor, RandomSource.create(17), Blocks.GOLD_BLOCK.defaultBlockState(), 6);
        h.assertTrue(placed == 6, "one lava contact must allow a full connected vein");
        h.assertTrue(level.getBlockState(anchor).is(Blocks.GOLD_BLOCK), "the first block must touch lava above");
        h.assertTrue(level.getBlockState(anchor.above()).is(Blocks.LAVA), "lava must survive");
        h.assertTrue(level.getBlockState(anchor.east()).is(Blocks.BEDROCK), "bedrock must survive");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void solarVeinRejectsDryRockAndOceanLevel(GameTestHelper h) {
        var level = h.getLevel();
        BlockPos dry = h.absolutePos(new BlockPos(2, 2, 2)).atY(64);
        level.setBlock(dry, Blocks.BLACKSTONE.defaultBlockState(), 2);
        h.assertTrue(
                SolarPoolOreFeature.placeVein(
                                level, dry, RandomSource.create(1), Blocks.GOLD_BLOCK.defaultBlockState(), 6)
                        == 0,
                "dry blackstone must not seed a vein");
        BlockPos ocean = new BlockPos(dry.getX(), 32, dry.getZ());
        level.setBlock(ocean, Blocks.BLACKSTONE.defaultBlockState(), 2);
        level.setBlock(ocean.above(), Blocks.LAVA.defaultBlockState(), 2);
        h.assertTrue(
                SolarPoolOreFeature.placeVein(
                                level, ocean, RandomSource.create(1), Blocks.GOLD_BLOCK.defaultBlockState(), 6)
                        == 0,
                "the lava ocean boundary must not seed a vein");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void tungstenHeightDensityIsLinearAndPositiveAtTheFloor(GameTestHelper h) {
        int[] histogram = new int[73];
        RandomSource random = RandomSource.create(511);
        for (int i = 0; i < 300000; i++) {
            int y = RisingTungstenOreFeature.sampleHeight(random);
            h.assertTrue(y >= -64 && y <= 8, "height must stay in the deepslate layer");
            histogram[y + 64]++;
        }
        double sum = 73 * (32 + 104) / 2.0;
        for (int i = 0; i < histogram.length; i++) {
            double expected = 300000 * (i + 32) / sum;
            h.assertTrue(
                    Math.abs(histogram[i] - expected) < expected * .09,
                    "density must follow the line through theoretical Y=-96: " + (i - 64));
        }
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void tungstenNativeScatteringOnlyReplacesDeepslate(GameTestHelper h) {
        var level = h.getLevel();
        BlockPos center = h.absolutePos(new BlockPos(3, 3, 3)).atY(80);
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-2, -2, -2), center.offset(2, 2, 2))) {
            level.setBlock(pos, Blocks.DEEPSLATE.defaultBlockState(), 2);
        }
        level.setBlock(center.east(), Blocks.BEDROCK.defaultBlockState(), 2);
        level.setBlock(center.above(), Blocks.STONE.defaultBlockState(), 2);
        RandomSource random = RandomSource.create(21);
        for (int i = 0; i < 24; i++) {
            RisingTungstenOreFeature.placeVein(
                    level,
                    level.getChunkSource().getGenerator(),
                    random,
                    center,
                    Blocks.GOLD_BLOCK.defaultBlockState());
        }
        int count = 0;
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-2, -2, -2), center.offset(2, 2, 2))) {
            if (level.getBlockState(pos).is(Blocks.GOLD_BLOCK)) {
                count++;
            }
        }
        h.assertTrue(count > 0, "native scattered placement must produce ore in deepslate");
        h.assertTrue(level.getBlockState(center.east()).is(Blocks.BEDROCK), "bedrock must never be replaced");
        h.assertTrue(level.getBlockState(center.above()).is(Blocks.STONE), "ordinary stone is ineligible");
        h.succeed();
    }

    private OreProgressionGameTests() {}
}
