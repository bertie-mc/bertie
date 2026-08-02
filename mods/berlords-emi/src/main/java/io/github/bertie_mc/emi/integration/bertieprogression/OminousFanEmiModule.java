package io.github.bertie_mc.emi.integration.bertieprogression;

import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import io.github.bertie_mc.emi.framework.Categories;
import io.github.bertie_mc.emi.framework.GenericEmiRecipe;
import io.github.bertie_mc.emi.framework.MachineDescriptor;
import io.github.bertie_mc.emi.framework.Recipes;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;

/**
 * bertieprogression — an Encased Fan blowing through Twilight Forest's Ominous Fire, the fifth fan
 * processing type alongside Create's water / fire / soul fire / lava.
 *
 * <p>Matched by recipe TYPE id, not by class: bertieprogression is a separately published sibling
 * mod, and nothing else in this module compiles against one.
 */
public final class OminousFanEmiModule {

    private static final ResourceLocation TYPE =
            ResourceLocation.fromNamespaceAndPath("bertieprogression", "ominous_fan");

    private OminousFanEmiModule() {
    }

    public static void register(EmiRegistry reg) {
        EmiRecipeCategory cat = Categories.machine(
                reg, "bertieprogression_ominous_fan", "twilightforest:ominous_candle", "Ominous Fan Blowing");
        Recipes.forEachOfType(reg.getRecipeManager(), TYPE, (id, r) -> {
            MachineDescriptor d = new MachineDescriptor();
            r.getIngredients().forEach(ing -> d.itemIn(EmiIngredient.of(ing)));
            d.itemOut(EmiStack.of(r.getResultItem(RegistryAccess.EMPTY)));
            reg.addRecipe(new GenericEmiRecipe(cat, id, d));
        });
    }
}
