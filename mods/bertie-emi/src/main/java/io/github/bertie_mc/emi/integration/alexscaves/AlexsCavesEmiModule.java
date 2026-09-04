package io.github.bertie_mc.emi.integration.alexscaves;

import com.github.alexmodguy.alexscaves.server.recipe.NuclearFurnaceRecipe;
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
 * Alex's Caves — the Nuclear Furnace. It is an {@code AbstractCookingRecipe} of its own type, so EMI
 * never picks it up with the vanilla furnace categories despite looking exactly like one.
 *
 * <p>The Spelunkery Table is deliberately absent: its JEI entries are synthesised at plugin time from
 * the mod's block-to-ore mapping rather than read from recipes, and Advanced Loot Info already answers
 * the same question from the loot side.
 */
public final class AlexsCavesEmiModule {

    private AlexsCavesEmiModule() {}

    public static void register(EmiRegistry reg) {
        EmiRecipeCategory furnace = Categories.machine(
                reg, "alexscaves_nuclear_furnace", "alexscaves:nuclear_furnace_component", "Nuclear Furnace");
        Recipes.forEach(reg.getRecipeManager(), NuclearFurnaceRecipe.class, (id, recipe) -> {
            MachineDescriptor d = new MachineDescriptor();
            for (Ingredient ingredient : recipe.getIngredients()) {
                d.itemIn(EmiIngredient.of(ingredient));
            }
            d.itemOut(EmiStack.of(
                    recipe.getResultItem(Minecraft.getInstance().level.registryAccess())));
            if (recipe.getCookingTime() > 0) {
                d.info(Component.literal(Categories.seconds(recipe.getCookingTime())));
            }
            if (recipe.getExperience() > 0) {
                d.info(Component.literal(recipe.getExperience() + " XP"));
            }
            reg.addRecipe(new GenericEmiRecipe(furnace, id, d));
        });
    }
}
