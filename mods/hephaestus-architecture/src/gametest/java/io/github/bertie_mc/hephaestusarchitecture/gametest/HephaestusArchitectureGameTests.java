package io.github.bertie_mc.hephaestusarchitecture.gametest;

import com.stal111.forbidden_arcanus.common.block.properties.ModBlockStateProperties;
import io.github.bertie_mc.hephaestusarchitecture.structure.ForgeLayout;
import io.github.bertie_mc.hephaestusarchitecture.structure.ForgeLayouts;
import io.github.bertie_mc.hephaestusarchitecture.structure.ForgeStructurePlacement;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("hephaestusarchitecture")
@PrefixGameTestTemplate(false)
public final class HephaestusArchitectureGameTests {
    private HephaestusArchitectureGameTests() {}

    /** Pedestal counts per tier; tiers 1 and (until its template ships) 5 use the native ring. */
    private static final int[] EXPECTED_PEDESTALS = {8, 8, 12, 12, 8};

    @GameTest(template = "empty")
    public static void loadsPackagedTierLayouts(GameTestHelper helper) {
        for (int tier = 2; tier <= 4; tier++) {
            ResourceLocation id =
                    ResourceLocation.fromNamespaceAndPath("hephaestusarchitecture", "hephaestus_forge/tier_" + tier);
            if (helper.getLevel().getStructureManager().get(id).isEmpty()) {
                helper.fail("Missing runtime structure template " + id);
                return;
            }
            if (ForgeLayouts.candidatePedestalOffsets(helper.getLevel(), tier).isEmpty()) {
                helper.fail("Runtime structure template " + id + " has no pedestals");
                return;
            }
        }
        helper.succeed();
    }

    /**
     * Places every tier with the structure placer routine, on top of the previous tier's
     * leftovers, and requires each result to validate and activate immediately.
     */
    @GameTest(template = "placer_arena")
    public static void placedLayoutsValidateAndActivate(GameTestHelper helper) {
        BlockPos origin = helper.absolutePos(new BlockPos(7, 1, 7));
        for (int tier = 1; tier <= 5; tier++) {
            BlockPos forgePos = ForgeStructurePlacement.place(helper.getLevel(), origin, tier);

            ForgeLayout.Match match = ForgeLayouts.match(helper.getLevel(), forgePos, tier);
            if (match == null) {
                helper.fail("Placed tier " + tier + " layout does not validate");
                return;
            }
            if (match.pedestalOffsets().size() != EXPECTED_PEDESTALS[tier - 1]) {
                helper.fail("Placed tier " + tier + " layout has "
                        + match.pedestalOffsets().size() + " pedestals, expected " + EXPECTED_PEDESTALS[tier - 1]);
                return;
            }
            if (!helper.getLevel().getBlockState(forgePos).getValue(ModBlockStateProperties.ACTIVATED)) {
                helper.fail("Placed tier " + tier + " forge did not activate");
                return;
            }
            if (tier == 3) assertTierThreeMaterials(helper, forgePos);
        }
        helper.succeed();
    }

    private static void assertTierThreeMaterials(GameTestHelper helper, BlockPos forgePos) {
        var level = helper.getLevel();
        var core = BuiltInRegistries.BLOCK.get(ResourceLocation.parse("anvilcraft:earth_core_shard_block"));
        var corrupt =
                BuiltInRegistries.BLOCK.get(ResourceLocation.parse("forbidden_arcanus:corrupted_arcane_crystal_block"));
        var oldCrystal = BuiltInRegistries.BLOCK.get(ResourceLocation.parse("forbidden_arcanus:arcane_crystal_block"));
        int cores = 0;
        int crystals = 0;
        BlockPos firstCrystal = null;
        for (BlockPos pos : BlockPos.betweenClosed(forgePos.offset(-6, -1, -6), forgePos.offset(6, 4, 6))) {
            var state = level.getBlockState(pos);
            if (state.is(core)) cores++;
            if (state.is(corrupt)) {
                crystals++;
                firstCrystal = pos.immutable();
            }
        }
        helper.assertTrue(cores == 4 && crystals == 4, "Tier III placer must use four blocks of each new material");
        if (firstCrystal != null) {
            level.setBlockAndUpdate(firstCrystal, oldCrystal.defaultBlockState());
            helper.assertTrue(
                    ForgeLayouts.match(level, forgePos, 3) == null,
                    "An ordinary crystal block must no longer satisfy the tier III layout");
            level.setBlockAndUpdate(firstCrystal, corrupt.defaultBlockState());
        }
    }
}
