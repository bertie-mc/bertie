package io.github.bertie_mc.bertieprogression.gametest;

import io.github.bertie_mc.bertieprogression.compat.LightningMagnetInputs;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("bertieprogression")
@PrefixGameTestTemplate(false)
public final class LightningMagnetGameTests {
    @GameTest(template = "empty")
    public static void lightningConversionUsesConfiguredMaterialInsteadOfIron(GameTestHelper h) {
        h.assertTrue(
                LightningMagnetInputs.accepts(Blocks.GOLD_BLOCK.defaultBlockState()),
                "the configured fixture material must be accepted");
        h.assertTrue(
                !LightningMagnetInputs.accepts(Blocks.IRON_BLOCK.defaultBlockState()),
                "iron blocks must no longer be accepted");
        h.assertTrue(
                !LightningMagnetInputs.accepts(Blocks.LIGHTNING_ROD.defaultBlockState()),
                "the lightning rod is not the consumed material");
        h.succeed();
    }

    private LightningMagnetGameTests() {}
}
