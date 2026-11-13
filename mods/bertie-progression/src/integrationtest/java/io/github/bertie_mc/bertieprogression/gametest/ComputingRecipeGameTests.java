package io.github.bertie_mc.bertieprogression.gametest;

import java.util.List;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("bertieprogression")
@PrefixGameTestTemplate(false)
public final class ComputingRecipeGameTests {
    @GameTest(template = "empty", timeoutTicks = 100)
    public static void selectedRecipeTypesActuallyLoadInTheNativeManager(GameTestHelper helper) {
        var recipes = helper.getLevel().getRecipeManager();
        for (String id : List.of(
                "bertieprogression:atomic_forge/ace_stage_1",
                "bertieprogression:atomic_forge/ace_stage_2",
                "bertieprogression:atomic_forge/ace_stage_3",
                "bertieprogression:atomic_forge/ace_stage_4",
                "oritech:crafting/atomicforge",
                "mekmm:cnc_stamper",
                "mekmm:cnc_lathe",
                "mekmm:cnc_rolling_mill",
                "mekanism:pigment_extractor",
                "mekanism:pigment_mixer",
                "mekanism:painting_machine",
                "l2complements:craft/shulkerate_ingot",
                "irons_spellbooks:mithril_ingot_from_scrap",
                "refinedstorage:wireless_transmitter",
                "refinedstorage:wireless_grid")) {
            helper.assertTrue(
                    recipes.byKey(ResourceLocation.parse(id)).isPresent(),
                    "selected recipe must decode and load: " + id);
        }
        for (String id : List.of(
                "oritech:atomicforge/advcomputer",
                "oritech:atomicforge/urandust",
                "oritech:atomicforge/urandustgem",
                "irons_spellbooks:mithril_weave",
                "bertieprogression:anvil_collision/wireless_transmitter")) {
            helper.assertTrue(
                    recipes.byKey(ResourceLocation.parse(id)).isEmpty(),
                    "retired shortcut must remain unavailable: " + id);
        }
        helper.succeed();
    }

    private ComputingRecipeGameTests() {}
}
