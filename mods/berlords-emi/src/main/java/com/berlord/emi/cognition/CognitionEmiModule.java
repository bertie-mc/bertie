package com.berlord.emi.cognition;

import com.berlord.emi.framework.Categories;
import com.berlord.emi.framework.GenericEmiRecipe;
import com.berlord.emi.framework.MachineDescriptor;
import com.berlord.emi.framework.Recipes;
import com.cyanogen.cognition.recipe.EmptyingRecipe;
import com.cyanogen.cognition.recipe.FillingRecipe;
import com.cyanogen.cognition.recipe.MolecularMetamorpherRecipe;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeManager;

/**
 * Cognition machines. Cognitium fluid is shown as an "X mB" info line (it's a scalar cost/gain, not a
 * FluidStack). Deferred: infecting (passive block-spread, no workstation).
 */
public final class CognitionEmiModule {
    private CognitionEmiModule() {
    }

    private static final RegistryAccess REG = RegistryAccess.EMPTY;

    public static void register(EmiRegistry reg) {
        RecipeManager rm = reg.getRecipeManager();

        EmiRecipeCategory mm = Categories.machine(reg, "cognition_molecular", "cognition:molecular_metamorpher", "Molecular Metamorpher");
        Recipes.forEach(rm, MolecularMetamorpherRecipe.class, (id, r) -> {
            MachineDescriptor d = new MachineDescriptor();
            pair(d, r.ingredient1, r.count1);
            pair(d, r.ingredient2, r.count2);
            pair(d, r.ingredient3, r.count3);
            d.itemOut(EmiStack.of(r.getResultItem(REG)));
            if (r.getProcessTime() > 0) d.info(Component.literal(Categories.seconds(r.getProcessTime())));
            if (r.getCost() > 0) d.info(Component.literal(r.getCost() + " XP"));
            reg.addRecipe(new GenericEmiRecipe(mm, id, d));
        });

        EmiRecipeCategory fill = Categories.machine(reg, "cognition_filling", "cognition:experience_fountain", "Experience Fountain: Filling");
        Recipes.forEach(rm, FillingRecipe.class, (id, r) -> {
            MachineDescriptor d = new MachineDescriptor();
            d.itemIn(EmiIngredient.of(r.getIngredient()));
            d.itemOut(EmiStack.of(r.getResultItem(REG)));
            if (r.getCognitiumCost() > 0) d.info(Component.literal(r.getCognitiumCost() + " mB cognitium"));
            reg.addRecipe(new GenericEmiRecipe(fill, id, d));
        });

        EmiRecipeCategory empty = Categories.machine(reg, "cognition_emptying", "cognition:experience_fountain", "Experience Fountain: Emptying");
        Recipes.forEach(rm, EmptyingRecipe.class, (id, r) -> {
            MachineDescriptor d = new MachineDescriptor();
            d.itemIn(EmiIngredient.of(r.getIngredient()));
            if (r.hasResultStack()) d.itemOut(EmiStack.of(r.getResultItem(REG)));
            if (r.getCognitiumGain() > 0) d.info(Component.literal("+" + r.getCognitiumGain() + " mB cognitium"));
            reg.addRecipe(new GenericEmiRecipe(empty, id, d));
        });
    }

    private static void pair(MachineDescriptor d, Ingredient ing, int count) {
        if (ing != null && !ing.isEmpty()) d.itemIn(EmiIngredient.of(ing).setAmount(Math.max(1, count)));
    }
}
