package io.github.bertie_mc.emi.integration.anvilcraft;

import dev.emi.emi.api.recipe.BasicEmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.SlotWidget;
import dev.emi.emi.api.widget.WidgetHolder;
import io.github.bertie_mc.emi.framework.Categories;
import java.util.ArrayList;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;

/**
 * An anvil drop drawn as the arrangement that performs it: the anvil on top, a downward stroke, and
 * under it the decks the recipe actually needs — the items, the block they are lying on or the
 * cauldron they are floating in, and whatever that is standing on. Almost everything in AnvilCraft
 * is a structure the player builds rather than a machine they fill, and an inputs-arrow-outputs row
 * cannot say which block goes where; it can only add a sentence underneath, which is what this
 * replaces.
 *
 * <p>The shape follows Pastel's Anvil Crushing entry, which draws the same mechanic: anvil stacked
 * above its target on the left, arrow, results on the right. AnvilCraft needs more room than Pastel
 * does — several items under one anvil, up to three decks below them, and results that run past a
 * single slot — so the rows here are counted rather than fixed.
 */
class AnvilDropEmiRecipe extends BasicEmiRecipe {

    private final AnvilDrop drop;

    private static final int SLOT = 18;
    private static final int ARROW_W = 24;
    private static final int ARROW_H = 17;
    private static final int PAD = 2;
    private static final int TEXT_H = 10;
    /** The band between the anvil and what it lands on, holding the downward stroke. */
    private static final int FALL = 8;
    /** Results wrap after this many, so a recipe with a long tail of byproducts stays square. */
    private static final int OUT_PER_ROW = 4;

    private static final int STROKE_W = 7;
    private static final int STROKE_H = 5;
    private static final int STROKE_COLOUR = 0xFF5A5A5A;
    private static final int TEXT_COLOUR = 0xFF404040;

    /** Info text grows the panel to fit one line up to here, then word-wraps instead. */
    private static final int TEXT_GROW_CAP = 200;

    AnvilDropEmiRecipe(EmiRecipeCategory category, ResourceLocation id, AnvilDrop drop) {
        super(category, id, computeWidth(drop), computeHeight(drop));
        this.drop = drop;
        this.inputs = new ArrayList<>(drop.payload);
        if (drop.fluidIn != null) {
            this.inputs.add(drop.fluidIn);
        }
        this.catalysts = new ArrayList<>();
        if (drop.anvil != null && drop.anvilConsumed) {
            this.inputs.add(drop.anvil);
        }
        if (drop.base != null) {
            if (drop.baseConsumed) {
                this.inputs.add(drop.base);
            } else {
                this.catalysts.add(drop.base);
            }
        }
        if (drop.under != null) {
            this.catalysts.add(drop.under);
        }
        this.outputs = new ArrayList<>(drop.outputs);
        if (drop.fluidOut != null) {
            this.outputs.add(drop.fluidOut);
        }
    }

    // --- geometry -------------------------------------------------------------------------

    /** The widest deck of the scene, never fewer than one slot. */
    private static int sceneCells(AnvilDrop d) {
        return Math.max(1, Math.max(d.payload.size(), d.baseCells()));
    }

    private static int sceneWidth(AnvilDrop d) {
        return sceneCells(d) * SLOT;
    }

    private static int sceneHeight(AnvilDrop d) {
        return SLOT + FALL + d.decks() * SLOT;
    }

    private static int outputRows(AnvilDrop d) {
        int cells = d.outputCells();
        return cells == 0 ? 0 : (cells - 1) / OUT_PER_ROW + 1;
    }

    private static int outputWidth(AnvilDrop d) {
        return Math.min(OUT_PER_ROW, d.outputCells()) * SLOT;
    }

    private static int outputsX(AnvilDrop d) {
        return PAD + sceneWidth(d) + PAD * 2 + ARROW_W + PAD * 2;
    }

    /**
     * Mass Inject banks its ingredient and emits nothing on the drop itself, so an entry with no
     * results ends at the scene rather than pointing an arrow at empty panel.
     */
    private static int slotWidth(AnvilDrop d) {
        return d.outputCells() == 0 ? PAD + sceneWidth(d) + PAD : outputsX(d) + outputWidth(d) + PAD;
    }

    private static int computeWidth(AnvilDrop d) {
        int base = slotWidth(d);
        int widest = widestInfoPx(d);
        if (widest <= 0) {
            return base;
        }
        int cap = Math.max(base, TEXT_GROW_CAP);
        return Math.max(base, Math.min(widest + PAD * 2, cap));
    }

    private static int bodyHeight(AnvilDrop d) {
        return Math.max(sceneHeight(d), outputRows(d) * SLOT);
    }

    private static int computeHeight(AnvilDrop d) {
        int lines = infoLineCount(d);
        int infoH = lines == 0 ? 0 : PAD + lines * TEXT_H;
        return PAD + bodyHeight(d) + infoH + PAD;
    }

    private static int widestInfoPx(AnvilDrop d) {
        Font font = font();
        if (d.info.isEmpty() || font == null) {
            return 0;
        }
        int max = 0;
        for (Component line : d.info) {
            max = Math.max(max, font.width(line));
        }
        return max;
    }

    private static int infoLineCount(AnvilDrop d) {
        if (d.info.isEmpty()) {
            return 0;
        }
        Font font = font();
        if (font == null) {
            return d.info.size(); // font not ready: assume one row per line, no wrapping
        }
        int wrap = computeWidth(d) - PAD * 2;
        int n = 0;
        for (Component line : d.info) {
            n += Math.max(1, font.split(line, wrap).size());
        }
        return n;
    }

    private static Font font() {
        Minecraft mc = Minecraft.getInstance();
        return mc == null ? null : mc.font;
    }

    // --- drawing --------------------------------------------------------------------------

    @Override
    public void addWidgets(WidgetHolder w) {
        int sceneW = sceneWidth(drop);
        int y = PAD;

        EmiIngredient anvil = drop.anvil == null ? Categories.stack("minecraft:anvil") : drop.anvil;
        w.addSlot(anvil, PAD + centre(sceneW, SLOT), y).drawBack(false).recipeContext(this);
        y += SLOT;

        stroke(w, PAD + sceneW / 2 - STROKE_W / 2, y + (FALL - STROKE_H) / 2);
        y += FALL;

        if (drop.hasPayload()) {
            int x = PAD + centre(sceneW, drop.payload.size() * SLOT);
            for (EmiIngredient in : drop.payload) {
                w.addSlot(in, x, y).recipeContext(this);
                x += SLOT;
            }
            y += SLOT;
        }
        if (drop.baseCells() > 0) {
            int x = PAD + centre(sceneW, drop.baseCells() * SLOT);
            if (drop.base != null) {
                scenery(w, drop.base, x, y, !drop.baseConsumed);
                x += SLOT;
            }
            if (drop.fluidIn != null) {
                tank(w, drop.fluidIn, x, y);
            }
            y += SLOT;
        }
        if (drop.under != null) {
            scenery(w, drop.under, PAD + centre(sceneW, SLOT), y, true);
        }

        int body = bodyHeight(drop);
        if (drop.outputCells() > 0) {
            w.addFillingArrow(PAD + sceneW + PAD * 2, PAD + centre(body, ARROW_H), 2000);
        }

        int outX = outputsX(drop);
        int outY = PAD + centre(body, outputRows(drop) * SLOT);
        int cell = 0;
        for (EmiStack out : drop.outputs) {
            w.addSlot(out, outX + cell % OUT_PER_ROW * SLOT, outY + cell / OUT_PER_ROW * SLOT)
                    .recipeContext(this);
            cell++;
        }
        if (drop.fluidOut != null) {
            tank(w, drop.fluidOut, outX + cell % OUT_PER_ROW * SLOT, outY + cell / OUT_PER_ROW * SLOT);
        }

        int ty = PAD + body + PAD;
        Font font = font();
        int wrap = getDisplayWidth() - PAD * 2;
        for (Component line : drop.info) {
            if (font == null) {
                w.addText(line, PAD, ty, TEXT_COLOUR, false);
                ty += TEXT_H;
            } else {
                for (FormattedCharSequence seq : font.split(line, wrap)) {
                    w.addText(seq, PAD, ty, TEXT_COLOUR, false);
                    ty += TEXT_H;
                }
            }
        }
    }

    /**
     * A block of the structure rather than a slot the player fills, so it is drawn without a slot
     * frame. The catalyst marker is what says the drop leaves it standing.
     */
    private void scenery(WidgetHolder w, EmiIngredient block, int x, int y, boolean survives) {
        SlotWidget slot = w.addSlot(block, x, y).drawBack(false).recipeContext(this);
        if (survives) {
            slot.catalyst(true);
        }
    }

    /** A fluid rendered slot-sized, so it lines up with the item slots on the same deck. */
    private void tank(WidgetHolder w, EmiIngredient fluid, int x, int y) {
        int capacity = (int) Math.max(1L, fluid.getAmount());
        w.addTank(fluid, x, y, SLOT, SLOT, capacity).drawBack(true).recipeContext(this);
    }

    /** The downward stroke between the anvil and its target: a stem and an arrowhead, drawn in place. */
    private static void stroke(WidgetHolder w, int x, int y) {
        w.addDrawable(x, y, STROKE_W, STROKE_H, (draw, mouseX, mouseY, delta) -> {
            draw.fill(STROKE_W / 2, 0, STROKE_W / 2 + 1, 2, STROKE_COLOUR);
            for (int row = 0; row < 3; row++) {
                draw.fill(row, 2 + row, STROKE_W - row, 3 + row, STROKE_COLOUR);
            }
        });
    }

    private static int centre(int span, int content) {
        return Math.max(0, (span - content) / 2);
    }
}
