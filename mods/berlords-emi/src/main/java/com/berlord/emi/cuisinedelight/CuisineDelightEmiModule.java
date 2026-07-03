package com.berlord.emi.cuisinedelight;

import com.berlord.emi.framework.Categories;
import com.berlord.emi.framework.GenericEmiRecipe;
import com.berlord.emi.framework.MachineDescriptor;
import com.berlord.emi.framework.Recipes;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.xkmc.cuisinedelight.content.recipe.PlateCuisineRecipe;
import net.minecraft.core.RegistryAccess;

/** Cuisine Delight — plating on the skillet (food ingredients -> dish). Recipe base = l2core BaseRecipe. */
public final class CuisineDelightEmiModule {
    private CuisineDelightEmiModule() {
    }

    public static void register(EmiRegistry reg) {
        EmiRecipeCategory plate = Categories.machine(reg, "cuisinedelight_plate", "cuisinedelight:cuisine_skillet", "Plate Cuisine");
        Recipes.forEach(reg.getRecipeManager(), PlateCuisineRecipe.class, (id, r) -> {
            MachineDescriptor d = new MachineDescriptor();
            for (var match : r.list) {
                d.itemIn(EmiIngredient.of(match.ingredient()));
            }
            d.itemOut(EmiStack.of(r.getResultItem(RegistryAccess.EMPTY)));
            reg.addRecipe(new GenericEmiRecipe(plate, id, d));
        });
    }
}
