package com.berlord.emi.berriescherries;

import com.berlord.emi.framework.Categories;
import com.berlord.emi.framework.GenericEmiRecipe;
import com.berlord.emi.framework.MachineDescriptor;
import com.berlord.emi.framework.Recipes;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import net.mcreator.berriesandcherries.jei_recipes.BPressCherryRecipe;
import net.mcreator.berriesandcherries.jei_recipes.BpressRecipe;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.item.crafting.Ingredient;

/** Berries & Cherries — Berry Press (bpress + b_press_cherry). */
public final class BerriesAndCherriesEmiModule {
    private BerriesAndCherriesEmiModule() {
    }

    private static final String PRESS = "berries_and_cherries:berry_press";
    private static final RegistryAccess REG = RegistryAccess.EMPTY;

    public static void register(EmiRegistry reg) {
        EmiRecipeCategory press = Categories.machine(reg, "berries_press", PRESS, "Berry Press");
        Recipes.forEach(reg.getRecipeManager(), BpressRecipe.class, (id, r) -> {
            MachineDescriptor d = new MachineDescriptor();
            for (Ingredient ing : r.getIngredients()) d.itemIn(EmiIngredient.of(ing));
            d.itemOut(EmiStack.of(r.getResultItem(REG)));
            reg.addRecipe(new GenericEmiRecipe(press, id, d));
        });
        Recipes.forEach(reg.getRecipeManager(), BPressCherryRecipe.class, (id, r) -> {
            MachineDescriptor d = new MachineDescriptor();
            for (Ingredient ing : r.getIngredients()) d.itemIn(EmiIngredient.of(ing));
            d.itemOut(EmiStack.of(r.getResultItem(REG)));
            reg.addRecipe(new GenericEmiRecipe(press, id, d));
        });
    }
}
