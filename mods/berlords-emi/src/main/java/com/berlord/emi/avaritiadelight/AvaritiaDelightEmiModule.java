package com.berlord.emi.avaritiadelight;

import com.berlord.emi.framework.Categories;
import com.berlord.emi.framework.GenericEmiRecipe;
import com.berlord.emi.framework.MachineDescriptor;
import com.berlord.emi.framework.Recipes;
import committee.nova.avaritia_delight.common.crafting.recipe.CropExtractorRecipe;
import committee.nova.avaritia_delight.common.crafting.recipe.EXCookingRecipe;
import committee.nova.avaritia_delight.common.crafting.recipe.ExtremeCookingPotRecipe;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeManager;

/** Avaritia Delight — Extreme Cooking Pot, Extreme Stove, Crop Extractor. All flat ingredient lists. */
public final class AvaritiaDelightEmiModule {
    private AvaritiaDelightEmiModule() {
    }

    private static final RegistryAccess REG = RegistryAccess.EMPTY;

    public static void register(EmiRegistry reg) {
        RecipeManager rm = reg.getRecipeManager();

        EmiRecipeCategory pot = Categories.machine(reg,
                "avaritia_delight_extreme_cooking", "avaritia_delight:extreme_cooking_pot", "Extreme Cooking");
        Recipes.forEach(rm, ExtremeCookingPotRecipe.class, (id, r) -> {
            MachineDescriptor d = new MachineDescriptor();
            for (Ingredient ing : r.getIngredients()) d.itemIn(EmiIngredient.of(ing));
            d.itemOut(EmiStack.of(r.getResultItem(REG)));
            if (r.getCookTime() > 0) d.info(Component.literal(Categories.seconds(r.getCookTime())));
            reg.addRecipe(new GenericEmiRecipe(pot, id, d));
        });

        EmiRecipeCategory stove = Categories.machine(reg,
                "avaritia_delight_ex_cooking", "avaritia_delight:extreme_stove", "Extreme Stove");
        Recipes.forEach(rm, EXCookingRecipe.class, (id, r) -> {
            MachineDescriptor d = new MachineDescriptor();
            for (Ingredient ing : r.getIngredients()) d.itemIn(EmiIngredient.of(ing));
            d.itemOut(EmiStack.of(r.getResultItem(REG)));
            if (r.getCookingTime() > 0) d.info(Component.literal(Categories.seconds(r.getCookingTime())));
            reg.addRecipe(new GenericEmiRecipe(stove, id, d));
        });

        EmiRecipeCategory crop = Categories.machine(reg,
                "avaritia_delight_crop_extractor", "avaritia_delight:crop_extractor", "Crop Extractor");
        Recipes.forEach(rm, CropExtractorRecipe.class, (id, r) -> {
            MachineDescriptor d = new MachineDescriptor();
            d.itemIn(EmiIngredient.of(r.getInput()));
            d.itemOut(EmiStack.of(r.getOutput1()));
            d.itemOut(EmiStack.of(r.getOutput2()));
            d.itemOut(EmiStack.of(r.getOutput3()));
            d.itemOut(EmiStack.of(r.getOutput4()));
            if (r.getExtractionTime() > 0) d.info(Component.literal(Categories.seconds(r.getExtractionTime())));
            reg.addRecipe(new GenericEmiRecipe(crop, id, d));
        });
    }
}
