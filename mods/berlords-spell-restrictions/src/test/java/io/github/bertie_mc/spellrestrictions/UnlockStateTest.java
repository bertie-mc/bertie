package io.github.bertie_mc.spellrestrictions;

import static org.junit.jupiter.api.Assertions.*;

import io.github.bertie_mc.spellrestrictions.client.OrbPalette;
import java.util.Set;
import org.junit.jupiter.api.Test;

class UnlockStateTest {
    @Test
    void knowledgeAndRarityAreIndependent() {
        var state = new UnlockState(Set.of(), 0);
        assertFalse(state.permits("spell", 0, true, true));
        assertTrue(state.discover("spell"));
        assertTrue(state.permits("spell", 0, true, true));
        assertFalse(state.permits("spell", 4, true, true));
        state.unlockTier(4);
        assertTrue(state.permits("spell", 4, true, true));
        assertFalse(state.permits("another_spell", 0, true, true));
    }

    @Test
    void orbsUnlockAllLowerTiersAndNeverDowngrade() {
        var state = new UnlockState(Set.of("spell"), -1);
        assertFalse(state.permits("spell", 0, true, true));
        assertTrue(state.unlockTier(3));
        for (int i = 0; i <= 3; i++) assertTrue(state.permits("spell", i, true, true));
        assertFalse(state.unlockTier(2));
        assertFalse(state.unlockTier(3));
        assertFalse(state.unlockTier(5));
        assertEquals(3, state.tier());
    }

    @Test
    void discoveryIsIdempotentAndPermanentAfterItemRemoval() {
        var state = new UnlockState(Set.of(), 0);
        assertTrue(state.discover("spell"));
        assertFalse(state.discover("spell"));
        assertTrue(state.knows("spell"));
        assertThrows(UnsupportedOperationException.class, () -> state.spells().clear());
    }

    @Test
    void gateConfigurationCanDisableEachIndependently() {
        var state = new UnlockState(Set.of(), -1);
        assertTrue(state.permits("spell", 4, false, false));
        assertFalse(state.permits("spell", 4, false, true));
        state.discover("spell");
        assertTrue(state.permits("spell", 4, true, false));
    }

    @Test
    void orbPulseKeepsNeutralPixelsAndAlpha() {
        for (int rarity = 0; rarity < 5; rarity++)
            for (int alpha : new int[] {0, 64, 255}) {
                int original = (alpha << 24) | 0x907270;
                assertEquals(original, OrbPalette.pulse(original, rarity, 0));
                assertEquals(alpha, OrbPalette.pulse(original, rarity, .5) >>> 24);
                assertEquals(alpha, OrbPalette.orbit(original, rarity) >>> 24);
                assertEquals(OrbPalette.pulse(original, rarity, .25), OrbPalette.pulse(original, rarity, .75));
            }
    }
}
