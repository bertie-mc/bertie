package com.berlord.emi.twilightdelight;

import com.berlord.emi.framework.Categories;
import com.berlord.emi.framework.GenericEmiRecipe;
import com.berlord.emi.framework.MachineDescriptor;
import com.berlord.emi.framework.Recipes;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.xkmc.twilightdelight.content.recipe.SimpleFrozenRecipe;

/** Twilight Delight — Freezing (1 item -> 1 item). Recipe base = l2core BaseRecipe (public fields). */
public final class TwilightDelightEmiModule {
    private TwilightDelightEmiModule() {
    }

    public static void register(EmiRegistry reg) {
        EmiRecipeCategory frozen = Categories.machineNoStation(reg, "twilightdelight_frozen", "twilightforest:ice_bomb", "Freezing");
        Recipes.forEach(reg.getRecipeManager(), SimpleFrozenRecipe.class, (id, r) -> {
            MachineDescriptor d = new MachineDescriptor();
            d.itemIn(EmiIngredient.of(r.ingredient));
            d.itemOut(EmiStack.of(r.result));
            reg.addRecipe(new GenericEmiRecipe(frozen, id, d));
        });
    }
}
