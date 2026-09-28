package io.github.bertie_mc.emi.integration.anvilcraft;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
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
                .onto(null)
                .fluidIn(EmiStack.EMPTY)
                .out(EmiStack.EMPTY)
                .note(null);

        assertEquals(1, drop.payload.size());
        assertTrue(drop.decks.isEmpty());
        assertEquals(0, drop.outputCells());
        assertTrue(drop.info.isEmpty());
        // Only the payload hangs below the anvil, so the scene is one row deep.
        assertEquals(1, drop.rows());
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
    void cauldronSceneIsThreeRowsWithATankBesideTheCauldron() {
        AnvilDrop drop = new AnvilDrop()
                .item(EmiStack.of(Items.COPPER_INGOT))
                .on(EmiStack.of(Items.CAULDRON))
                .fluidIn(EmiStack.of(Fluids.WATER, 1_000))
                .on(EmiStack.of(Items.CAMPFIRE))
                .out(EmiStack.of(Items.BRICK))
                .note(Component.literal("The Campfire has to be lit"));

        assertEquals(3, drop.rows());
        assertEquals(2, drop.widestDeck());
        assertEquals(1, drop.outputCells());
        // The fluid belongs to the cauldron it was added after, not to the Campfire below it.
        assertNotNull(drop.decks.get(0).fluid());
        assertNull(drop.decks.get(1).fluid());
        assertTrue(drop.decks.get(0).survives());
    }

    /**
     * Block Smear and Block Compress both press two stacked blocks, and differ only in whether the
     * upper one is still standing afterwards. The decks keep that order and that distinction.
     */
    @Test
    void stackedBlocksKeepTheirOrderAndTheirFate() {
        AnvilDrop smear = new AnvilDrop()
                .on(EmiStack.of(Items.MOSS_BLOCK))
                .onto(EmiStack.of(Items.DIRT))
                .out(EmiStack.of(Items.GRASS_BLOCK));

        assertEquals(2, smear.rows());
        assertFalse(smear.hasPayload());
        assertTrue(smear.decks.get(0).survives());
        assertFalse(smear.decks.get(1).survives());

        AnvilDrop compress = new AnvilDrop()
                .onto(EmiStack.of(Items.DIRT))
                .onto(EmiStack.of(Items.STONE))
                .out(EmiStack.of(Items.COBBLESTONE));

        assertFalse(compress.decks.get(0).survives());
        assertFalse(compress.decks.get(1).survives());
    }

    @Test
    void aConsumedAnvilIsRecordedAsWellAsDrawn() {
        AnvilDrop drop = new AnvilDrop().anvilConsumed(EmiStack.of(Items.ANVIL));
        assertTrue(drop.anvilConsumed);

        AnvilDrop kept = new AnvilDrop().anvil(EmiStack.of(Items.ANVIL));
        assertFalse(kept.anvilConsumed);
    }
}
