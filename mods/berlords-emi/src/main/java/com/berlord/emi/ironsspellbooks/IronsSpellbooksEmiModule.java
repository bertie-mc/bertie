package com.berlord.emi.ironsspellbooks;

import com.berlord.emi.framework.Categories;
import com.berlord.emi.framework.GenericEmiRecipe;
import com.berlord.emi.framework.MachineDescriptor;
import com.berlord.emi.framework.Recipes;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.neoforge.NeoForgeEmiStack;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import io.redspace.ironsspellbooks.recipe_types.alchemist_cauldron.BrewAlchemistCauldronRecipe;
import io.redspace.ironsspellbooks.recipe_types.alchemist_cauldron.EmptyAlchemistCauldronRecipe;
import io.redspace.ironsspellbooks.recipe_types.alchemist_cauldron.FillAlchemistCauldronRecipe;
import net.neoforged.neoforge.fluids.FluidStack;

/**
 * Iron's Spellbooks Alchemist Cauldron — three recipe types, all on {@code irons_spellbooks:alchemist_cauldron}:
 * brew (fluid + item -> fluids [+ byproduct]), fill (item -> fluid + returned item), empty (item + fluid -> item).
 */
public final class IronsSpellbooksEmiModule {
    private IronsSpellbooksEmiModule() {
    }

    private static final String CAULDRON = "irons_spellbooks:alchemist_cauldron";

    public static void register(EmiRegistry reg) {
        EmiRecipeCategory brew = Categories.machine(reg, "irons_cauldron_brew", CAULDRON, "Alchemist Cauldron: Brew");
        Recipes.forEach(reg.getRecipeManager(), BrewAlchemistCauldronRecipe.class, (id, r) -> {
            MachineDescriptor d = new MachineDescriptor();
            d.fluidIn(NeoForgeEmiStack.of(r.fluidIn()));
            d.itemIn(EmiIngredient.of(r.reagent()));
            for (FluidStack f : r.results()) {
                d.fluidOut(NeoForgeEmiStack.of(f));
            }
            r.byproduct().ifPresent(bp -> d.itemOut(EmiStack.of(bp)));
            reg.addRecipe(new GenericEmiRecipe(brew, id, d));
        });

        EmiRecipeCategory fill = Categories.machine(reg, "irons_cauldron_fill", CAULDRON, "Alchemist Cauldron: Fill");
        Recipes.forEach(reg.getRecipeManager(), FillAlchemistCauldronRecipe.class, (id, r) -> {
            MachineDescriptor d = new MachineDescriptor();
            d.itemIn(EmiIngredient.of(r.input()));
            d.fluidOut(NeoForgeEmiStack.of(r.result()));
            d.itemOut(EmiStack.of(r.returned()));
            reg.addRecipe(new GenericEmiRecipe(fill, id, d));
        });

        EmiRecipeCategory empty = Categories.machine(reg, "irons_cauldron_empty", CAULDRON, "Alchemist Cauldron: Empty");
        Recipes.forEach(reg.getRecipeManager(), EmptyAlchemistCauldronRecipe.class, (id, r) -> {
            MachineDescriptor d = new MachineDescriptor();
            d.itemIn(EmiIngredient.of(r.input()));
            d.fluidIn(NeoForgeEmiStack.of(r.fluid()));
            d.itemOut(EmiStack.of(r.result()));
            reg.addRecipe(new GenericEmiRecipe(empty, id, d));
        });
    }
}
