package com.berlord.emi.slavicdelight;

import com.berlord.emi.framework.Categories;
import com.berlord.emi.framework.GenericEmiRecipe;
import com.berlord.emi.framework.MachineDescriptor;
import com.berlord.emi.framework.Recipes;
import com.legomanchik.slavic_delight.common.crafting.BrewBarrelRecipe;
import com.legomanchik.slavic_delight.common.crafting.ClayPotRecipe;
import com.legomanchik.slavic_delight.common.crafting.JarRecipe;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeManager;

/** Slavic Delight — Clay Pot, Brew Barrel, Jar (pickling). */
public final class SlavicDelightEmiModule {
    private SlavicDelightEmiModule() {
    }

    private static final RegistryAccess REG = RegistryAccess.EMPTY;

    public static void register(EmiRegistry reg) {
        RecipeManager rm = reg.getRecipeManager();

        EmiRecipeCategory pot = Categories.machine(reg, "slavic_clay_pot", "slavic_delight:clay_pot", "Clay Pot Cooking");
        Recipes.forEach(rm, ClayPotRecipe.class, (id, r) -> {
            MachineDescriptor d = new MachineDescriptor();
            for (Ingredient ing : r.getIngredients()) d.itemIn(EmiIngredient.of(ing));
            d.itemOut(EmiStack.of(r.getResultItem(REG)));
            if (r.getCookTime() > 0) d.info(Component.literal(Categories.seconds(r.getCookTime())));
            reg.addRecipe(new GenericEmiRecipe(pot, id, d));
        });

        EmiRecipeCategory barrel = Categories.machine(reg, "slavic_brewing", "slavic_delight:brew_barrel", "Brew Barrel");
        Recipes.forEach(rm, BrewBarrelRecipe.class, (id, r) -> {
            MachineDescriptor d = new MachineDescriptor();
            for (Ingredient ing : r.getIngredients()) d.itemIn(EmiIngredient.of(ing));
            ItemStack bottle = r.getBottle();
            if (bottle != null && !bottle.isEmpty()) d.catalyst(EmiStack.of(bottle));
            d.itemOut(EmiStack.of(r.getResultItem(REG)));
            ItemStack extra = r.getContainerOverride();
            if (extra != null && !extra.isEmpty()) d.itemOut(EmiStack.of(extra));
            if (r.getBrewingTime() > 0) d.info(Component.literal(Categories.seconds(r.getBrewingTime())));
            reg.addRecipe(new GenericEmiRecipe(barrel, id, d));
        });

        EmiRecipeCategory jar = Categories.machine(reg, "slavic_pickling", "slavic_delight:jar", "Pickling");
        Recipes.forEach(rm, JarRecipe.class, (id, r) -> {
            MachineDescriptor d = new MachineDescriptor();
            for (Ingredient ing : r.getIngredients()) d.itemIn(EmiIngredient.of(ing));
            d.itemOut(EmiStack.of(r.getResultItem(REG)));
            if (r.getCookTime() > 0) d.info(Component.literal(Categories.seconds(r.getCookTime())));
            reg.addRecipe(new GenericEmiRecipe(jar, id, d));
        });
    }
}
