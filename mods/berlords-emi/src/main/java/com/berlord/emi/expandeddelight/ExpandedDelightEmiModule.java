package com.berlord.emi.expandeddelight;

import com.berlord.emi.framework.Categories;
import com.berlord.emi.framework.GenericEmiRecipe;
import com.berlord.emi.framework.MachineDescriptor;
import com.berlord.emi.framework.Recipes;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import ianm1647.expandeddelight.common.crafting.JuicerRecipe;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.crafting.Ingredient;

/** ExpandedDelight — the Juicer (item(s) -> juice item). */
public final class ExpandedDelightEmiModule {
    private ExpandedDelightEmiModule() {
    }

    public static void register(EmiRegistry reg) {
        EmiRecipeCategory cat = Categories.machine(reg,
                "expandeddelight_juicing", "expandeddelight:juicer", "Juicing");
        Recipes.forEach(reg.getRecipeManager(), JuicerRecipe.class, (id, r) -> {
            MachineDescriptor d = new MachineDescriptor();
            for (Ingredient ing : r.getIngredients()) d.itemIn(EmiIngredient.of(ing));
            d.itemOut(EmiStack.of(r.getResultItem(RegistryAccess.EMPTY)));
            if (r.getJuiceTime() > 0) d.info(Component.literal(Categories.seconds(r.getJuiceTime())));
            reg.addRecipe(new GenericEmiRecipe(cat, id, d));
        });
    }
}
