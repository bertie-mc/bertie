package io.github.bertie_mc.bertieprogression;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import java.io.InputStreamReader;
import mekanism.api.recipes.PressurizedReactionRecipe;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import org.junit.jupiter.api.Test;

class CapacitorReactionTest {
    @Test
    void componentSensitivePotionIngredientDecodesAndRejectsOtherPotions() throws Exception {
        try (var stream =
                getClass().getResourceAsStream("/data/bertieprogression/recipe/reaction/empty_capacitor.json")) {
            var json = JsonParser.parseReader(new InputStreamReader(stream)).getAsJsonObject();
            // Use vanilla registry entries to exercise the exact production codec without loading
            // Create and Iron's Spellbooks into the focused Mekanism test environment.
            var fluid = json.getAsJsonObject("fluid_input");
            fluid.addProperty("fluids", "minecraft:water");
            fluid.getAsJsonObject("components")
                    .getAsJsonObject("minecraft:potion_contents")
                    .addProperty("potion", "minecraft:healing");
            var ops = RegistryOps.create(
                    JsonOps.INSTANCE, RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY));
            var recipe =
                    (PressurizedReactionRecipe) Recipe.CODEC.parse(ops, json).getOrThrow();
            var correct = new FluidStack(Fluids.WATER, 5000);
            correct.set(DataComponents.POTION_CONTENTS, new PotionContents(Potions.HEALING));
            assertTrue(recipe.getInputFluid().test(correct));
            var wrong = correct.copy();
            wrong.set(DataComponents.POTION_CONTENTS, new PotionContents(Potions.AWKWARD));
            assertFalse(recipe.getInputFluid().test(wrong));
            assertFalse(recipe.getInputFluid().test(new FluidStack(Fluids.WATER, 5000)));
        }
    }
}
