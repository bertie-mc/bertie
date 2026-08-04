package io.github.bertie_mc.hephaestusarchitecture.gametest;

import io.github.bertie_mc.hephaestusarchitecture.structure.ForgeLayouts;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("hephaestusarchitecture")
@PrefixGameTestTemplate(false)
public final class HephaestusArchitectureGameTests {
    private HephaestusArchitectureGameTests() {
    }

    @GameTest(template = "empty")
    public static void loadsPackagedTierLayouts(GameTestHelper helper) {
        for (int tier = 2; tier <= 4; tier++) {
            ResourceLocation id = ResourceLocation.fromNamespaceAndPath(
                    "hephaestusarchitecture", "hephaestus_forge/tier_" + tier);
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
}
