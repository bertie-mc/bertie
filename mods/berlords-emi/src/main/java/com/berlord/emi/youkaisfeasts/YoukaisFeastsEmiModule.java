package com.berlord.emi.youkaisfeasts;

import com.berlord.emi.framework.Categories;
import com.berlord.emi.framework.GenericEmiRecipe;
import com.berlord.emi.framework.MachineDescriptor;
import com.berlord.emi.framework.Recipes;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.neoforge.NeoForgeEmiStack;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.xkmc.youkaishomecoming.content.pot.basin.SimpleBasinRecipe;
import dev.xkmc.youkaishomecoming.content.pot.cooking.core.UnorderedCookingRecipe;
import dev.xkmc.youkaishomecoming.content.pot.ferment.SimpleFermentationRecipe;
import dev.xkmc.youkaishomecoming.content.pot.kettle.KettleRecipe;
import dev.xkmc.youkaishomecoming.content.pot.rack.DryingRackRecipe;
import dev.xkmc.youkaishomecoming.content.pot.steamer.SteamingRecipe;
import dev.xkmc.youkaishomecoming.content.pot.table.recipe.MixedCuisineRecipe;
import dev.xkmc.youkaishomecoming.content.pot.table.recipe.OrderedCuisineRecipe;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeManager;
import net.neoforged.neoforge.fluids.FluidStack;

/**
 * Youkai's Feasts cooking stations (recipe base = l2core BaseRecipe; public fields). Deferred (bespoke):
 * cuisine_fixed (no inputs, transforms a prior cuisine), immediate_soup (defines a liquid base, no item output).
 */
public final class YoukaisFeastsEmiModule {
    private YoukaisFeastsEmiModule() {
    }

    private static final RegistryAccess REG = RegistryAccess.EMPTY;

    public static void register(EmiRegistry reg) {
        RecipeManager rm = reg.getRecipeManager();

        EmiRecipeCategory board = Categories.machine(reg, "youkaisfeasts_cuisine", "youkaisfeasts:cuisine_board", "Cuisine Board");
        Recipes.forEach(rm, OrderedCuisineRecipe.class, (id, r) -> {
            MachineDescriptor d = new MachineDescriptor();
            for (Ingredient ing : r.input) d.itemIn(EmiIngredient.of(ing));
            d.itemOut(EmiStack.of(r.getResult()));
            reg.addRecipe(new GenericEmiRecipe(board, id, d));
        });
        Recipes.forEach(rm, MixedCuisineRecipe.class, (id, r) -> {
            MachineDescriptor d = new MachineDescriptor();
            for (Ingredient ing : r.getCustomIngredients()) d.itemIn(EmiIngredient.of(ing));
            d.itemOut(EmiStack.of(r.getResult()));
            reg.addRecipe(new GenericEmiRecipe(board, id, d));
        });

        EmiRecipeCategory pot = Categories.machine(reg, "youkaisfeasts_cooking", "youkaisfeasts:small_iron_pot", "Pot Cooking");
        Recipes.forEach(rm, UnorderedCookingRecipe.class, (id, r) -> {
            MachineDescriptor d = new MachineDescriptor();
            for (Ingredient ing : r.getInput()) d.itemIn(EmiIngredient.of(ing));
            d.itemOut(EmiStack.of(r.getResult()));
            time(d, r.getProcessTime());
            reg.addRecipe(new GenericEmiRecipe(pot, id, d));
        });

        EmiRecipeCategory ferment = Categories.machine(reg, "youkaisfeasts_fermentation", "youkaisfeasts:fermentation_tank", "Fermentation");
        Recipes.forEach(rm, SimpleFermentationRecipe.class, (id, r) -> {
            MachineDescriptor d = new MachineDescriptor();
            for (Ingredient ing : r.ingredients) d.itemIn(EmiIngredient.of(ing));
            fluidInIf(d, r.inputFluid);
            for (ItemStack out : r.results) d.itemOut(EmiStack.of(out));
            fluidOutIf(d, r.outputFluid);
            catalystIf(d, r.defaultContainer);
            time(d, r.getProcessTime());
            reg.addRecipe(new GenericEmiRecipe(ferment, id, d));
        });

        EmiRecipeCategory kettle = Categories.machine(reg, "youkaisfeasts_kettle", "youkaisfeasts:kettle", "Kettle");
        Recipes.forEach(rm, KettleRecipe.class, (id, r) -> {
            MachineDescriptor d = new MachineDescriptor();
            for (Ingredient ing : r.input) d.itemIn(EmiIngredient.of(ing));
            fluidOutIf(d, r.result);
            time(d, r.getProcessTime());
            reg.addRecipe(new GenericEmiRecipe(kettle, id, d));
        });

        EmiRecipeCategory basin = Categories.machine(reg, "youkaisfeasts_basin", "youkaisfeasts:wood_basin", "Wood Basin");
        Recipes.forEach(rm, SimpleBasinRecipe.class, (id, r) -> {
            MachineDescriptor d = new MachineDescriptor();
            d.itemIn(EmiIngredient.of(r.input));
            fluidOutIf(d, r.output);
            reg.addRecipe(new GenericEmiRecipe(basin, id, d));
        });

        EmiRecipeCategory steam = Categories.machine(reg, "youkaisfeasts_steaming", "youkaisfeasts:steamer_pot", "Steaming");
        Recipes.forEach(rm, SteamingRecipe.class, (id, r) -> {
            MachineDescriptor d = new MachineDescriptor();
            for (Ingredient ing : r.getIngredients()) d.itemIn(EmiIngredient.of(ing));
            d.itemOut(EmiStack.of(r.getResultItem(REG)));
            time(d, r.getCookingTime());
            reg.addRecipe(new GenericEmiRecipe(steam, id, d));
        });

        EmiRecipeCategory dry = Categories.machine(reg, "youkaisfeasts_drying", "youkaisfeasts:drying_rack", "Drying Rack");
        Recipes.forEach(rm, DryingRackRecipe.class, (id, r) -> {
            MachineDescriptor d = new MachineDescriptor();
            for (Ingredient ing : r.getIngredients()) d.itemIn(EmiIngredient.of(ing));
            d.itemOut(EmiStack.of(r.getResultItem(REG)));
            time(d, r.getCookingTime());
            reg.addRecipe(new GenericEmiRecipe(dry, id, d));
        });
    }

    private static void time(MachineDescriptor d, int t) {
        if (t > 0) d.info(Component.literal(Categories.seconds(t)));
    }

    private static void fluidInIf(MachineDescriptor d, FluidStack f) {
        if (f != null && !f.isEmpty()) d.fluidIn(NeoForgeEmiStack.of(f));
    }

    private static void fluidOutIf(MachineDescriptor d, FluidStack f) {
        if (f != null && !f.isEmpty()) d.fluidOut(NeoForgeEmiStack.of(f));
    }

    private static void catalystIf(MachineDescriptor d, ItemStack s) {
        if (s != null && !s.isEmpty()) d.catalyst(EmiStack.of(s));
    }
}
