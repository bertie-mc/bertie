package io.github.bertie_mc.emi.integration.anvilcraft;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.emi.emi.api.stack.EmiStack;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluids;
import org.junit.jupiter.api.Test;

class AnvilDropTest {

    @Test
    void ignoresEmptyEntries() {
        AnvilDrop drop = new AnvilDrop()
                .item(null)
                .item(EmiStack.EMPTY)
                .item(EmiStack.of(Items.STONE))
                .on(EmiStack.EMPTY)
                .over(null)
                .fluidIn(EmiStack.EMPTY)
                .out(EmiStack.EMPTY)
                .note(null);

        assertEquals(1, drop.payload.size());
        assertEquals(0, drop.baseCells());
        assertEquals(0, drop.outputCells());
        assertTrue(drop.info.isEmpty());
        // Only the payload hangs below the anvil, so the scene is one deck deep.
        assertEquals(1, drop.decks());
    }

    @Test
    void identicalPayloadItemsShareOneSlot() {
        AnvilDrop drop = new AnvilDrop()
                .itemMerged(EmiStack.of(Items.COPPER_INGOT).setAmount(2))
                .itemMerged(EmiStack.of(Items.COPPER_INGOT))
                .itemMerged(EmiStack.of(Items.IRON_INGOT));

        assertEquals(2, drop.payload.size());
        assertEquals(3, drop.payload.get(0).getAmount());
    }

    /** A cauldron recipe: items above, the cauldron and what it holds below, a machine under that. */
    @Test
    void cauldronSceneIsThreeDecksWithATankBesideTheBase() {
        AnvilDrop drop = new AnvilDrop()
                .item(EmiStack.of(Items.COPPER_INGOT))
                .on(EmiStack.of(Items.CAULDRON))
                .fluidIn(EmiStack.of(Fluids.WATER, 1_000))
                .over(EmiStack.of(Items.CAMPFIRE))
                .out(EmiStack.of(Items.BRICK))
                .note(Component.literal("The Campfire has to be lit"));

        assertEquals(3, drop.decks());
        assertEquals(2, drop.baseCells());
        assertEquals(1, drop.outputCells());
        assertFalse(drop.baseConsumed);
    }

    @Test
    void aTransformedBaseIsConsumedAndABareBlockDropIsTwoDecks() {
        AnvilDrop drop = new AnvilDrop().onto(EmiStack.of(Items.STONE)).out(EmiStack.of(Items.COBBLESTONE));

        assertTrue(drop.baseConsumed);
        assertFalse(drop.hasPayload());
        assertEquals(1, drop.decks());
    }

    @Test
    void aConsumedAnvilIsRecordedAsWellAsDrawn() {
        AnvilDrop drop = new AnvilDrop().anvilConsumed(EmiStack.of(Items.ANVIL));
        assertTrue(drop.anvilConsumed);

        AnvilDrop kept = new AnvilDrop().anvil(EmiStack.of(Items.ANVIL));
        assertFalse(kept.anvilConsumed);
    }
}
