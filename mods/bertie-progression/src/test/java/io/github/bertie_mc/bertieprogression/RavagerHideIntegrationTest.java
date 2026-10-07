package io.github.bertie_mc.bertieprogression;

import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import java.nio.file.Files;
import java.nio.file.Path;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.loot.LootTable;
import org.junit.jupiter.api.Test;

class RavagerHideIntegrationTest {
    @Test
    void registeredQuarkHidesAreVisibleThroughTheProgressionTab() {
        var tab = ModItems.MAIN_TAB.get();
        tab.buildContents(new CreativeModeTab.ItemDisplayParameters(
                FeatureFlags.VANILLA_SET, true, VanillaRegistries.createLookup()));
        for (String id : new String[] {"ravager_hide", "bonded_ravager_hide"}) {
            var item = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("quark", id));
            assertNotSame(Items.AIR, item, id);
            assertTrue(tab.getDisplayItems().stream().anyMatch(stack -> stack.is(item)), id);
        }
    }

    @Test
    void ravagerLootTableDecodesWithTheActualQuarkItem() throws Exception {
        Path path = Path.of(getClass()
                .getResource("/data/minecraft/loot_table/entities/ravager.json")
                .toURI());
        JsonObject raw = JsonParser.parseString(Files.readString(path)).getAsJsonObject();
        // Unit bootstrap has registries but no loaded item tags. Use a concrete item to exercise
        // the same equipment predicate codec; the original knife tag is checked by the data suite.
        JsonObject mainhand = raw.getAsJsonArray("pools")
                .get(1)
                .getAsJsonObject()
                .getAsJsonArray("conditions")
                .get(0)
                .getAsJsonObject()
                .getAsJsonObject("predicate")
                .getAsJsonObject("equipment")
                .getAsJsonObject("mainhand");
        JsonArray items = new JsonArray();
        items.add("minecraft:iron_sword");
        mainhand.add("items", items);
        var result = LootTable.DIRECT_CODEC.parse(
                RegistryOps.create(JsonOps.INSTANCE, VanillaRegistries.createLookup()), raw);
        assertTrue(
                result.result().isPresent(),
                () -> result.error().map(error -> error.message()).orElse("loot decode failed"));
    }
}
