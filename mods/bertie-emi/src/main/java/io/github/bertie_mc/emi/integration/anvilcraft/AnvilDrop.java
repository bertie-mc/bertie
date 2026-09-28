package io.github.bertie_mc.emi.integration.anvilcraft;

import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.chat.Component;

/**
 * One anvil drop, described as the thing a player builds rather than as a row of slots: an anvil at
 * the top and, below it, the decks it falls through — the items lying on or floating in something,
 * the block that something is, and whatever that block is standing on. {@link AnvilDropEmiRecipe}
 * draws it.
 *
 * <p>The base is either machinery the recipe runs on and leaves intact — a Crushing Table, a
 * cauldron, a lit Campfire — or the block the recipe itself converts. {@link #on} and {@link #onto}
 * are the two cases, and which is used decides whether the slot is marked as a catalyst.
 */
final class AnvilDrop {

    final List<EmiIngredient> payload = new ArrayList<>();
    final List<EmiStack> outputs = new ArrayList<>();
    final List<Component> info = new ArrayList<>();

    EmiIngredient anvil;
    boolean anvilConsumed;
    EmiIngredient base;
    boolean baseConsumed;
    EmiIngredient under;
    EmiIngredient fluidIn;
    EmiStack fluidOut;

    /** The anvil to draw. Only the tiered anvils need to say; everything else gets the vanilla one. */
    AnvilDrop anvil(EmiIngredient stack) {
        if (present(stack)) {
            anvil = stack;
        }
        return this;
    }

    /** An anvil the drop destroys, which makes it a cost rather than equipment. */
    AnvilDrop anvilConsumed(EmiIngredient stack) {
        anvilConsumed = present(stack);
        return anvil(stack);
    }

    /** An item lying on the base, or floating in it when the base is a cauldron. */
    AnvilDrop item(EmiIngredient stack) {
        if (present(stack)) {
            payload.add(stack);
        }
        return this;
    }

    /**
     * The same as {@link #item} but folding into an identical slot already present, so a recipe
     * asking for four of one thing reads as one slot of "x4" rather than four copies of the icon.
     */
    AnvilDrop itemMerged(EmiIngredient stack) {
        if (!present(stack)) {
            return this;
        }
        for (EmiIngredient existing : payload) {
            if (sameStacks(existing, stack)) {
                existing.setAmount(existing.getAmount() + stack.getAmount());
                return this;
            }
        }
        return item(stack);
    }

    /** Same set of stacks, ignoring how many of each — that is what makes two slots mergeable. */
    private static boolean sameStacks(EmiIngredient a, EmiIngredient b) {
        List<EmiStack> left = a.getEmiStacks();
        List<EmiStack> right = b.getEmiStacks();
        if (left.size() != right.size()) {
            return false;
        }
        for (int n = 0; n < left.size(); n++) {
            if (!left.get(n).isEqual(right.get(n))) {
                return false;
            }
        }
        return true;
    }

    /** Machinery directly under the payload that the recipe needs present and does not consume. */
    AnvilDrop on(EmiIngredient block) {
        if (present(block)) {
            base = block;
            baseConsumed = false;
        }
        return this;
    }

    /** The block directly under the payload, which this recipe converts into something else. */
    AnvilDrop onto(EmiIngredient block) {
        if (present(block)) {
            base = block;
            baseConsumed = true;
        }
        return this;
    }

    /** A second block below the base — the Heater or lit Campfire a cauldron is standing on. */
    AnvilDrop over(EmiIngredient block) {
        if (present(block)) {
            under = block;
        }
        return this;
    }

    AnvilDrop out(EmiStack stack) {
        if (present(stack)) {
            outputs.add(stack);
        }
        return this;
    }

    /**
     * What the cauldron holds when the anvil lands, drawn as a tank beside it. None of the filled
     * cauldrons — vanilla's or AnvilCraft's — has an item form, so the fluid is the only way to say
     * which one the recipe means.
     */
    AnvilDrop fluidIn(EmiIngredient fluid) {
        if (present(fluid)) {
            fluidIn = fluid;
        }
        return this;
    }

    /** What it holds afterwards. */
    AnvilDrop fluidOut(EmiStack fluid) {
        if (present(fluid)) {
            fluidOut = fluid;
        }
        return this;
    }

    /** A fact the picture cannot carry — a speed threshold, a dimension, a mass total. */
    AnvilDrop note(Component line) {
        if (line != null) {
            info.add(line);
        }
        return this;
    }

    private static boolean present(EmiIngredient stack) {
        return stack != null && !stack.isEmpty();
    }

    boolean hasPayload() {
        return !payload.isEmpty();
    }

    /** Slots across the base deck: the block the payload rests on, and what that block holds. */
    int baseCells() {
        return (base == null ? 0 : 1) + (fluidIn == null ? 0 : 1);
    }

    int outputCells() {
        return outputs.size() + (fluidOut == null ? 0 : 1);
    }

    /** How many decks hang below the anvil: the payload, the base and the block under it. */
    int decks() {
        return (hasPayload() ? 1 : 0) + (baseCells() == 0 ? 0 : 1) + (under == null ? 0 : 1);
    }
}
