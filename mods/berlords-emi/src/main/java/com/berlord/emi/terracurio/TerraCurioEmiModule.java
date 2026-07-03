package com.berlord.emi.terracurio;

import com.berlord.emi.framework.Categories;
import com.berlord.emi.framework.GenericEmiRecipe;
import com.berlord.emi.framework.MachineDescriptor;
import com.berlord.emi.framework.Recipes;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import net.minecraft.world.item.crafting.Ingredient;
import org.confluence.terra_curio.common.recipe.WorkshopRecipe;

/**
 * Terra Curio — Workshop (accessory crafting station): a flat ingredient list -> one result. Reads the
 * public {@code ingredients}/{@code result} fields from the confluence-lib recipe base. extra_step_stool
 * is vanilla smithing (already shown by EMI) and is skipped.
 */
public final class TerraCurioEmiModule {
    private TerraCurioEmiModule() {
    }

    public static void register(EmiRegistry reg) {
        EmiRecipeCategory workshop = Categories.machine(reg,
                "terra_curio_workshop", "terra_curio:workshop", "Workshop");
        Recipes.forEach(reg.getRecipeManager(), WorkshopRecipe.class, (id, r) -> {
            MachineDescriptor d = new MachineDescriptor();
            for (Ingredient ing : r.ingredients) {
                d.itemIn(EmiIngredient.of(ing));
            }
            d.itemOut(EmiStack.of(r.result));
            reg.addRecipe(new GenericEmiRecipe(workshop, id, d));
        });
    }
}
