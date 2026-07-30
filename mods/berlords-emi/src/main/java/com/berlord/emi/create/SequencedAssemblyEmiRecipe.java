package com.berlord.emi.create;

import dev.emi.emi.api.recipe.BasicEmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.WidgetHolder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

/**
 * Bespoke layout for {@code create:sequenced_assembly} (Precision Mechanism &amp; other chain crafts):
 * the starting ingredient, then one column per sequenced step showing the step's machine (as a
 * non-consumed catalyst badge on the top row) above the item/fluid applied at that step (bottom row),
 * then the weighted result(s). The transitional "incomplete" item sits above the starting ingredient;
 * the loop count is shown below. Modelled on Create's own JEI {@code SequencedAssemblyCategory}
 * (start -> numbered machine sub-steps -> chanced result) and on the sibling bespoke
 * {@link MechanicalCraftingEmiRecipe}. Only ever constructed when {@code create} is loaded.
 */
public final class SequencedAssemblyEmiRecipe extends BasicEmiRecipe {

    /**
     * One sequenced step: the machine(s) that perform it, the item/fluid applied on the belt at that
     * step (either list may be empty, e.g. pressing/cutting apply nothing), and a short operation label
     * ("Deploying"/"Pressing"/...) shown as the machine's tooltip.
     */
    public record Step(List<EmiStack> machines, List<EmiIngredient> appliedItems,
                       List<EmiIngredient> appliedFluids, Component label) {
    }

    private static final int SLOT = 18;
    private static final int ARROW_W = 24;
    private static final int ARROW_H = 17;
    private static final int PAD = 2;
    private static final int TEXT_H = 10;
    private static final int STEP_GAP = 2;

    private final EmiIngredient start;
    private final List<Step> steps;
    private final EmiStack transitional;
    private final int loops;
    private final List<EmiStack> results;

    public SequencedAssemblyEmiRecipe(EmiRecipeCategory category, ResourceLocation id, EmiIngredient start,
                                      List<Step> steps, EmiStack transitional, int loops, List<EmiStack> results) {
        super(category, id, computeWidth(start, steps, results), computeHeight(loops));
        this.start = start;
        this.steps = steps;
        this.transitional = transitional;
        this.loops = loops;
        this.results = results;

        this.inputs.add(start);
        for (Step s : steps) {
            this.inputs.addAll(s.appliedItems());
            this.inputs.addAll(s.appliedFluids());
            this.catalysts.addAll(s.machines());
        }
        if (transitional != null && !transitional.isEmpty()) {
            this.catalysts.add(transitional);
        }
        this.outputs.addAll(results);
    }

    /** Flow-row columns a step occupies: its applied item/fluid slots, but never fewer than its machine badges. */
    private static int flowSlots(Step s) {
        int applied = s.appliedItems().size() + s.appliedFluids().size();
        int machines = s.machines().size();
        return applied > 0 ? Math.max(applied, machines) : Math.max(1, machines);
    }

    private static int computeWidth(EmiIngredient start, List<Step> steps, List<EmiStack> results) {
        int x = PAD + SLOT + ARROW_W + PAD * 2; // starting ingredient + arrow
        for (Step s : steps) {
            x += flowSlots(s) * SLOT + STEP_GAP;
        }
        x += ARROW_W + PAD * 2;                 // arrow to results
        x += Math.max(1, results.size()) * SLOT;
        return x + PAD;
    }

    private static int computeHeight(int loops) {
        int infoH = loops > 1 ? PAD + TEXT_H : 0; // single "Repeats Nx" line
        return PAD + SLOT + SLOT + infoH + PAD;   // machine/transitional row + flow row
    }

    @Override
    public void addWidgets(WidgetHolder w) {
        int yTop = PAD;
        int yFlow = PAD + SLOT;
        int x = PAD;

        // Transitional "incomplete" item above the starting ingredient (searchable, non-consumed).
        if (transitional != null && !transitional.isEmpty()) {
            w.addSlot(transitional, x, yTop).catalyst(true).recipeContext(this);
        }
        w.addSlot(start, x, yFlow).recipeContext(this);
        x += SLOT;

        w.addFillingArrow(x + PAD, yFlow + (SLOT - ARROW_H) / 2, 800);
        x += ARROW_W + PAD * 2;

        for (Step s : steps) {
            int applied = s.appliedItems().size() + s.appliedFluids().size();
            if (applied > 0) {
                int mx = x;
                for (EmiStack m : s.machines()) {
                    w.addSlot(m, mx, yTop).catalyst(true).appendTooltip(labelOf(s)).recipeContext(this);
                    mx += SLOT;
                }
                int drawn = 0;
                for (EmiIngredient a : s.appliedItems()) {
                    w.addSlot(a, x, yFlow).recipeContext(this);
                    x += SLOT;
                    drawn++;
                }
                for (EmiIngredient f : s.appliedFluids()) {
                    w.addTank(f, x, yFlow, SLOT, SLOT, (int) Math.max(1L, f.getAmount())).drawBack(true).recipeContext(this);
                    x += SLOT;
                    drawn++;
                }
                x += (Math.max(drawn, s.machines().size()) - drawn) * SLOT; // room for extra machine badges
            } else if (s.machines().isEmpty()) {
                x += SLOT; // reserve an empty column for a step with neither machine nor applied item
            } else {
                for (EmiStack m : s.machines()) {
                    w.addSlot(m, x, yFlow).catalyst(true).appendTooltip(labelOf(s)).recipeContext(this);
                    x += SLOT;
                }
            }
            x += STEP_GAP;
        }

        w.addFillingArrow(x + PAD, yFlow + (SLOT - ARROW_H) / 2, 800);
        x += ARROW_W + PAD * 2;

        for (EmiStack r : results) {
            w.addSlot(r, x, yFlow).recipeContext(this);
            x += SLOT;
        }

        if (loops > 1) {
            w.addText(Component.literal("Repeats " + loops + "x"), PAD, yFlow + SLOT + PAD, 0xFF404040, false);
        }
    }

    private static Component labelOf(Step s) {
        return s.label() != null ? s.label() : Component.empty();
    }
}
