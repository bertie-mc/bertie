package io.github.bertie_mc.emi.integration.stellaris;

import io.github.bertie_mc.emi.framework.Categories;
import io.github.bertie_mc.emi.framework.GenericEmiRecipe;
import io.github.bertie_mc.emi.framework.MachineDescriptor;
import io.github.bertie_mc.emi.framework.Recipes;
import com.st0x0ef.stellaris.common.data.recipes.RocketStationRecipe;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.item.crafting.Ingredient;

/**
 * Stellaris — Rocket Station (item grid -> rocket). fuel_refinery / water_separator are deferred
 * (they use Architectury FluidStack, not NeoForge FluidStack).
 */
public final class StellarisEmiModule {
    private StellarisEmiModule() {
    }

    public static void register(EmiRegistry reg) {
        EmiRecipeCategory rocket = Categories.machine(reg, "stellaris_rocket_station", "stellaris:rocket_station", "Rocket Station");
        Recipes.forEach(reg.getRecipeManager(), RocketStationRecipe.class, (id, r) -> {
            MachineDescriptor d = new MachineDescriptor();
            for (Ingredient ing : r.getIngredients()) d.itemIn(EmiIngredient.of(ing));
            d.itemOut(EmiStack.of(r.getResultItem(RegistryAccess.EMPTY)));
            reg.addRecipe(new GenericEmiRecipe(rocket, id, d));
        });
    }
}
