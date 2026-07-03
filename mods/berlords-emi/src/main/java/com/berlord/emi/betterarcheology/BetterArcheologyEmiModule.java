package com.berlord.emi.betterarcheology;

import com.berlord.emi.framework.Categories;
import com.berlord.emi.framework.GenericEmiRecipe;
import com.berlord.emi.framework.MachineDescriptor;
import com.berlord.emi.framework.Recipes;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import net.Pandarix.recipe.IdentifyingRecipe;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.item.crafting.Ingredient;

/** Better Archeology — Identifying (unidentified artifact -> item, at the Archeology Table). */
public final class BetterArcheologyEmiModule {
    private BetterArcheologyEmiModule() {
    }

    public static void register(EmiRegistry reg) {
        EmiRecipeCategory cat = Categories.machine(reg, "betterarcheology_identifying", "betterarcheology:archeology_table", "Identifying");
        Recipes.forEach(reg.getRecipeManager(), IdentifyingRecipe.class, (id, r) -> {
            MachineDescriptor d = new MachineDescriptor();
            for (Ingredient ing : r.getIngredients()) d.itemIn(EmiIngredient.of(ing));
            d.itemOut(EmiStack.of(r.getResultItem(RegistryAccess.EMPTY)));
            reg.addRecipe(new GenericEmiRecipe(cat, id, d));
        });
    }
}
