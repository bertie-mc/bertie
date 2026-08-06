package io.github.bertie_mc.bertieprogression.emi;

import dev.emi.emi.api.recipe.BasicEmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.WidgetHolder;
import java.util.List;
import net.minecraft.resources.ResourceLocation;

/** One data-driven Ominous Fan conversion displayed as a simple input-to-output recipe. */
final class OminousFanEmiRecipe extends BasicEmiRecipe {
    private static final int SLOT = 18;
    private static final int ARROW_WIDTH = 24;
    private static final int ARROW_HEIGHT = 17;
    private static final int PADDING = 2;
    private static final int WIDTH = SLOT * 2 + ARROW_WIDTH + PADDING * 4;
    private static final int HEIGHT = SLOT + PADDING * 2;

    private final EmiIngredient input;
    private final EmiStack output;

    OminousFanEmiRecipe(EmiRecipeCategory category, ResourceLocation id, EmiIngredient input, EmiStack output) {
        super(category, id, WIDTH, HEIGHT);
        this.input = input;
        this.output = output;
        this.inputs = List.of(input);
        this.outputs = List.of(output);
    }

    @Override
    public void addWidgets(WidgetHolder widgets) {
        int y = PADDING;
        int arrowX = PADDING + SLOT + PADDING;
        widgets.addSlot(input, PADDING, y).recipeContext(this);
        widgets.addFillingArrow(arrowX, y + (SLOT - ARROW_HEIGHT) / 2, 2000);
        widgets.addSlot(output, arrowX + ARROW_WIDTH + PADDING, y).recipeContext(this);
    }
}
