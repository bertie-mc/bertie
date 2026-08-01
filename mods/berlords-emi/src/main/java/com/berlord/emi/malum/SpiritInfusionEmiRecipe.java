package com.berlord.emi.malum;

import dev.emi.emi.api.recipe.BasicEmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.WidgetHolder;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

/**
 * Bespoke "Spirit Infusion" (Spirit Altar) layout, inspired by Malum's own JEI category: the primary
 * input and the result stacked down the centre with a downward arrow between them, the spirit shards
 * gathered on the LEFT and the extra inputs on the RIGHT. Replaces the generic single row, which laid
 * ~16 slots straight off the panel edge.
 *
 * <p>EMI bounds a recipe's display height to its panel, so a tall single column of a dozen shards
 * overflows the bottom (and Malum's own 142x185 page texture is likewise too tall to reuse as a
 * background). Instead of stretching taller, each side <b>wraps</b> into up to {@link #MAX_COLS}
 * columns of at most {@link #MAX_ROWS} rows, spending the recipe's spare horizontal room so the height
 * stays fixed at the compact size that fits. Only constructed when {@code malum} is loaded.
 */
public final class SpiritInfusionEmiRecipe extends BasicEmiRecipe {

    private static final int W = 142;
    private static final int SLOT = 18;
    private static final int MAX_ROWS = 6;    // rows before a side wraps into another column
    private static final int MAX_COLS = 2;    // columns each side can spread into (bounded by width)
    // Centre stack + the inner column anchors, mirroring Malum's JEI spacing.
    private static final int PRIMARY_X = 63, PRIMARY_Y = 57;
    private static final int OUTPUT_X = 63, OUTPUT_Y = 124;
    private static final int SPIRIT_X = 20;   // left column, wraps further left
    private static final int EXTRA_X = 104;   // right column, wraps further right
    private static final int COL_CENTER_Y = 49;

    private final EmiIngredient primary;
    private final List<EmiIngredient> extras;
    private final List<EmiIngredient> spirits;
    private final EmiStack result;

    public SpiritInfusionEmiRecipe(EmiRecipeCategory category, ResourceLocation id,
                                   EmiIngredient primary, List<EmiIngredient> extras,
                                   List<EmiIngredient> spirits, EmiStack result) {
        super(category, id, W, height(spirits.size(), extras.size()));
        this.primary = primary;
        this.extras = extras;
        this.spirits = spirits;
        this.result = result;

        this.inputs.add(primary);
        this.inputs.addAll(spirits);
        this.inputs.addAll(extras);
        this.outputs.add(result);
    }

    private static int height(int spiritCount, int extraCount) {
        int bottom = OUTPUT_Y - 1 + SLOT;                          // the centre stack sets the base height
        bottom = Math.max(bottom, gridBottom(spiritCount));
        bottom = Math.max(bottom, gridBottom(extraCount));
        return bottom + 2;
    }

    private static int columns(int n) {
        return Math.min(MAX_COLS, Math.max(1, (n + MAX_ROWS - 1) / MAX_ROWS));
    }

    /** Bottom edge of a wrapped grid of {@code n} items (0 if empty). */
    private static int gridBottom(int n) {
        if (n <= 0) {
            return 0;
        }
        int rows = (n + columns(n) - 1) / columns(n);
        return (firstTop(rows) - 1) + (rows - 1) * SLOT + SLOT;
    }

    /** Top Y of the first row of a column of {@code rows}, vertically centred like Malum's JEI page. */
    private static int firstTop(int rows) {
        return COL_CENTER_Y - 9 * (rows - 1);
    }

    @Override
    public void addWidgets(WidgetHolder w) {
        w.addSlot(primary, PRIMARY_X - 1, PRIMARY_Y - 1).recipeContext(this);
        w.addSlot(result, OUTPUT_X - 1, OUTPUT_Y - 1).recipeContext(this);

        // Downward arrow from primary to result (EMI ships only a horizontal one, so draw our own).
        w.addDrawable(PRIMARY_X + 3, PRIMARY_Y + 36, 9, 11, (g, mx, my, delta) -> drawDownArrow(g));

        grid(w, spirits, SPIRIT_X, true);   // spirits wrap leftward
        grid(w, extras, EXTRA_X, false);    // extras wrap rightward
    }

    /** Lay items column-major into up to {@link #MAX_COLS} columns, wrapping away from the centre. */
    private void grid(WidgetHolder w, List<EmiIngredient> items, int anchorX, boolean growLeft) {
        int n = items.size();
        if (n == 0) {
            return;
        }
        int cols = columns(n);
        int rows = (n + cols - 1) / cols;
        int top = firstTop(rows);
        for (int i = 0; i < n; i++) {
            int col = i / rows;
            int row = i % rows;
            int x = growLeft ? anchorX - col * SLOT : anchorX + col * SLOT;
            w.addSlot(items.get(i), x - 1, top + row * SLOT - 1).recipeContext(this);
        }
    }

    /** A small downward arrow (dark grey), drawn relative to the drawable's own translated origin. */
    private static void drawDownArrow(GuiGraphics g) {
        int c = 0xFF404040;
        g.fill(3, 0, 6, 5, c);   // shaft
        g.fill(0, 5, 9, 6, c);   // head, widest row
        g.fill(1, 6, 8, 7, c);
        g.fill(2, 7, 7, 8, c);
        g.fill(3, 8, 6, 9, c);
        g.fill(4, 9, 5, 10, c);  // tip
    }
}
