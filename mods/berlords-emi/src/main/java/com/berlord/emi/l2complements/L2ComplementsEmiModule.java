package com.berlord.emi.l2complements;

import com.berlord.emi.framework.Categories;
import com.berlord.emi.framework.GenericEmiRecipe;
import com.berlord.emi.framework.MachineDescriptor;
import com.berlord.emi.framework.Recipes;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.xkmc.l2complements.content.recipe.BurntRecipe;
import dev.xkmc.l2complements.content.recipe.DiffusionRecipe;

/**
 * L2 Complements — Burning (item -> essence) and Diffusion (block + base block -> block, via the
 * Diffusion Wand). Recipe base = l2core BaseRecipe (public fields). Diffusion fields are Blocks.
 */
public final class L2ComplementsEmiModule {
    private L2ComplementsEmiModule() {
    }

    public static void register(EmiRegistry reg) {
        EmiRecipeCategory burnt = Categories.machineNoStation(reg, "l2complements_burnt", "minecraft:lava_bucket", "Burning");
        Recipes.forEach(reg.getRecipeManager(), BurntRecipe.class, (id, r) -> {
            MachineDescriptor d = new MachineDescriptor();
            d.itemIn(EmiIngredient.of(r.ingredient));
            d.itemOut(EmiStack.of(r.result));
            reg.addRecipe(new GenericEmiRecipe(burnt, id, d));
        });

        EmiRecipeCategory diff = Categories.machine(reg, "l2complements_diffusion", "l2complements:diffusion_wand", "Diffusion");
        Recipes.forEach(reg.getRecipeManager(), DiffusionRecipe.class, (id, r) -> {
            MachineDescriptor d = new MachineDescriptor();
            d.itemIn(EmiStack.of(r.ingredient));   // Block is an ItemLike
            d.catalyst(EmiStack.of(r.base));
            d.itemOut(EmiStack.of(r.result));
            reg.addRecipe(new GenericEmiRecipe(diff, id, d));
        });
    }
}
