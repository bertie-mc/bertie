package io.github.bertie_mc.emi.integration.enigmaticlegacyplus;

import auviotre.enigmatic.legacy.contents.crafting.SpellstoneTableRecipe;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import io.github.bertie_mc.emi.framework.Categories;
import io.github.bertie_mc.emi.framework.GenericEmiRecipe;
import io.github.bertie_mc.emi.framework.MachineDescriptor;
import io.github.bertie_mc.emi.framework.Recipes;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.crafting.Ingredient;

/**
 * Enigmatic Legacy+ — the Spellstone Table. Ingredients plus a cost in Cursed Debris, which the
 * table draws from its own buffer rather than a slot, so the cost is stated as an info line.
 *
 * <p>The mod's other recipe types ({@code cursed_shaped}, {@code shapeless_no_remain}) are crafting
 * recipes, so EMI already lists them under Crafting.
 */
public final class EnigmaticLegacyPlusEmiModule {

    private EnigmaticLegacyPlusEmiModule() {}

    public static void register(EmiRegistry reg) {
        EmiRecipeCategory table = Categories.machine(
                reg, "enigmatic_spellstone_table", "enigmaticlegacyplus:spellstone_table", "Spellstone Table");
        Recipes.forEach(reg.getRecipeManager(), SpellstoneTableRecipe.class, (id, recipe) -> {
            MachineDescriptor d = new MachineDescriptor();
            for (Ingredient ingredient : recipe.getIngredients()) {
                d.itemInMerged(EmiIngredient.of(ingredient));
            }
            d.itemOut(EmiStack.of(
                    recipe.getResultItem(Minecraft.getInstance().level.registryAccess())));
            if (recipe.getCount() > 0) {
                d.info(Component.literal(recipe.getCount() + " Cursed Debris"));
            }
            if (recipe.isAllDifferent()) {
                d.info(Component.literal("Every ingredient must be a different item"));
            }
            reg.addRecipe(new GenericEmiRecipe(table, id, d));
        });
    }
}
