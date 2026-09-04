package io.github.bertie_mc.emi.integration.alexsmobs;

import com.github.alexthe666.alexsmobs.AlexsMobs;
import com.github.alexthe666.alexsmobs.misc.CapsidRecipe;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import io.github.bertie_mc.emi.framework.Categories;
import io.github.bertie_mc.emi.framework.GenericEmiRecipe;
import io.github.bertie_mc.emi.framework.MachineDescriptor;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Ingredient;

/**
 * Alex's Mobs — the Capsid, which transmutes items dropped into it. Its recipes are loaded by the
 * mod's own reload listener rather than the RecipeManager, so they carry no ids and are reachable
 * only through the mod's proxy; the ids here are therefore built from the result.
 */
public final class AlexsMobsEmiModule {

    private AlexsMobsEmiModule() {}

    public static void register(EmiRegistry reg) {
        EmiRecipeCategory capsid = Categories.machine(reg, "alexsmobs_capsid", "alexsmobs:capsid", "Capsid");
        List<CapsidRecipe> recipes = AlexsMobs.PROXY.getCapsidRecipeManager().getCapsidRecipes();
        int ordinal = 0;
        for (CapsidRecipe recipe : recipes) {
            MachineDescriptor d = new MachineDescriptor();
            for (Ingredient ingredient : recipe.getIngredients()) {
                d.itemInMerged(EmiIngredient.of(ingredient));
            }
            d.itemOut(EmiStack.of(recipe.getResult()));
            if (recipe.getTime() > 0) {
                d.info(Component.literal(Categories.seconds(recipe.getTime())));
            }
            reg.addRecipe(new GenericEmiRecipe(capsid, id(recipe, ordinal++), d));
        }
    }

    private static ResourceLocation id(CapsidRecipe recipe, int ordinal) {
        ResourceLocation result = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(
                recipe.getResult().getItem());
        return ResourceLocation.fromNamespaceAndPath(
                "bertieemi", "alexsmobs/capsid/" + result.getNamespace() + "/" + result.getPath() + "/" + ordinal);
    }
}
