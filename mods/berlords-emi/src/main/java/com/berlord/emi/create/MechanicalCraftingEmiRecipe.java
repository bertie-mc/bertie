package com.berlord.emi.create;

import dev.emi.emi.api.recipe.BasicEmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.WidgetHolder;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

/**
 * NxN grid layout for {@code create:mechanical_crafting} (up to 9x9; the bertie pack uses at most
 * 5x5): the shaped pattern as a slot grid, filling arrow, result. The grid list is row-major
 * with {@code gw*gh} entries; empty cells render as empty slots.
 */
public final class MechanicalCraftingEmiRecipe extends BasicEmiRecipe {
    private static final int SLOT = 18;
    private static final int ARROW_W = 24;
    private static final int ARROW_H = 17;
    private static final int PAD = 2;

    private final int gw;
    private final int gh;
    private final List<EmiIngredient> grid;

    public MechanicalCraftingEmiRecipe(EmiRecipeCategory cat, ResourceLocation id,
                                       int gw, int gh, List<EmiIngredient> grid, EmiStack result) {
        super(cat, id, PAD * 2 + gw * SLOT + PAD + ARROW_W + PAD + SLOT + PAD,
                PAD * 2 + Math.max(gh, 1) * SLOT);
        this.gw = gw;
        this.gh = gh;
        this.grid = grid;
        for (EmiIngredient i : grid) {
            if (!i.isEmpty()) {
                this.inputs.add(i);
            }
        }
        this.outputs.add(result);
    }

    @Override
    public void addWidgets(WidgetHolder w) {
        for (int r = 0; r < gh; r++) {
            for (int c = 0; c < gw; c++) {
                int idx = r * gw + c;
                EmiIngredient ing = idx < grid.size() ? grid.get(idx) : EmiStack.EMPTY;
                w.addSlot(ing, PAD + c * SLOT, PAD + r * SLOT).recipeContext(this);
            }
        }
        int cy = PAD + (Math.max(gh, 1) * SLOT) / 2;
        w.addFillingArrow(PAD * 2 + gw * SLOT, cy - ARROW_H / 2, 3000);
        w.addSlot(this.outputs.get(0), PAD * 2 + gw * SLOT + ARROW_W + PAD, cy - SLOT / 2)
                .recipeContext(this);
    }
}
