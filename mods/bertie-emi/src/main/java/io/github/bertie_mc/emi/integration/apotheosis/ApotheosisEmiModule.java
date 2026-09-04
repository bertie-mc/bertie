package io.github.bertie_mc.emi.integration.apotheosis;

import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.shadowsoffire.apotheosis.affix.salvaging.SalvagingRecipe;
import dev.shadowsoffire.apotheosis.socket.gem.cutting.BasicGemCuttingRecipe;
import dev.shadowsoffire.apotheosis.socket.gem.cutting.PurityUpgradeRecipe;
import io.github.bertie_mc.emi.framework.Categories;
import io.github.bertie_mc.emi.framework.GenericEmiRecipe;
import io.github.bertie_mc.emi.framework.MachineDescriptor;
import io.github.bertie_mc.emi.framework.Recipes;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.crafting.RecipeManager;
import net.neoforged.neoforge.common.crafting.SizedIngredient;

/**
 * Apotheosis' two tables that only ever had a JEI category: the Salvaging Table (an item broken back
 * down into a spread of materials) and the Gem Cutting Table (four slots that either cut a gem out of
 * raw materials or raise an existing gem's purity).
 *
 * <p>Its third JEI category is a set of extensions to vanilla smithing — add sockets, unnaming,
 * withdrawal and the rest. Those are {@code SmithingRecipe}s, so EMI already lists them under
 * Smithing and they are left alone.
 */
public final class ApotheosisEmiModule {

    private ApotheosisEmiModule() {}

    public static void register(EmiRegistry reg) {
        RecipeManager rm = reg.getRecipeManager();
        salvaging(reg, rm);
        gemCutting(reg, rm);
    }

    /**
     * One input, several outputs, each with its own count range. A range is written on the info line
     * rather than as a stack amount because the slot can only show one number.
     */
    private static void salvaging(EmiRegistry reg, RecipeManager rm) {
        EmiRecipeCategory salvaging =
                Categories.machine(reg, "apotheosis_salvaging", "apotheosis:salvaging_table", "Salvaging");
        Recipes.forEach(rm, SalvagingRecipe.class, (id, recipe) -> {
            MachineDescriptor d = new MachineDescriptor();
            d.itemIn(EmiIngredient.of(recipe.getInput()));
            for (SalvagingRecipe.OutputData output : recipe.getOutputs()) {
                if (output.stack().isEmpty()) {
                    continue;
                }
                EmiStack stack = EmiStack.of(output.stack());
                stack.setAmount(Math.max(1, output.min()));
                d.itemOut(stack);
                if (output.max() != output.min()) {
                    d.info(Component.literal(
                            output.stack().getHoverName().getString() + ": " + output.min() + " to " + output.max()));
                }
            }
            reg.addRecipe(new GenericEmiRecipe(salvaging, id, d));
        });
    }

    /**
     * The table has a base slot plus top, left and right material slots, and each material slot
     * accepts any one of a list. A purity upgrade states no output item at all — it lifts whatever gem
     * sits in the base slot one grade — so its target grade becomes the info line.
     */
    private static void gemCutting(EmiRegistry reg, RecipeManager rm) {
        EmiRecipeCategory cutting =
                Categories.machine(reg, "apotheosis_gem_cutting", "apotheosis:gem_cutting_table", "Gem Cutting");
        Recipes.forEach(rm, BasicGemCuttingRecipe.class, (id, recipe) -> {
            MachineDescriptor d = new MachineDescriptor();
            d.itemIn(EmiIngredient.of(recipe.base()));
            materials(d, recipe.top());
            materials(d, recipe.left());
            materials(d, recipe.right());
            d.itemOut(EmiStack.of(recipe.output()));
            reg.addRecipe(new GenericEmiRecipe(cutting, id, d));
        });
        Recipes.forEach(rm, PurityUpgradeRecipe.class, (id, recipe) -> {
            MachineDescriptor d = new MachineDescriptor();
            materials(d, recipe.left());
            materials(d, recipe.right());
            d.info(Component.literal(
                    "Raises a socketable gem to " + recipe.purity().getName()));
            reg.addRecipe(new GenericEmiRecipe(cutting, id, d));
        });
    }

    private static void materials(MachineDescriptor d, List<SizedIngredient> slot) {
        for (SizedIngredient ingredient : slot) {
            d.itemInMerged(EmiIngredient.of(ingredient.ingredient(), ingredient.count()));
        }
    }
}
