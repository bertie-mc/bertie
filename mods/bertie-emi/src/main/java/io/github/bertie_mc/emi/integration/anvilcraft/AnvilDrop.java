package io.github.bertie_mc.emi.integration.anvilcraft;

import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.chat.Component;

/**
 * One anvil drop, described as the thing a player builds rather than as a row of slots: an anvil at
 * the top and, below it, the decks it falls through. {@link AnvilDropEmiRecipe} draws it.
 *
 * <p>The topmost deck is the payload — items lying on something or floating in a cauldron. Under it
 * comes a stack of blocks, in the order they are stacked in the world, because more than one type
 * needs two: Block Smear presses a block onto the one below it and Block Compress squashes a pair
 * into one. Each of those blocks is either left standing or gone afterwards, which is the whole
 * difference between those two categories, so {@link #on} and {@link #onto} record it per deck.
 */
final class AnvilDrop {

    /** One block of the structure, and whether it is still there when the anvil has finished. */
    record Deck(EmiIngredient block, boolean survives, EmiIngredient fluid) {}

    final List<EmiIngredient> payload = new ArrayList<>();
    final List<Deck> decks = new ArrayList<>();
    final List<EmiStack> outputs = new ArrayList<>();
    final List<Component> info = new ArrayList<>();

    EmiIngredient anvil;
    boolean anvilConsumed;
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

    /** An item lying on the top deck, or floating in it when that deck is a cauldron. */
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

    /** The next deck down: a block the drop needs present and leaves standing. */
    AnvilDrop on(EmiIngredient block) {
        return deck(block, true);
    }

    /** The next deck down: a block the drop converts or takes away. */
    AnvilDrop onto(EmiIngredient block) {
        return deck(block, false);
    }

    private AnvilDrop deck(EmiIngredient block, boolean survives) {
        if (present(block)) {
            decks.add(new Deck(block, survives, null));
        }
        return this;
    }

    /**
     * What the deck just added holds, drawn as a tank beside it. No filled cauldron has an item form
     * in vanilla or in AnvilCraft, so the fluid is the only way to say which one the recipe means.
     */
    AnvilDrop fluidIn(EmiIngredient fluid) {
        if (present(fluid) && !decks.isEmpty()) {
            int last = decks.size() - 1;
            Deck deck = decks.get(last);
            decks.set(last, new Deck(deck.block(), deck.survives(), fluid));
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

    AnvilDrop out(EmiStack stack) {
        if (present(stack)) {
            outputs.add(stack);
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

    /** The widest deck: a block and, where there is one, the fluid tank beside it. */
    int widestDeck() {
        int widest = payload.size();
        for (Deck deck : decks) {
            widest = Math.max(widest, deck.fluid() == null ? 1 : 2);
        }
        return widest;
    }

    int outputCells() {
        return outputs.size() + (fluidOut == null ? 0 : 1);
    }

    /** How many rows hang below the anvil: the payload, then one per block of the structure. */
    int rows() {
        return (hasPayload() ? 1 : 0) + decks.size();
    }
}
