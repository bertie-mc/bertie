package io.github.bertie_mc.emi.integration.stellaris;

import com.st0x0ef.stellaris.common.data.recipes.FuelRefineryRecipe;
import com.st0x0ef.stellaris.common.data.recipes.RocketStationRecipe;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import io.github.bertie_mc.emi.framework.Categories;
import io.github.bertie_mc.emi.framework.GenericEmiRecipe;
import io.github.bertie_mc.emi.framework.InfoPages;
import io.github.bertie_mc.emi.framework.MachineDescriptor;
import io.github.bertie_mc.emi.framework.Recipes;
import java.util.List;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.crafting.Ingredient;

/**
 * Stellaris — the Rocket Station (item grid -> rocket) and the Fuel Refinery (oil -> fuel and diesel).
 *
 * <p>The refinery was deferred for a while because Stellaris speaks Architectury's FluidStack rather
 * than NeoForge's; the two only differ in their bucket unit, so converting the amount is enough and
 * the recipes can be read directly. It also closes the question of what Oil is for, which nothing in
 * the pack answered — the Water Separator stays out, as it holds no recipes of its own.
 */
public final class StellarisEmiModule {

    private StellarisEmiModule() {}

    public static void register(EmiRegistry reg) {
        EmiRecipeCategory rocket =
                Categories.machine(reg, "stellaris_rocket_station", "stellaris:rocket_station", "Rocket Station");
        Recipes.forEach(reg.getRecipeManager(), RocketStationRecipe.class, (id, r) -> {
            MachineDescriptor d = new MachineDescriptor();
            for (Ingredient ing : r.getIngredients()) d.itemIn(EmiIngredient.of(ing));
            d.itemOut(EmiStack.of(r.getResultItem(RegistryAccess.EMPTY)));
            reg.addRecipe(new GenericEmiRecipe(rocket, id, d));
        });

        fuelRefinery(reg);
        oilSource(reg);
    }

    private static void fuelRefinery(EmiRegistry reg) {
        EmiRecipeCategory refinery =
                Categories.machine(reg, "stellaris_fuel_refinery", "stellaris:fuel_refinery", "Fuel Refinery");
        Recipes.forEach(reg.getRecipeManager(), FuelRefineryRecipe.class, (id, r) -> {
            MachineDescriptor d = new MachineDescriptor();
            d.fluidIn(fluid(r.ingredientStack()));
            d.fluidOut(fluid(r.fuelStack()));
            d.fluidOut(fluid(r.dieselStack()));
            if (r.energy() > 0) {
                d.info(Component.literal(r.energy() + " energy"));
            }
            reg.addRecipe(new GenericEmiRecipe(refinery, id, d));
        });
    }

    /**
     * Oil is not made by anything — every chunk holds a reserve and the Pumpjack draws it out, so no
     * recipe could ever point at it and the fluid read as unobtainable.
     */
    private static void oilSource(EmiRegistry reg) {
        InfoPages.page(
                reg,
                "stellaris/oil",
                List.of("stellaris:oil_bucket"),
                "Every chunk holds its own oil reserve.",
                "A Pumpjack draws 10 oil at a time from the chunk it stands in,",
                "at a cost of 2 energy per unit, until that chunk runs dry.");
    }

    /** Architectury counts fluid in its own bucket unit, so the amount has to be rebased to EMI's mB. */
    private static EmiStack fluid(dev.architectury.fluid.FluidStack stack) {
        if (stack == null || stack.isEmpty()) {
            return null;
        }
        long millibuckets = stack.getAmount() * 1000L / dev.architectury.fluid.FluidStack.bucketAmount();
        return EmiStack.of(stack.getFluid(), Math.max(1L, millibuckets));
    }
}
