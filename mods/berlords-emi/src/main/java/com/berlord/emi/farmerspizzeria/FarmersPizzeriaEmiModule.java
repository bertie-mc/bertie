package com.berlord.emi.farmerspizzeria;

import com.berlord.emi.framework.Categories;
import com.berlord.emi.framework.GenericEmiRecipe;
import com.berlord.emi.framework.MachineDescriptor;
import com.berlord.emi.framework.Recipes;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import net.hardzi.farmerspizzeria.jei_recipes.PizzaCompilingRecipe;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

/** Farmer's Pizzeria — pizza compiling (dough base + toppings -> pizza). No machine block; icon is a pizza. */
public final class FarmersPizzeriaEmiModule {
    private FarmersPizzeriaEmiModule() {
    }

    public static void register(EmiRegistry reg) {
        EmiRecipeCategory cat = Categories.machine(reg,
                "farmerspizzeria_pizza_compiling", "farmerspizzeria:margarita_pizza", "Pizza Compiling");
        Recipes.forEach(reg.getRecipeManager(), PizzaCompilingRecipe.class, (id, r) -> {
            MachineDescriptor d = new MachineDescriptor();
            for (Ingredient ing : r.getIngredients()) d.itemIn(EmiIngredient.of(ing));
            for (ItemStack out : r.getResultItems()) d.itemOut(EmiStack.of(out));
            reg.addRecipe(new GenericEmiRecipe(cat, id, d));
        });
    }
}
