package io.github.bertie_mc.bertieprogression;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.List;
import mekanism.api.MekanismAPI;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

class OreMaterialsIntegrationTest {
    @Test
    void refiningMaterialsRegisterWithModelsAndTextures() {
        for (String name : List.of("bronze_sheet", "platinum_rod", "platinum_wire", "coal_dense_charge")) {
            assertTrue(BuiltInRegistries.ITEM.containsKey(
                    ResourceLocation.fromNamespaceAndPath(BertieProgression.MODID, name)), name);
            assertNotNull(getClass().getResource("/assets/bertieprogression/models/item/" + name + ".json"));
            assertNotNull(getClass().getResource("/assets/bertieprogression/textures/item/" + name + ".png"));
        }
        assertTrue(BuiltInRegistries.ITEM.containsKey(
                ResourceLocation.parse("bertieprogression:incomplete_platinum_wire")));
    }

    @Test
    void missingFormsRegisterOnceWithModelsAndAnimatedSlurries() throws Exception {
        assertEquals(OreMaterials.ITEMS.size(), new HashSet<>(OreMaterials.ITEMS).size());
        for (String name : OreMaterials.ITEMS) {
            assertFalse(name.contains("plutonium"), name);
            var id = ResourceLocation.fromNamespaceAndPath(BertieProgression.MODID, name);
            assertTrue(BuiltInRegistries.ITEM.containsKey(id), name);
            assertNotNull(getClass().getResource("/assets/bertieprogression/models/item/" + name + ".json"));
            assertNotNull(getClass().getResource("/assets/bertieprogression/textures/item/" + name + ".png"));
        }
        for (var entry : OreMaterials.SLURRIES.entrySet()) {
            for (String state : List.of("dirty", "clean")) {
                String name = state + "_" + entry.getKey();
                var chemical = MekanismAPI.CHEMICAL_REGISTRY.get(
                        ResourceLocation.fromNamespaceAndPath(BertieProgression.MODID, name));
                assertFalse(chemical.isEmptyType(), name);
                assertEquals(0xFFFFFF, chemical.getTint());
                assertEquals(entry.getValue().intValue(), chemical.getColorRepresentation());
                assertEquals("c:ores/" + entry.getKey(), chemical.getOreTag().location().toString());
                assertNotNull(getClass().getResource("/assets/bertieprogression/textures/chemical/" + name + ".png.mcmeta"));
            }
        }
    }

    @Test
    void intermediateRecipesDecodeAgainstRegisteredItemsAndChemicals() throws Exception {
        var ops = net.minecraft.resources.RegistryOps.create(com.mojang.serialization.JsonOps.INSTANCE,
                net.minecraft.core.RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY));
        for (String name : List.of("dust_from_dirty_dust", "dirty_dust_from_clump", "clump_from_shard",
                "shard_from_crystal", "crystal_from_slurry", "clean_slurry")) {
            try (var stream = getClass().getResourceAsStream(
                    "/data/bertieprogression/recipe/processing/tungsten/" + name + ".json")) {
                assertNotNull(stream, name);
                var json = JsonParser.parseReader(new InputStreamReader(stream));
                assertNotNull(net.minecraft.world.item.crafting.Recipe.CODEC.parse(ops, json).getOrThrow(), name);
            }
        }
    }

}
