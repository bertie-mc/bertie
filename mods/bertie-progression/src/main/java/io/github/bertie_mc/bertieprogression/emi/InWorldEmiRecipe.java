package io.github.bertie_mc.bertieprogression.emi;

import dev.emi.emi.api.recipe.BasicEmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.SlotWidget;
import dev.emi.emi.api.widget.WidgetHolder;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;

/** One synthesized in-world interaction: inputs, non-consumed catalysts, outputs and instructions. */
final class InWorldEmiRecipe extends BasicEmiRecipe {
    private static final int SLOT = 18;
    private static final int ARROW_WIDTH = 24;
    private static final int ARROW_HEIGHT = 17;
    private static final int PADDING = 2;
    private static final int TEXT_HEIGHT = 10;
    private static final int TEXT_GROW_CAP = 200;

    private final List<EmiIngredient> itemInputs;
    private final List<EmiIngredient> recipeCatalysts;
    private final List<EmiStack> itemOutputs;
    private final List<Component> info;

    InWorldEmiRecipe(
            EmiRecipeCategory category,
            ResourceLocation id,
            List<EmiIngredient> itemInputs,
            List<EmiIngredient> catalysts,
            List<EmiStack> itemOutputs,
            List<Component> info) {
        super(
                category,
                id,
                computeWidth(itemInputs, catalysts, itemOutputs, info),
                computeHeight(itemInputs, catalysts, itemOutputs, info));
        this.itemInputs = List.copyOf(itemInputs);
        this.recipeCatalysts = List.copyOf(catalysts);
        this.itemOutputs = List.copyOf(itemOutputs);
        this.info = List.copyOf(info);
        this.inputs = this.itemInputs;
        this.catalysts = this.recipeCatalysts;
        this.outputs = this.itemOutputs;
    }

    private static int computeWidth(
            List<EmiIngredient> inputs, List<EmiIngredient> catalysts, List<EmiStack> outputs, List<Component> info) {
        int cells = inputs.size() + catalysts.size() + outputs.size();
        int base = Math.max(160, cells * SLOT + ARROW_WIDTH + PADDING * 4);
        Font font = font();
        if (font == null || info.isEmpty()) {
            return base;
        }
        int widest = 0;
        for (Component line : info) {
            widest = Math.max(widest, font.width(line));
        }
        return Math.max(base, Math.min(widest + PADDING * 2, Math.max(base, TEXT_GROW_CAP)));
    }

    private static int computeHeight(
            List<EmiIngredient> inputs, List<EmiIngredient> catalysts, List<EmiStack> outputs, List<Component> info) {
        int width = computeWidth(inputs, catalysts, outputs, info);
        Font font = font();
        int lines = 0;
        for (Component line : info) {
            lines += font == null
                    ? 1
                    : Math.max(1, font.split(line, width - PADDING * 2).size());
        }
        int infoHeight = lines == 0 ? 0 : PADDING + lines * TEXT_HEIGHT;
        return PADDING + SLOT + infoHeight + PADDING;
    }

    private static Font font() {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft == null ? null : minecraft.font;
    }

    @Override
    public void addWidgets(WidgetHolder widgets) {
        int y = PADDING;
        int x = PADDING;

        for (EmiIngredient input : itemInputs) {
            widgets.addSlot(input, x, y).recipeContext(this);
            x += SLOT;
        }
        for (EmiIngredient catalyst : recipeCatalysts) {
            SlotWidget slot = widgets.addSlot(catalyst, x, y).recipeContext(this);
            slot.catalyst(true);
            x += SLOT;
        }

        widgets.addFillingArrow(x + PADDING, y + (SLOT - ARROW_HEIGHT) / 2, 2000);
        x += ARROW_WIDTH + PADDING * 2;

        for (EmiStack output : itemOutputs) {
            widgets.addSlot(output, x, y).recipeContext(this);
            x += SLOT;
        }

        int textY = y + SLOT + PADDING;
        Font font = font();
        int wrapWidth = getDisplayWidth() - PADDING * 2;
        for (Component line : info) {
            if (font == null) {
                widgets.addText(line, PADDING, textY, 0xFF404040, false);
                textY += TEXT_HEIGHT;
                continue;
            }
            for (FormattedCharSequence sequence : font.split(line, wrapWidth)) {
                widgets.addText(sequence, PADDING, textY, 0xFF404040, false);
                textY += TEXT_HEIGHT;
            }
        }
    }
}
