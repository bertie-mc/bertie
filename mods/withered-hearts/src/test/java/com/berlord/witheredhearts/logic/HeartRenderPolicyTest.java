package com.berlord.witheredhearts.logic;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HeartRenderPolicyTest {

    @Test
    void derivesRemainingDamageFromDurationAndAmplifier() {
        assertEquals(0, HeartRenderPolicy.remainingHalfHearts(39, 0));
        assertEquals(1, HeartRenderPolicy.remainingHalfHearts(40, 0));
        assertEquals(2, HeartRenderPolicy.remainingHalfHearts(80, 0));
        assertEquals(2, HeartRenderPolicy.remainingHalfHearts(40, 1));
        assertEquals(4, HeartRenderPolicy.remainingHalfHearts(40, 2));
        assertEquals(4, HeartRenderPolicy.remainingHalfHearts(40, 8));
        assertEquals(0, HeartRenderPolicy.remainingHalfHearts(-1, 0));
    }

    @Test
    void consumesFullAndHalfHeartsFromTheDarkSegment() {
        HeartRenderPolicy.Draw full = HeartRenderPolicy.nextDraw(false, 4, false);
        assertTrue(full.withered());
        assertEquals(2, full.remainingHalfHearts());

        HeartRenderPolicy.Draw half = HeartRenderPolicy.nextDraw(false, 2, true);
        assertTrue(half.withered());
        assertEquals(1, half.remainingHalfHearts());

        HeartRenderPolicy.Draw boundary = HeartRenderPolicy.nextDraw(false, 1, false);
        assertTrue(boundary.withered());
        assertEquals(0, boundary.remainingHalfHearts());
    }

    @Test
    void leavesContainersAndTheRemainderOfTheBarUntouched() {
        HeartRenderPolicy.Draw container = HeartRenderPolicy.nextDraw(true, 3, false);
        assertFalse(container.withered());
        assertEquals(3, container.remainingHalfHearts());

        HeartRenderPolicy.Draw exhausted = HeartRenderPolicy.nextDraw(false, 0, false);
        assertFalse(exhausted.withered());
        assertEquals(0, exhausted.remainingHalfHearts());
    }
}
