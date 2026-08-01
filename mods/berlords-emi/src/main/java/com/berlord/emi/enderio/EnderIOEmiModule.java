package com.berlord.emi.enderio;

import com.berlord.emi.framework.Categories;
import com.berlord.emi.framework.GenericEmiRecipe;
import com.berlord.emi.framework.MachineDescriptor;
import com.berlord.emi.framework.Recipes;
import com.enderio.enderio.content.machines.alloy.AlloySmeltingRecipe;
import com.enderio.enderio.content.machines.painting.PaintingRecipe;
import com.enderio.enderio.content.machines.sag_mill.SagMillingRecipe;
import com.enderio.enderio.content.machines.slicer.SlicingRecipe;
import com.enderio.enderio.content.machines.soul_binder.SoulBindingRecipe;
import com.enderio.enderio.content.machines.vat.FermentingRecipe;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.neoforge.NeoForgeEmiIngredient;
import dev.emi.emi.api.neoforge.NeoForgeEmiStack;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeManager;
import net.neoforged.neoforge.common.crafting.SizedIngredient;

/**
 * Ender IO machine recipes. The generic FE-machine categories. Deferred: tank (FILL/EMPTY mode branch),
 * enchanting (per-level computed) and fire_crafting (in-world) — see research/enderio-recipe-spec.md.
 */
public final class EnderIOEmiModule {
    private EnderIOEmiModule() {
    }

    public static void register(EmiRegistry reg) {
        RecipeManager rm = reg.getRecipeManager();

        EmiRecipeCategory sag = Categories.machine(reg, "enderio_sag_milling", "enderio:sag_mill", "SAG Mill");
        Recipes.forEach(rm, SagMillingRecipe.class, (id, r) -> {
            MachineDescriptor d = new MachineDescriptor();
            d.itemIn(EmiIngredient.of(r.input()));
            for (var oi : r.outputs()) {
                d.itemOut(EmiStack.of(oi.getItemStack()).setChance(oi.chance()));
            }
            energy(d, r.energy());
            reg.addRecipe(new GenericEmiRecipe(sag, id, d));
        });

        EmiRecipeCategory alloy = Categories.machine(reg, "enderio_alloy_smelting", "enderio:alloy_smelter", "Alloy Smelting");
        Recipes.forEach(rm, AlloySmeltingRecipe.class, (id, r) -> {
            MachineDescriptor d = new MachineDescriptor();
            for (SizedIngredient si : r.inputs()) {
                d.itemIn(NeoForgeEmiIngredient.of(si));
            }
            d.itemOut(EmiStack.of(r.output()));
            energy(d, r.energy());
            if (r.experience() > 0) {
                d.info(Component.literal(r.experience() + " XP"));
            }
            reg.addRecipe(new GenericEmiRecipe(alloy, id, d));
        });

        EmiRecipeCategory paint = Categories.machine(reg, "enderio_painting", "enderio:painting_machine", "Painting");
        Recipes.forEach(rm, PaintingRecipe.class, (id, r) -> {
            MachineDescriptor d = new MachineDescriptor();
            d.itemIn(EmiIngredient.of(r.input()));
            d.itemOut(EmiStack.of(r.output()));
            reg.addRecipe(new GenericEmiRecipe(paint, id, d));
        });

        EmiRecipeCategory soul = Categories.machine(reg, "enderio_soul_binding", "enderio:soul_binder", "Soul Binding");
        Recipes.forEach(rm, SoulBindingRecipe.class, (id, r) -> {
            MachineDescriptor d = new MachineDescriptor();
            d.itemIn(EmiIngredient.of(r.input()));
            d.itemOut(EmiStack.of(r.output()));
            energy(d, r.energy());
            reg.addRecipe(new GenericEmiRecipe(soul, id, d));
        });

        EmiRecipeCategory slice = Categories.machine(reg, "enderio_slicing", "enderio:slice_and_splice", "Slice 'N' Splice");
        Recipes.forEach(rm, SlicingRecipe.class, (id, r) -> {
            MachineDescriptor d = new MachineDescriptor();
            for (Ingredient ing : r.inputs()) {
                d.itemIn(EmiIngredient.of(ing));
            }
            d.itemOut(EmiStack.of(r.output()));
            energy(d, r.energy());
            reg.addRecipe(new GenericEmiRecipe(slice, id, d));
        });

        EmiRecipeCategory vat = Categories.machine(reg, "enderio_vat_fermenting", "enderio:vat", "Vat Fermenting");
        Recipes.forEach(rm, FermentingRecipe.class, (id, r) -> {
            MachineDescriptor d = new MachineDescriptor();
            d.fluidIn(NeoForgeEmiIngredient.of(r.input()));
            d.itemIn(EmiIngredient.of(r.firstReagent()));
            d.itemIn(EmiIngredient.of(r.secondReagent()));
            d.fluidOut(NeoForgeEmiStack.of(r.output()));
            if (r.ticks() > 0) {
                d.info(Component.literal(Categories.seconds(r.ticks())));
            }
            reg.addRecipe(new GenericEmiRecipe(vat, id, d));
        });
    }

    private static void energy(MachineDescriptor d, int fe) {
        if (fe > 0) {
            d.info(Component.literal(fe + " FE"));
        }
    }
}
