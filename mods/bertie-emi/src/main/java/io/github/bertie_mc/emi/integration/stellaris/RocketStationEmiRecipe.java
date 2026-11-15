package io.github.bertie_mc.emi.integration.stellaris;

import dev.emi.emi.api.recipe.BasicEmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.WidgetHolder;
import java.util.List;
import net.minecraft.resources.ResourceLocation;

/** The ingredient order is positional in RocketStationRecipe.matches, not a shapeless list. */
final class RocketStationEmiRecipe extends BasicEmiRecipe {
    RocketStationEmiRecipe(
            EmiRecipeCategory category, ResourceLocation id, List<EmiIngredient> ingredients, EmiStack result) {
        super(category, id, 170, 124);
        this.inputs = List.copyOf(ingredients);
        this.outputs = List.of(result);
    }

    @Override
    public void addWidgets(WidgetHolder widgets) {
        for (int i = 0; i < RocketLayouts.CONSTRUCTION.size(); i++) {
            var position = RocketLayouts.CONSTRUCTION.get(i);
            widgets.addSlot(i < inputs.size() ? inputs.get(i) : EmiStack.EMPTY, position.x(), position.y());
        }
        widgets.addFillingArrow(103, 54, 2000);
        widgets.addSlot(outputs.getFirst(), 136, 50).large(true).recipeContext(this);
    }
}
