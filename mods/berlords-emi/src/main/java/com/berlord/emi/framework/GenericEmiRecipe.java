package com.berlord.emi.framework;

import dev.emi.emi.api.recipe.BasicEmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.SlotWidget;
import dev.emi.emi.api.widget.WidgetHolder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;

import java.util.ArrayList;

/**
 * The reusable recipe widget: a single uniform row of inputs (items then fluids) + catalysts on the
 * left, a filling arrow in the middle, and outputs on the right, with optional info text below. Items
 * render as slots and fluids as slot-sized tanks so both align in one 18px row. Covers the ~90% of
 * machine categories that are "N in -> M out".
 */
public class GenericEmiRecipe extends BasicEmiRecipe {
    private final MachineDescriptor d;

    private static final int SLOT = 18;
    private static final int ARROW_W = 24;
    private static final int ARROW_H = 17;
    private static final int PAD = 2;
    private static final int TEXT_H = 10;
    private static final int MIN_WIDTH = 80;
    /**
     * Info text grows the panel up to this width to fit on one line; anything wider word-wraps
     * instead of widening further. Keeps short lines (e.g. "Requires at least tier 2") on one row
     * while long instruction sentences flow to 2-3 lines rather than clipping off the edge.
     */
    private static final int TEXT_GROW_CAP = 200;

    public GenericEmiRecipe(EmiRecipeCategory category, ResourceLocation id, MachineDescriptor d) {
        super(category, id, computeWidth(d), computeHeight(d));
        this.d = d;
        this.inputs = new ArrayList<>();
        this.inputs.addAll(d.itemInputs);
        this.inputs.addAll(d.fluidInputs);
        this.catalysts = new ArrayList<>(d.catalysts);
        this.outputs = new ArrayList<>();
        this.outputs.addAll(d.itemOutputs);
        this.outputs.addAll(d.fluidOutputs);
    }

    /** Width demanded by the slot row plus any explicit {@link MachineDescriptor#minWidth} floor. */
    private static int baseWidth(MachineDescriptor d) {
        int cells = d.inputCells() + d.outputCells();
        return Math.max(Math.max(MIN_WIDTH, d.minWidth), cells * SLOT + ARROW_W + PAD * 4);
    }

    private static int computeWidth(MachineDescriptor d) {
        int base = baseWidth(d);
        int widest = widestInfoPx(d);
        if (widest <= 0) {
            return base;
        }
        // Grow to fit the widest info line, but never past the cap (or the base, if already wider):
        // beyond the cap the info loop word-wraps instead.
        int cap = Math.max(base, TEXT_GROW_CAP);
        return Math.max(base, Math.min(widest + PAD * 2, cap));
    }

    private static int computeHeight(MachineDescriptor d) {
        int lines = infoLineCount(d);
        int infoH = lines == 0 ? 0 : PAD + lines * TEXT_H;
        return PAD + SLOT + infoH + PAD;
    }

    /** Pixel width of the widest info line (0 if there are none or the font isn't ready yet). */
    private static int widestInfoPx(MachineDescriptor d) {
        if (d.info.isEmpty()) {
            return 0;
        }
        Font font = font();
        if (font == null) {
            return 0;
        }
        int max = 0;
        for (Component line : d.info) {
            max = Math.max(max, font.width(line));
        }
        return max;
    }

    /** Usable text width inside the panel (the wrap width for info lines). */
    private static int wrapWidth(MachineDescriptor d) {
        return computeWidth(d) - PAD * 2;
    }

    /** Total rendered info rows after word-wrapping each line to the panel width. */
    private static int infoLineCount(MachineDescriptor d) {
        if (d.info.isEmpty()) {
            return 0;
        }
        Font font = font();
        if (font == null) {
            return d.info.size(); // font not ready: assume one row per line (no wrapping)
        }
        int w = wrapWidth(d);
        int n = 0;
        for (Component line : d.info) {
            n += Math.max(1, font.split(line, w).size());
        }
        return n;
    }

    private static Font font() {
        Minecraft mc = Minecraft.getInstance();
        return mc == null ? null : mc.font;
    }

    @Override
    public void addWidgets(WidgetHolder w) {
        int y = PAD;
        int x = PAD;

        for (EmiIngredient in : d.itemInputs) {
            placeItem(w, in, x, y, false);
            x += SLOT;
        }
        for (EmiIngredient f : d.fluidInputs) {
            placeTank(w, f, x, y);
            x += SLOT;
        }
        for (EmiIngredient cat : d.catalysts) {
            placeItem(w, cat, x, y, true);
            x += SLOT;
        }

        w.addFillingArrow(x + PAD, y + (SLOT - ARROW_H) / 2, 2000);
        x += ARROW_W + PAD * 2;

        for (EmiStack s : d.itemOutputs) {
            placeItem(w, s, x, y, false);
            x += SLOT;
        }
        for (EmiStack f : d.fluidOutputs) {
            placeTank(w, f, x, y);
            x += SLOT;
        }

        int ty = y + SLOT + PAD;
        Font font = font();
        int wrapW = getDisplayWidth() - PAD * 2;
        for (Component line : d.info) {
            if (font == null) {
                w.addText(line, PAD, ty, 0xFF404040, false);
                ty += TEXT_H;
            } else {
                for (FormattedCharSequence seq : font.split(line, wrapW)) {
                    w.addText(seq, PAD, ty, 0xFF404040, false);
                    ty += TEXT_H;
                }
            }
        }
    }

    private void placeItem(WidgetHolder w, EmiIngredient stack, int x, int y, boolean catalyst) {
        SlotWidget slot = w.addSlot(stack, x, y).recipeContext(this);
        if (catalyst) {
            slot.catalyst(true);
        }
    }

    /** A fluid rendered as a slot-sized tank, so it lines up with the item slots in the same row. */
    private void placeTank(WidgetHolder w, EmiIngredient fluid, int x, int y) {
        int cap = (int) Math.max(1L, fluid.getAmount());
        w.addTank(fluid, x, y, SLOT, SLOT, cap).drawBack(true).recipeContext(this);
    }
}
