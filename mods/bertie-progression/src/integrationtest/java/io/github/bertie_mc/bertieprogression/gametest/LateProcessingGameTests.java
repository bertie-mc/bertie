package io.github.bertie_mc.bertieprogression.gametest;

import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import java.util.List;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Recipe;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("bertieprogression")
@PrefixGameTestTemplate(false)
public final class LateProcessingGameTests {
    @GameTest(template = "empty", timeoutTicks = 100)
    public static void selectedAmountsAndMaterialFormsLoadThroughNativeCodecs(GameTestHelper helper) {
        JsonObject purification = encoded(helper, "mekanism:processing/iron/clump/from_raw_ore");
        helper.assertTrue(
                count(purification, "item_input") == 2 && count(purification, "output") == 5,
                "Purification must consume two raw items and produce five Clumps");
        purification = encoded(helper, "mekanism:processing/iron/clump/from_raw_block");
        helper.assertTrue(
                count(purification, "item_input") == 2 && count(purification, "output") == 45,
                "Two raw storage blocks must produce 45 Clumps");
        JsonObject injection = encoded(helper, "mekanism:processing/iron/shard/from_raw_ore");
        helper.assertTrue(
                count(injection, "item_input") == 1 && count(injection, "output") == 4,
                "Injection must consume one raw item and produce four Shards");
        for (String id : List.of(
                "mekanism:chemical_infuser",
                "mekanism:chemical_injection_chamber",
                "mekanism:energized_smelter",
                "mekanism:crusher",
                "mekanism:thermal_evaporation/block",
                "mekanism:thermal_evaporation/valve",
                "mekanism:thermal_evaporation/controller",
                "mekanism:seismic_reader",
                "mekanism:seismic_vibrator",
                "oritech:crafting/cooler",
                "bertieprogression:crushing/mithril_scrap",
                "bertieprogression:enriching/mithril_scrap",
                "bertieprogression:create/lead_sheet",
                "bertieprogression:anvilcraft/lead_sheet",
                "bertieprogression:stamper/lead_sheet",
                "berlordscarving:lead_slate",
                "berlordscarving:lead_big_slate")) {
            helper.assertTrue(
                    helper.getLevel()
                            .getRecipeManager()
                            .byKey(ResourceLocation.parse(id))
                            .isPresent(),
                    "Selected recipe must load: " + id);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void removedConversionProvidersCannotRestoreTheirRecipes(GameTestHelper helper) {
        helper.assertTrue(
                ModList.get().isLoaded("twilightdelight") && ModList.get().isLoaded("twilightforest"),
                "Native conversion providers must be loaded for this regression");
        var recipes = helper.getLevel().getRecipeManager();
        for (String id : List.of(
                "create:splashing/magma_block",
                "twilightforest:charm_of_life_2",
                "twilightdelight:frozen/cutting/ice_bomb",
                "twilightdelight:frozen/cutting/ice_bow",
                "twilightdelight:frozen/cutting/ice_sword",
                "twilightdelight:frozen/obsidian",
                "twilightdelight:frozen/packed_ice")) {
            helper.assertTrue(
                    recipes.byKey(ResourceLocation.parse(id)).isEmpty(), "Retired recipe must be absent: " + id);
        }
        helper.assertTrue(
                recipes.getRecipes().stream()
                        .noneMatch(r -> r.value().getClass().getName().endsWith("SimpleFrozenRecipe")),
                "No Twilight Delight item conversion may remain active");
        helper.succeed();
    }

    private static JsonObject encoded(GameTestHelper helper, String id) {
        var recipe = helper.getLevel()
                .getRecipeManager()
                .byKey(ResourceLocation.parse(id))
                .orElseThrow()
                .value();
        return Recipe.CODEC
                .encodeStart(
                        RegistryOps.create(JsonOps.INSTANCE, helper.getLevel().registryAccess()), recipe)
                .getOrThrow()
                .getAsJsonObject();
    }

    private static int count(JsonObject recipe, String key) {
        var value = recipe.getAsJsonObject(key);
        return value.has("count") ? value.get("count").getAsInt() : 1;
    }

    private LateProcessingGameTests() {}
}
