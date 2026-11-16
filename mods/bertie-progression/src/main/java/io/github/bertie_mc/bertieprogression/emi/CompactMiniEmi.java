package io.github.bertie_mc.bertieprogression.emi;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/** Native presentation for the pack's component-bearing Miniaturization outputs. */
final class CompactMiniEmi {
    private static EmiStack stack(String id, int count) {
        return EmiStack.of(BuiltInRegistries.ITEM.get(ResourceLocation.parse(id)))
                .setAmount(count);
    }

    static void register(EmiRegistry registry, InWorldEmiCategory giantCategory) {
        var manager = registry.getRecipeManager();
        var giantId = ResourceLocation.parse("bertieprogression:multiblock/compact_machine_wall");
        var conversionId = ResourceLocation.parse("bertieprogression:multiblock_conversion/compact_machine_wall");
        if (manager.byKey(giantId).isPresent() || manager.byKey(conversionId).isPresent()) {
            registry.removeRecipes(recipe -> giantId.equals(recipe.getId()) || conversionId.equals(recipe.getId()));
            registry.addRecipe(
                    new InWorldEmiRecipe(
                            giantCategory,
                            ResourceLocation.parse("bertieprogression:giant_anvil/compact_machine_wall"),
                            List.of(
                                    stack("minecraft:polished_deepslate", 98),
                                    stack("anvilcraft:levitation_powder_block", 26),
                                    stack("anvilcraft:heavy_iron_block", 1)),
                            List.of(stack("anvilcraft:giant_anvil", 1), stack("minecraft:crafting_table", 25)),
                            List.of(stack("compactmachines:wall", 1)),
                            List.of(
                                    Component.literal(
                                            "Build a 5×5×5 Polished Deepslate shell. Inside: a 3×3×3 Levitation Powder Block shell, with Heavy Iron Block at the centre."),
                                    Component.literal(
                                            "Cover with 5×5 Crafting Tables and drop a Giant Anvil. Forms one Wall at the centre. A Space Overcompressor in the cap's centre gives item output instead."))));
        }
        try (var input = CompactMiniEmi.class.getResourceAsStream("/compact_mini_recipes.json")) {
            if (input == null) return;
            var rows = JsonParser.parseReader(new InputStreamReader(input, StandardCharsets.UTF_8))
                    .getAsJsonArray();
            var projector = stack("compactcrafting:field_projector", 1);
            var category = new InWorldEmiCategory(
                    ResourceLocation.parse("bertieprogression:miniaturization"),
                    projector,
                    Component.literal("Miniaturization"));
            registry.addCategory(category);
            registry.addWorkstation(category, projector);
            var ops = RegistryOps.create(
                    JsonOps.INSTANCE, RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY));
            for (var element : rows) {
                var row = element.getAsJsonObject();
                var id = ResourceLocation.parse(row.get("recipe").getAsString());
                if (manager.byKey(id).isEmpty()) continue;
                int size = row.get("size").getAsInt();
                var ingredients = new ArrayList<EmiIngredient>();
                if (row.has("machine_layout")) {
                    for (var material : row.getAsJsonObject("ingredients").entrySet()) {
                        ingredients.add(
                                stack(material.getKey(), material.getValue().getAsInt()));
                    }
                } else {
                    ingredients.add(stack(
                            row.get("wall").getAsString(), row.get("wall_count").getAsInt()));
                }
                String core = null;
                if (row.has("core") && !row.get("core").isJsonNull()) {
                    core = row.get("core").getAsString();
                    ingredients.add(stack(core, 1));
                }
                ingredients.add(stack(row.get("catalyst").getAsString(), 1));
                var output = ItemStack.CODEC.parse(ops, row.get("output")).getOrThrow();
                registry.addRecipe(
                        new InWorldEmiRecipe(
                                category,
                                id,
                                ingredients,
                                List.of(projector),
                                List.of(EmiStack.of(output)),
                                List.of(
                                        Component.literal("Build a hollow " + size + "×" + size + "×" + size
                                                + " cube of the first material inside the field."),
                                        Component.literal(
                                                core == null
                                                        ? "Keep the interior empty; drop the catalyst item into the field."
                                                        : "Place the centre block at the exact centre; keep the rest of the interior empty. Drop the catalyst item into the field."))));
            }
        } catch (java.io.IOException exception) {
            throw new IllegalStateException("Unable to read Miniaturization presentation", exception);
        }
    }

    private CompactMiniEmi() {}
}
