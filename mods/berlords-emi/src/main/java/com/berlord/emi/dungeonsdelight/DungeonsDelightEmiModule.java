package com.berlord.emi.dungeonsdelight;

import com.berlord.emi.framework.Categories;
import com.berlord.emi.framework.GenericEmiRecipe;
import com.berlord.emi.framework.MachineDescriptor;
import com.berlord.emi.framework.Recipes;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.crafting.Ingredient;
import net.yirmiri.dungeonsdelight.common.block.monster_pot.MonsterPotRecipe;

/** DungeonsDelight Monster Cooking (standalone cooking-pot recipe, not a Farmer's Delight subclass). */
public final class DungeonsDelightEmiModule {
    private DungeonsDelightEmiModule() {
    }

    public static void register(EmiRegistry reg) {
        EmiRecipeCategory cat = Categories.machine(reg,
                "dungeonsdelight_monster_cooking", "dungeonsdelight:monster_pot", "Monster Cooking");
        Recipes.forEach(reg.getRecipeManager(), MonsterPotRecipe.class, (id, r) -> {
            MachineDescriptor d = new MachineDescriptor();
            for (Ingredient ing : r.getIngredients()) d.itemIn(EmiIngredient.of(ing));
            d.itemOut(EmiStack.of(r.getResultItem(RegistryAccess.EMPTY)));
            if (r.getCookTime() > 0) d.info(Component.literal(Categories.seconds(r.getCookTime())));
            if (r.getExperience() > 0) d.info(Component.literal(r.getExperience() + " XP"));
            reg.addRecipe(new GenericEmiRecipe(cat, id, d));
        });
    }
}
