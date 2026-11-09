package io.github.bertie_mc.bertieprogression;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import java.io.InputStreamReader;
import mekanism.api.recipes.PressurizedReactionRecipe;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import org.junit.jupiter.api.Test;

class CapacitorReactionTest {
    @Test
    void nativeManaFluidIngredientDecodesWithoutPotionComponents() throws Exception {
        try (var stream =
                getClass().getResourceAsStream("/data/bertieprogression/recipe/reaction/empty_capacitor.json")) {
            var json = JsonParser.parseReader(new InputStreamReader(stream)).getAsJsonObject();
            // Substitute a registered liquid to exercise the production codec without loading Magitech.
            var fluid = json.getAsJsonObject("fluid_input");
            assertEquals("magitech:mana_potion", fluid.get("fluid").getAsString());
            assertFalse(fluid.has("components"));
            fluid.addProperty("fluid", "minecraft:water");
            var ops = RegistryOps.create(
                    JsonOps.INSTANCE, RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY));
            var recipe =
                    (PressurizedReactionRecipe) Recipe.CODEC.parse(ops, json).getOrThrow();
            var correct = new FluidStack(Fluids.WATER, 5000);
            assertTrue(recipe.getInputFluid().test(correct));
            assertFalse(recipe.getInputFluid().test(new FluidStack(Fluids.LAVA, 5000)));
            assertFalse(recipe.getInputFluid().test(new FluidStack(Fluids.WATER, 4999)));
        }
    }
}
