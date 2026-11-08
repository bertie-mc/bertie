package io.github.bertie_mc.bertieprogression.recipe;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import java.io.InputStreamReader;
import mekanism.api.recipes.basic.BasicSmeltingRecipe;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.RegistryOps;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.BlastingRecipe;
import net.minecraft.world.item.crafting.CookingBookCategory;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeType;
import org.junit.jupiter.api.Test;

class ElectricSmeltingTest {
    @Test
    void energizedSmelterRecipeUsesItsOwnTypeRatherThanVanillaSmelting() throws Exception {
        var registries = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
        try (var stream = getClass().getResourceAsStream(
                "/data/bertieprogression/recipe/electric_smelting/charge_mix.json")) {
            var json = JsonParser.parseReader(new InputStreamReader(stream)).getAsJsonObject();
            json.getAsJsonObject("input").addProperty("item", "minecraft:quartz");
            json.getAsJsonObject("output").addProperty("id", "minecraft:amethyst_shard");
            var decoded = Recipe.CODEC.parse(RegistryOps.create(JsonOps.INSTANCE, registries), json).getOrThrow();
            assertInstanceOf(BasicSmeltingRecipe.class, decoded);
            assertEquals("mekanism:smelting", BuiltInRegistries.RECIPE_TYPE.getKey(decoded.getType()).toString());
        }
    }

    @Test
    void poweredFurnaceConversionPreservesTheBlastingResultAndDuration() {
        var registries = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
        var original = new BlastingRecipe("charge_mix", CookingBookCategory.MISC,
                Ingredient.of(Items.QUARTZ), new ItemStack(Items.AMETHYST_SHARD), 0.5f, 100);
        var electric = ElectricSmelting.convert(original, registries);
        assertEquals(ElectricSmelting.SOURCE, electric.id());
        assertEquals(1, electric.value().getResultItem(registries).getCount());
        assertEquals(Items.AMETHYST_SHARD, electric.value().getResultItem(registries).getItem());
        assertEquals(100, electric.value().getCookingTime());
        assertEquals(RecipeType.BLASTING, original.getType());
    }
}
