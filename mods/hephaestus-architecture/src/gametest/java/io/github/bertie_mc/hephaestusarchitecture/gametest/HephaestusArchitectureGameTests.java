package io.github.bertie_mc.hephaestusarchitecture.gametest;

import com.stal111.forbidden_arcanus.common.block.properties.ModBlockStateProperties;
import io.github.bertie_mc.hephaestusarchitecture.structure.ForgeLayout;
import io.github.bertie_mc.hephaestusarchitecture.structure.ForgeLayouts;
import io.github.bertie_mc.hephaestusarchitecture.structure.ForgeStructurePlacement;
import net.minecraft.core.BlockPos;
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
        }
        helper.succeed();
    }
}
