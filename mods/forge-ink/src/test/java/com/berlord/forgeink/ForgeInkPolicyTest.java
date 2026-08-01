package com.berlord.forgeink;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ForgeInkPolicyTest {

    @Test
    void mapsEachIronsSpellbooksInkToItsForgeTier() {
        String[] paths = {"common_ink", "uncommon_ink", "rare_ink", "epic_ink", "legendary_ink"};

        for (int i = 0; i < paths.length; i++) {
            assertEquals(i + 1, InkPolicy.inkTier("irons_spellbooks:" + paths[i]));
        }
    }

    @Test
    void rejectsItemsOutsideTheFiveExactInkIds() {
        assertEquals(-1, InkPolicy.inkTier("minecraft:experience_bottle"));
        assertEquals(-1, InkPolicy.inkTier("irons_spellbooks:common_ink_fragment"));
        assertEquals(-1, InkPolicy.inkTier("another_mod:common_ink"));
    }

    @Test
    void doublesEssenceForEveryTier() {
        assertEquals(40, InkPolicy.essenceAmount(1));
        assertEquals(80, InkPolicy.essenceAmount(2));
        assertEquals(160, InkPolicy.essenceAmount(3));
        assertEquals(320, InkPolicy.essenceAmount(4));
        assertEquals(640, InkPolicy.essenceAmount(5));
        assertThrows(IllegalArgumentException.class, () -> InkPolicy.essenceAmount(0));
        assertThrows(IllegalArgumentException.class, () -> InkPolicy.essenceAmount(6));
    }

    @Test
    void acceptsOnlyAValidInkMatchingTheForgeTier() {
        for (int forgeTier = 1; forgeTier <= 5; forgeTier++) {
            for (int inkTier = 1; inkTier <= 5; inkTier++) {
                assertEquals(inkTier == forgeTier, InkPolicy.allowsInkTier(inkTier, forgeTier));
            }
        }

        assertFalse(InkPolicy.allowsInkTier(-1, 1));
        assertFalse(InkPolicy.allowsInkTier(0, 0));
        assertFalse(InkPolicy.allowsInkTier(6, 6));
        assertTrue(InkPolicy.allowsInkTier(3, 3));
    }
}
