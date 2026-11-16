package io.github.bertie_mc.bertieprogression;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.MapCodec;
import dev.dubhe.anvilcraft.recipe.multiblock.MultiblockConversionRecipe;
import dev.dubhe.anvilcraft.recipe.multiblock.MultiblockInput;
import io.github.bertie_mc.bertieprogression.recipe.CompactMachineRecipePolicy;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.junit.jupiter.api.Test;

class CompactStorageRecipesTest {
    private JsonObject json(String path) throws Exception {
        try (var input = getClass().getResourceAsStream(path)) {
            assertNotNull(input, path);
            return JsonParser.parseReader(new InputStreamReader(input, java.nio.charset.StandardCharsets.UTF_8))
                    .getAsJsonObject();
        }
    }

    @Test
    void farmingIsFilteredWithoutHidingOtherRoomVariants() {
        var ops = RegistryOps.create(
                JsonOps.INSTANCE, RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY));
        for (String template : List.of("farming", "tiny", "soaryn")) {
            var value = JsonParser.parseString(
                    "{\"id\":\"compactmachines:new_machine\",\"components\":{\"compactmachines:room_template\":\"compactmachines:"
                            + template + "\"}}");
            ItemStack stack = ItemStack.CODEC.parse(ops, value).getOrThrow();
            assertEquals(template.equals("farming"), RemovedItems.isRemoved(stack), template);
        }
    }

    @Test
    void savedPackOrderCannotRestoreStockRecipes() throws Exception {
        assertNotNull(Class.forName("net.minecraft.world.item.crafting.RecipeManager"));
        var wall = ResourceLocation.parse("compactmachines:wall");
        var soaryn = ResourceLocation.parse("compactmachines:new_machine_soaryn");
        var module = ResourceLocation.parse("compactmachines:enlarging_module");
        var selected = ResourceLocation.parse("bertieprogression:miniaturization/compact_machine_soaryn");
        JsonElement value = new JsonObject();
        var input = Map.of(wall, value, soaryn, value, module, value, selected, value);
        assertEquals(Map.of(module, value, selected, value), CompactMachineRecipePolicy.filter(input));
        assertEquals(4, input.size());
    }

    @Test
    void nativeGiantAnvilMatchesTheCompleteFiveCube() throws Exception {
        var ops = RegistryOps.create(
                JsonOps.INSTANCE, RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY));
        var data = json("/data/bertieprogression/recipe/multiblock_conversion/compact_machine_wall.json");
        var recipe = (MultiblockConversionRecipe) Recipe.CODEC.parse(ops, data).getOrThrow();
        var pattern = recipe.getInputPattern();
        List<List<List<BlockState>>> layers = new ArrayList<>();
        for (int y = 0; y < 5; y++) {
            List<List<BlockState>> rows = new ArrayList<>();
            for (int z = 0; z < 5; z++) {
                List<BlockState> row = new ArrayList<>();
                for (int x = 0; x < 5; x++)
                    row.add(pattern.getPredicate(x, y, z).getDefaultState());
                rows.add(row);
            }
            layers.add(rows);
        }
        assertTrue(recipe.matches(new MultiblockInput(layers, 5), null));
        assertEquals(
                ResourceLocation.parse("compactmachines:wall"),
                BuiltInRegistries.BLOCK.getKey(
                        recipe.getOutputPattern().getPredicate(2, 2, 2).getBlock()));
        int walls = 0;
        for (int y = 0; y < 5; y++)
            for (int z = 0; z < 5; z++)
                for (int x = 0; x < 5; x++) {
                    var state = recipe.getOutputPattern().getPredicate(x, y, z).getDefaultState();
                    if (state.isAir()) continue;
                    walls++;
                    assertEquals(
                            ResourceLocation.parse("compactmachines:wall"),
                            BuiltInRegistries.BLOCK.getKey(state.getBlock()));
                }
        assertEquals(1, walls);
        layers.get(2).get(2).set(2, Blocks.AIR.defaultBlockState());
        assertFalse(recipe.matches(new MultiblockInput(layers, 5), null));
    }

    @Test
    void nativeMiniaturizationDecodesAllFieldSizesAndMachineComponents() throws Exception {
        var clazz = Class.forName("dev.compactmods.crafting.recipes.MiniaturizationRecipe");
        var codec = (MapCodec<?>) clazz.getField("CODEC").get(null);
        var ops = RegistryOps.create(
                JsonOps.INSTANCE, RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY));
        var view = JsonParser.parseReader(
                        new InputStreamReader(getClass().getResourceAsStream("/compact_mini_recipes.json")))
                .getAsJsonArray();
        for (var entry : view) {
            var row = entry.getAsJsonObject();
            if (row.has("machine_layout")) continue;
            String id = row.get("recipe").getAsString().split(":")[1];
            var data = json("/data/bertieprogression/recipe/" + id + ".json").deepCopy();
            // Material IDs are audited against the full pack; this fixture tests native geometry and output components.
            var components = data.getAsJsonObject("components");
            if (components.has("core")) components.getAsJsonObject("core").addProperty("block", "minecraft:iron_block");
            data.getAsJsonObject("catalyst").addProperty("id", "minecraft:stick");
            Object recipe = codec.codec().parse(ops, data).getOrThrow();
            int size = row.get("size").getAsInt();
            var dimensions = (AABB) clazz.getMethod("dimensions").invoke(recipe);
            assertEquals(size, dimensions.getXsize());
            assertEquals(size, dimensions.getYsize());
            assertEquals(size, dimensions.getZsize());
            var totals = (Map<?, ?>) clazz.getMethod("cachedComponentTotals").invoke(recipe);
            assertEquals(size * size * size - (size - 2) * (size - 2) * (size - 2), totals.get("wall"));
            if (components.has("core")) assertEquals(1, totals.get("core"));
            var outputs = (ItemStack[]) clazz.getMethod("outputs").invoke(recipe);
            assertEquals(1, outputs.length);
            if (id.endsWith("compact_machine_soaryn")) {
                assertFalse(RemovedItems.isRemoved(outputs[0]));
                var component = BuiltInRegistries.DATA_COMPONENT_TYPE.get(
                        ResourceLocation.parse("compactmachines:room_template"));
                assertEquals(ResourceLocation.parse("compactmachines:soaryn"), outputs[0].get(component));
            }
        }
    }
}
