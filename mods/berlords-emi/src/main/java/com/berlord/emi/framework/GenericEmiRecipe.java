package com.berlord.emi.framework;

import dev.emi.emi.api.recipe.BasicEmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.SlotWidget;
import dev.emi.emi.api.widget.WidgetHolder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

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

    private static int computeWidth(MachineDescriptor d) {
        int cells = d.inputCells() + d.outputCells();
        return Math.max(MIN_WIDTH, cells * SLOT + ARROW_W + PAD * 4);
    }

    private static int computeHeight(MachineDescriptor d) {
        int infoH = d.info.isEmpty() ? 0 : PAD + d.info.size() * TEXT_H;
        return PAD + SLOT + infoH + PAD;
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
        for (Component line : d.info) {
            w.addText(line, PAD, ty, 0xFF404040, false);
            ty += TEXT_H;
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
