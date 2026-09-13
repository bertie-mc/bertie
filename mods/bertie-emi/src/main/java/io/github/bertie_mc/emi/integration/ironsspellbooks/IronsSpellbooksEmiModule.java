package io.github.bertie_mc.emi.integration.ironsspellbooks;

import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.neoforge.NeoForgeEmiStack;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import io.github.bertie_mc.emi.framework.Categories;
import io.github.bertie_mc.emi.framework.GenericEmiRecipe;
import io.github.bertie_mc.emi.framework.MachineDescriptor;
import io.github.bertie_mc.emi.framework.Recipes;
import io.redspace.ironsspellbooks.recipe_types.alchemist_cauldron.EmptyAlchemistCauldronRecipe;
import io.redspace.ironsspellbooks.recipe_types.alchemist_cauldron.FillAlchemistCauldronRecipe;

/**
 * Iron's Spellbooks Alchemist Cauldron — fill (item -> fluid + returned item) and empty (item + fluid
 * -> item), both on {@code irons_spellbooks:alchemist_cauldron}.
 *
 * <p>Brewing is deliberately absent. Extra Mod Integrations ships an Iron's Spellbooks module that
 * already covers it, along with the Scroll Forge, the Arcane Anvil and a page per spell, so a brew
 * category here only produced a second tab showing the same recipes. It covers neither fill nor
 * empty, which is what is left.
 */
public final class IronsSpellbooksEmiModule {
    private IronsSpellbooksEmiModule() {}

    private static final String CAULDRON = "irons_spellbooks:alchemist_cauldron";

    public static void register(EmiRegistry reg) {
        EmiRecipeCategory fill = Categories.machine(reg, "irons_cauldron_fill", CAULDRON, "Alchemist Cauldron: Fill");
        Recipes.forEach(reg.getRecipeManager(), FillAlchemistCauldronRecipe.class, (id, r) -> {
            MachineDescriptor d = new MachineDescriptor();
            d.itemIn(EmiIngredient.of(r.input()));
            d.fluidOut(NeoForgeEmiStack.of(r.result()));
            d.itemOut(EmiStack.of(r.returned()));
            reg.addRecipe(new GenericEmiRecipe(fill, id, d));
        });

        EmiRecipeCategory empty =
                Categories.machine(reg, "irons_cauldron_empty", CAULDRON, "Alchemist Cauldron: Empty");
        Recipes.forEach(reg.getRecipeManager(), EmptyAlchemistCauldronRecipe.class, (id, r) -> {
            MachineDescriptor d = new MachineDescriptor();
            d.itemIn(EmiIngredient.of(r.input()));
            d.fluidIn(NeoForgeEmiStack.of(r.fluid()));
            d.itemOut(EmiStack.of(r.result()));
            reg.addRecipe(new GenericEmiRecipe(empty, id, d));
        });
    }
}
