package com.berlord.emi.malum;

import com.berlord.emi.framework.Categories;
import com.berlord.emi.framework.GenericEmiRecipe;
import com.berlord.emi.framework.MachineDescriptor;
import com.berlord.emi.framework.Recipes;
import com.sammy.malum.common.recipe.RuneworkingRecipe;
import com.sammy.malum.common.recipe.SpiritFocusingRecipe;
import com.sammy.malum.common.recipe.SpiritInfusionRecipe;
import com.sammy.malum.common.recipe.UnchainedTransmutationRecipe;
import com.sammy.malum.common.recipe.VoidFavorRecipe;
import com.sammy.malum.core.systems.recipe.SpiritIngredient;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.neoforge.NeoForgeEmiIngredient;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.crafting.RecipeManager;
import net.neoforged.neoforge.common.crafting.SizedIngredient;

/**
 * Malum's magic crafting. Spirit ingredients render as their shard item with the required count.
 * Generic types only; deferred: spirit_repair (damaged->repaired N-rows) and soul_binding (output is a
 * GeasEffectType, not an item). Node smelting/blasting already show in EMI's vanilla furnace categories.
 * Reads each recipe's own public fields per research/malum-recipe-spec.md.
 */
public final class MalumEmiModule {
    private MalumEmiModule() {
    }

    public static void register(EmiRegistry reg) {
        RecipeManager rm = reg.getRecipeManager();

        EmiRecipeCategory infusion = Categories.machine(reg, "malum_spirit_infusion", "malum:spirit_altar", "Spirit Infusion");
        Recipes.forEach(rm, SpiritInfusionRecipe.class, (id, r) -> {
            MachineDescriptor d = new MachineDescriptor();
            d.itemIn(NeoForgeEmiIngredient.of(r.input));
            for (SizedIngredient si : r.extraInputs) {
                d.itemIn(NeoForgeEmiIngredient.of(si));
            }
            for (SpiritIngredient sp : r.spirits) {
                d.itemIn(spirit(sp));
            }
            d.itemOut(EmiStack.of(r.result));
            reg.addRecipe(new GenericEmiRecipe(infusion, id, d));
        });

        EmiRecipeCategory rune = Categories.machine(reg, "malum_runeworking", "malum:runic_workbench", "Runeworking");
        Recipes.forEach(rm, RuneworkingRecipe.class, (id, r) -> {
            MachineDescriptor d = new MachineDescriptor();
            d.itemIn(NeoForgeEmiIngredient.of(r.input));
            d.itemIn(NeoForgeEmiIngredient.of(r.secondaryInput));
            d.itemOut(EmiStack.of(r.output));
            reg.addRecipe(new GenericEmiRecipe(rune, id, d));
        });

        EmiRecipeCategory focus = Categories.machine(reg, "malum_spirit_focusing", "malum:spirit_crucible", "Spirit Focusing");
        Recipes.forEach(rm, SpiritFocusingRecipe.class, (id, r) -> {
            MachineDescriptor d = new MachineDescriptor();
            d.itemIn(EmiIngredient.of(r.input));
            for (SpiritIngredient sp : r.spirits) {
                d.itemIn(spirit(sp));
            }
            d.itemOut(EmiStack.of(r.output));
            if (r.time > 0) {
                d.info(Component.literal(Categories.seconds(r.time)));
            }
            reg.addRecipe(new GenericEmiRecipe(focus, id, d));
        });

        EmiRecipeCategory trans = Categories.machine(reg, "malum_transmutation", "malum:arcane_spirit", "Spirit Transmutation");
        Recipes.forEach(rm, UnchainedTransmutationRecipe.class, (id, r) -> {
            MachineDescriptor d = new MachineDescriptor();
            d.itemIn(EmiIngredient.of(r.ingredient));
            d.itemOut(EmiStack.of(r.output));
            reg.addRecipe(new GenericEmiRecipe(trans, id, d));
        });

        EmiRecipeCategory well = Categories.machine(reg, "malum_void_favor", "malum:void_depot", "Weeping Well");
        Recipes.forEach(rm, VoidFavorRecipe.class, (id, r) -> {
            MachineDescriptor d = new MachineDescriptor();
            d.itemIn(EmiIngredient.of(r.input));
            d.itemOut(EmiStack.of(r.result));
            reg.addRecipe(new GenericEmiRecipe(well, id, d));
        });
    }

    private static EmiIngredient spirit(SpiritIngredient sp) {
        return EmiStack.of(sp.asItemStack()).setAmount(Math.max(1, sp.count()));
    }
}
