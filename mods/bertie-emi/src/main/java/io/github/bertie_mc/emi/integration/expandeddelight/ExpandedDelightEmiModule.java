package io.github.bertie_mc.emi.integration.expandeddelight;

import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import ianm1647.expandeddelight.common.crafting.JuicerRecipe;
import io.github.bertie_mc.emi.framework.Categories;
import io.github.bertie_mc.emi.framework.GenericEmiRecipe;
import io.github.bertie_mc.emi.framework.MachineDescriptor;
import io.github.bertie_mc.emi.framework.Recipes;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.crafting.Ingredient;

/** ExpandedDelight — the Juicer (item(s) -> juice item). */
public final class ExpandedDelightEmiModule {
    private ExpandedDelightEmiModule() {}

    public static void register(EmiRegistry reg) {
        EmiRecipeCategory cat = Categories.machine(reg, "expandeddelight_juicing", "expandeddelight:juicer", "Juicing");
        Recipes.forEach(reg.getRecipeManager(), JuicerRecipe.class, (id, r) -> {
            MachineDescriptor d = new MachineDescriptor();
            for (Ingredient ing : r.getIngredients()) d.itemIn(EmiIngredient.of(ing));
            d.itemOut(EmiStack.of(r.getResultItem(RegistryAccess.EMPTY)));
            if (r.getJuiceTime() > 0) d.info(Component.literal(Categories.seconds(r.getJuiceTime())));
            reg.addRecipe(new GenericEmiRecipe(cat, id, d));
        });
    }
}
