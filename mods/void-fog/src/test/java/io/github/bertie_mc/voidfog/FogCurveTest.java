package io.github.bertie_mc.voidfog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class FogCurveTest {
    private static final int OVERWORLD_FLOOR = -64;
    private static final int FADE = 10;
    private static final int FULL = 5;

    /** Eye height above the block a player stands on. */
    private static final double EYE = 1.62;

    @Test
    void isAbsentAboveTheFadeDepth() {
        assertEquals(0.0F, FogCurve.strength(64.0, OVERWORLD_FLOOR, FADE, FULL));
        assertEquals(0.0F, FogCurve.strength(-54.0, OVERWORLD_FLOOR, FADE, FULL));
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

    /** Standing on the highest bedrock, y=-59, is the weakest the fog gets underfoot. */
    @Test
    void isPartialOnTheHighestBedrock() {
        float s = FogCurve.strength(-58.0 + EYE, OVERWORLD_FLOOR, FADE, FULL);
        assertTrue(s > 0.3F && s < 0.55F, "expected a moderate fog on the top bedrock, was " + s);
    }

    /**
     * The band eases in and out rather than ramping straight. A linear band has a corner at
     * each end, and on a five block band that corner is visible as you walk down into it.
     */
    @Test
    void theBandEasesAtBothEndsRatherThanRamping() {
        float justInside = FogCurve.strength(-64.0 + 9.0, OVERWORLD_FLOOR, FADE, FULL);
        float justAboveFull = FogCurve.strength(-64.0 + 6.0, OVERWORLD_FLOOR, FADE, FULL);
        assertTrue(justInside < 0.12F, "should barely start at the top of the band, was " + justInside);
        assertTrue(justAboveFull > 0.88F, "should be nearly full just above fullDepth, was " + justAboveFull);

        // Still monotonic, and still pinned at both ends.
        float previous = 1.1F;
        for (int tenths = 50; tenths <= 100; tenths++) {
            float here = FogCurve.strength(-64.0 + tenths / 10.0, OVERWORLD_FLOOR, FADE, FULL);
            assertTrue(here <= previous, "strength must not rise as you go up, at " + tenths / 10.0);
            previous = here;
        }
        assertEquals(1.0F, FogCurve.strength(-64.0 + FULL, OVERWORLD_FLOOR, FADE, FULL));
        assertEquals(0.0F, FogCurve.strength(-64.0 + FADE, OVERWORLD_FLOOR, FADE, FULL));
    }

    @Test
    void measuresDepthFromEachDimensionsOwnFloor() {
        assertEquals(1.0F, FogCurve.strength(5.0, 0, FADE, FULL));
        assertEquals(0.0F, FogCurve.strength(10.0, 0, FADE, FULL));
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

    /**
     * The reference is a fixed clear distance, not the render distance. Anchoring to the
     * render distance is what left a tunnel readable at a third strength: a third of the way
     * from 192 blocks is still 79 blocks of clear view.
     */
    @Test
    void distanceRunsFromTheReferenceDownToTheThickest() {
        assertEquals(48.0F, FogCurve.distance(48.0F, 12.0F, 0.0F), 1.0e-3F);
        assertEquals(12.0F, FogCurve.distance(48.0F, 12.0F, 1.0F), 1.0e-3F);
        assertEquals(24.0F, FogCurve.distance(48.0F, 12.0F, 0.5F), 1.0e-2F);
    }

    /** The two readings that have to match: on the top bedrock, and on the lowest floor. */
    @Test
    void distanceAtTheDepthsAPlayerActuallyReaches() {
        float top = FogCurve.distance(48.0F, 12.0F, FogCurve.strength(-58.0 + EYE, OVERWORLD_FLOOR, FADE, FULL));
        float bottom = FogCurve.distance(48.0F, 12.0F, FogCurve.strength(-63.0 + EYE, OVERWORLD_FLOOR, FADE, FULL));
        assertTrue(top > 20.0F && top < 32.0F, "top bedrock should still show a room, was " + top);
        assertTrue(bottom <= 14.0F, "the lowest floor should be near pitch black, was " + bottom);
    }

    @Test
    void distanceFallsBackToLerpWhenAnEndIsZero() {
        assertEquals(8.0F, FogCurve.distance(16.0F, 0.0F, 0.5F), 1.0e-4F);
    }

    /**
     * The reason a lit tunnel stayed readable: linear colour meant the ordinary bedrock floor,
     * around half strength, still kept half the world's colour, so distance faded to grey
     * rather than to black and every torch down the tunnel stayed visible.
     */
    @Test
    void colourGoesToBlackAcrossTheBandNotOnlyAtTheBottom() {
        assertEquals(1.0F, FogCurve.colourKept(0.0F, 1.0F));
        assertEquals(0.0F, FogCurve.colourKept(1.0F, 1.0F), 1.0e-6F);

        float onBedrock = FogCurve.colourKept(0.48F, 1.0F);
        assertTrue(onBedrock < 0.3F, "half strength should be mostly black, was " + onBedrock);
        assertTrue(onBedrock > 0.0F, "and not fully black yet, was " + onBedrock);
    }

    @Test
    void colourIsUntouchedWhenDarknessIsZero() {
        assertEquals(1.0F, FogCurve.colourKept(1.0F, 0.0F), 1.0e-6F);
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
