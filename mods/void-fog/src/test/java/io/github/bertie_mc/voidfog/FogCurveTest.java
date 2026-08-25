package io.github.bertie_mc.voidfog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class FogCurveTest {
    private static final int OVERWORLD_FLOOR = -64;
    private static final int FADE = 19;
    private static final int FULL = 5;

    /** Eye height above the block a player stands on. */
    private static final double EYE = 1.62;

    @Test
    void isAbsentAboveTheFadeDepth() {
        assertEquals(0.0F, FogCurve.strength(64.0, OVERWORLD_FLOOR, FADE, FULL));
        assertEquals(0.0F, FogCurve.strength(-45.0, OVERWORLD_FLOOR, FADE, FULL));
    }

    /**
     * Full strength has to be reachable by a PLAYER, not by a point in the rock. The lowest
     * floor anyone stands on is the y=-64 bedrock layer, which puts an eye at -62.38. A
     * fullDepth of 0 asks for an eye at y=-64 and can never be met, which is why the fog
     * topped out around a third and a tunnel stayed readable eighty blocks down.
     */
    @Test
    void isFullWhereTheLowestFloorPutsAnEye() {
        assertEquals(1.0F, FogCurve.strength(-63.0 + EYE, OVERWORLD_FLOOR, FADE, FULL));
    }

    /**
     * Standing on the highest bedrock is the weakest the fog gets underfoot, and with the band
     * reaching up to y=-45 that is no longer "moderate" - by the time you are on bedrock at
     * all you are nearly at full strength.
     */
    @Test
    void isNearlyFullOnTheHighestBedrock() {
        float s = FogCurve.strength(-58.0 + EYE, OVERWORLD_FLOOR, FADE, FULL);
        assertTrue(s > 0.85F, "expected near-full fog on the top bedrock, was " + s);
    }

    /**
     * The band eases in and out rather than ramping straight. A linear band has a corner at
     * each end, and on a five block band that corner is visible as you walk down into it.
     */
    @Test
    void theBandEasesAtBothEndsRatherThanRamping() {
        float justInside = FogCurve.strength(-64.0 + (FADE - 1), OVERWORLD_FLOOR, FADE, FULL);
        float justAboveFull = FogCurve.strength(-64.0 + 6.0, OVERWORLD_FLOOR, FADE, FULL);
        assertTrue(justInside < 0.12F, "should barely start at the top of the band, was " + justInside);
        assertTrue(justAboveFull > 0.88F, "should be nearly full just above fullDepth, was " + justAboveFull);

        // Still monotonic, and still pinned at both ends.
        float previous = 1.1F;
        for (int tenths = FULL * 10; tenths <= FADE * 10; tenths++) {
            float here = FogCurve.strength(-64.0 + tenths / 10.0, OVERWORLD_FLOOR, FADE, FULL);
            assertTrue(here <= previous, "strength must not rise as you go up, at " + tenths / 10.0);
            previous = here;
        }
        assertEquals(1.0F, FogCurve.strength(-64.0 + FULL, OVERWORLD_FLOOR, FADE, FULL));
        assertEquals(0.0F, FogCurve.strength(-64.0 + FADE, OVERWORLD_FLOOR, FADE, FULL));
    }

    @Test
    void measuresDepthFromEachDimensionsOwnFloor() {
        assertEquals(1.0F, FogCurve.strength(FULL, 0, FADE, FULL));
        assertEquals(0.0F, FogCurve.strength(FADE, 0, FADE, FULL));
    }

    @Test
    void collapsesToAHardSwitchWhenTheBandIsInverted() {
        assertEquals(1.0F, FogCurve.strength(-60.0, OVERWORLD_FLOOR, FULL, FADE));
        assertEquals(0.0F, FogCurve.strength(-20.0, OVERWORLD_FLOOR, FULL, FADE));
    }

    @Test
    void lerpsTowardsTheFoggedValue() {
        assertEquals(192.0F, FogCurve.lerp(192.0F, 16.0F, 0.0F));
        assertEquals(16.0F, FogCurve.lerp(192.0F, 16.0F, 1.0F));
        assertEquals(104.0F, FogCurve.lerp(192.0F, 16.0F, 0.5F));
    }

    private static final double FALLOFF = 3.0;

    /**
     * The fault this curve exists to fix. The far plane used to interpolate from a FIXED
     * reference, so the moment strength rose above zero it snapped from the render distance
     * to that reference - 192 blocks to 28 within a tenth of a block of descent. Starting
     * from the far plane the game was about to use leaves nothing to step over.
     */
    @Test
    void theViewDistanceIsContinuousWhereTheBandBegins() {
        float renderFar = 192.0F;
        float above = FogCurve.strength(-45.0, OVERWORLD_FLOOR, FADE, FULL);
        assertEquals(0.0F, above, "the band should not have started at y=-45");

        float justInside = FogCurve.strength(-45.2, OVERWORLD_FLOOR, FADE, FULL);
        float far = FogCurve.distance(renderFar, 8.0F, justInside, FALLOFF);
        assertTrue(
                far > renderFar * 0.93F,
                "entering the band must barely change the view, was " + far + " of " + renderFar);
    }

    @Test
    void theViewDistanceClosesDownToTheThickestAtFullStrength() {
        assertEquals(192.0F, FogCurve.distance(192.0F, 8.0F, 0.0F, FALLOFF), 1.0e-3F);
        assertEquals(8.0F, FogCurve.distance(192.0F, 8.0F, 1.0F, FALLOFF), 1.0e-3F);
    }

    /** Deep down the answer is the same whatever the render distance is set to. */
    @Test
    void theDeepEndDoesNotDependOnRenderDistance() {
        float deep = FogCurve.strength(-58.0, OVERWORLD_FLOOR, FADE, FULL);
        float wide = FogCurve.distance(192.0F, 8.0F, deep, FALLOFF);
        float narrow = FogCurve.distance(128.0F, 8.0F, deep, FALLOFF);
        assertTrue(Math.abs(wide - narrow) < 1.0F, "expected agreement, got " + wide + " and " + narrow);
        assertTrue(wide < 10.0F, "should be nearly solid down there, was " + wide);
    }

    @Test
    void distanceFallsBackToLerpWhenAnEndIsZero() {
        assertEquals(0.0F, FogCurve.distance(16.0F, 0.0F, 1.0F, FALLOFF), 1.0e-4F);
    }

    /**
     * Colour rides the same ramp as the distance, so the two arrive together. It used to be
     * its own power curve, which left zero steeply and darkened the view by a sixth the
     * instant the band was entered.
     */
    @Test
    void colourRidesTheSameRampAsTheDistance() {
        assertEquals(1.0F, FogCurve.colourKept(0.0F, 1.0F, FALLOFF), 1.0e-6F);
        assertEquals(0.0F, FogCurve.colourKept(1.0F, 1.0F, FALLOFF), 1.0e-6F);

        float justInside = FogCurve.strength(-45.2, OVERWORLD_FLOOR, FADE, FULL);
        float keep = FogCurve.colourKept(justInside, 1.0F, FALLOFF);
        assertTrue(keep > 0.97F, "entering the band must barely darken it, was " + keep);
    }

    @Test
    void colourIsUntouchedWhenDarknessIsZero() {
        assertEquals(1.0F, FogCurve.colourKept(1.0F, 0.0F, FALLOFF), 1.0e-6F);
    }

    @Test
    void theRampIsPinnedAtBothEndsAndRisesThroughout() {
        assertEquals(0.0F, FogCurve.ramp(0.0F, FALLOFF));
        assertEquals(1.0F, FogCurve.ramp(1.0F, FALLOFF));
        float previous = -1.0F;
        for (int step = 0; step <= 100; step++) {
            float here = FogCurve.ramp(step / 100.0F, FALLOFF);
            assertTrue(here >= previous, "ramp must not fall, at " + step);
            previous = here;
        }
    }

    @Test
    void skyFalloffClearsTheFogUnderAnOpeningAndEasesBackIn() {
        assertEquals(0.0F, FogCurve.skyFalloff(0.0, 24), 1.0e-6F);
        assertEquals(1.0F, FogCurve.skyFalloff(24.0, 24));
        assertEquals(1.0F, FogCurve.skyFalloff(999.0, 24));
        // Nothing open in range at all.
        assertEquals(1.0F, FogCurve.skyFalloff(-1.0, 24));
        // Quadratic, so the first steps away from the opening barely bring it back.
        assertEquals(0.25F, FogCurve.skyFalloff(12.0, 24), 1.0e-6F);
        assertTrue(FogCurve.skyFalloff(6.0, 24) < 0.1F);
    }

    @Test
    void skyFalloffIsInertWhenTheCheckIsOff() {
        assertEquals(1.0F, FogCurve.skyFalloff(0.0, 0));
    }

    @Test
    void easeMovesAtMostOneStepAndDoesNotOvershoot() {
        assertEquals(0.5F, FogCurve.ease(0.4F, 1.0F, 0.1F), 1.0e-6F);
        assertEquals(0.3F, FogCurve.ease(0.4F, 0.0F, 0.1F), 1.0e-6F);
        assertEquals(1.0F, FogCurve.ease(0.95F, 1.0F, 0.1F), 1.0e-6F);
        assertEquals(0.0F, FogCurve.ease(0.05F, 0.0F, 0.1F), 1.0e-6F);
    }
}
