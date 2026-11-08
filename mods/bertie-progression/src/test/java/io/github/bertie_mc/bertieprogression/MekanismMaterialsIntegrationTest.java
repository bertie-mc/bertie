package io.github.bertie_mc.bertieprogression;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import mekanism.api.MekanismAPI;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

class MekanismMaterialsIntegrationTest {
    @Test
    void enrichedFuelRecipesDecodeAgainstTheLiveChemicalRegistry() throws Exception {
        var ops = net.minecraft.resources.RegistryOps.create(
                com.mojang.serialization.JsonOps.INSTANCE,
                net.minecraft.core.RegistryAccess.fromRegistryOfRegistries(
                        net.minecraft.core.registries.BuiltInRegistries.REGISTRY));
        for (String name : java.util.List.of("zinc", "amethyst")) {
            String path = "/data/bertieprogression/recipe/chemical_conversion/" + name + "/from_enriched.json";
            try (var stream = getClass().getResourceAsStream(path)) {
                var json = com.google.gson.JsonParser.parseReader(new java.io.InputStreamReader(stream));
                var recipe = net.minecraft.world.item.crafting.Recipe.CODEC
                        .parse(ops, json)
                        .getOrThrow();
                var fuel = (mekanism.api.recipes.ItemStackToChemicalRecipe) recipe;
                var item = name.equals("zinc") ? ModItems.ENRICHED_ZINC.get() : ModItems.ENRICHED_AMETHYST.get();
                var output = fuel.getOutput(new net.minecraft.world.item.ItemStack(item));
                assertEquals(80, output.getAmount());
                assertEquals(
                        MekanismAPI.CHEMICAL_REGISTRY.get(
                                ResourceLocation.fromNamespaceAndPath(BertieProgression.MODID, name)),
                        output.getChemical());
            }
        }
    }

    @Test
    void infusionMaterialsRegisterWithTheirOwnIconsAndDisplayColors() throws Exception {
        com.google.gson.JsonArray atlas;
        try (var stream = getClass().getResourceAsStream("/assets/minecraft/atlases/blocks.json")) {
            atlas = com.google.gson.JsonParser.parseReader(new java.io.InputStreamReader(stream))
                    .getAsJsonObject()
                    .getAsJsonArray("sources");
        }
        for (var entry :
                java.util.Map.of("zinc", 0xB9E9C1, "amethyst", 0xB38EF3).entrySet()) {
            var id = ResourceLocation.fromNamespaceAndPath(BertieProgression.MODID, entry.getKey());
            var chemical = MekanismAPI.CHEMICAL_REGISTRY.get(id);
            assertFalse(chemical.isEmptyType(), id.toString());
            assertEquals(
                    ResourceLocation.fromNamespaceAndPath(BertieProgression.MODID, "chemical/" + entry.getKey()),
                    chemical.getIcon());
            assertEquals(0xFFFFFF, chemical.getTint(), "precoloured sprites must not be tinted twice");
            assertEquals(entry.getValue().intValue(), chemical.getColorRepresentation());
            assertEquals("chemical.bertieprogression." + entry.getKey(), chemical.getTranslationKey());
            org.junit.jupiter.api.Assertions.assertTrue(
                    atlas.asList().stream()
                            .anyMatch(source -> source.getAsJsonObject()
                                    .get("resource")
                                    .getAsString()
                                    .equals(chemical.getIcon().toString())),
                    "tank and GUI icons must be stitched into the block atlas");
        }
    }
}
