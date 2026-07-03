package com.berlord.emi.forbiddenarcanus;

import com.berlord.emi.framework.Categories;
import com.berlord.emi.framework.GenericEmiRecipe;
import com.berlord.emi.framework.MachineDescriptor;
import com.berlord.emi.framework.Recipes;
import com.stal111.forbidden_arcanus.common.item.crafting.ClibanoRecipe;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

/**
 * Forbidden & Arcanus — Clibano Combustion (the multi-fuel smelter). 1-2 item inputs -> result, with an
 * optional chance "residue" second output, plus fire-type / time / xp info lines. The Hephaestus Forge
 * ritual system and apply_modifier (vanilla smithing) are deferred — see PROJECT.md.
 */
public final class ForbiddenArcanusEmiModule {
    private ForbiddenArcanusEmiModule() {
    }

    public static void register(EmiRegistry reg) {
        EmiRecipeCategory clibano = Categories.machine(reg,
                "fa_clibano_combustion", "forbidden_arcanus:clibano_core", "Clibano Combustion");
        Recipes.forEach(reg.getRecipeManager(), ClibanoRecipe.class, (id, r) -> {
            MachineDescriptor d = new MachineDescriptor();
            for (Ingredient ing : r.getIngredients()) {
                d.itemIn(EmiIngredient.of(ing));
            }
            d.itemOut(EmiStack.of(r.result()));
            // optional chance residue (second output)
            try {
                r.residueChance().ifPresent(rc -> {
                    ItemStack residue = rc.type().value().combineInfo().result();
                    if (!residue.isEmpty()) {
                        d.itemOut(EmiStack.of(residue).setChance((float) rc.chance()));
                    }
                });
            } catch (Throwable ignored) {
            }
            try {
                if (r.requiredFireType() != null) {
                    d.info(Component.literal("Fire: " + Categories.capitalize(r.requiredFireType().getSerializedName())));
                }
            } catch (Throwable ignored) {
            }
            if (r.getDefaultCookingTime() > 0) {
                d.info(Component.literal(Categories.seconds(r.getDefaultCookingTime())));
            }
            if (r.getExperience() > 0) {
                d.info(Component.literal(r.getExperience() + " XP"));
            }
            reg.addRecipe(new GenericEmiRecipe(clibano, id, d));
        });
    }
}
