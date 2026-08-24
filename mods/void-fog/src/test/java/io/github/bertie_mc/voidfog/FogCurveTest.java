package io.github.bertie_mc.voidfog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class FogCurveTest {
    private static final int OVERWORLD_FLOOR = -64;
    private static final int FADE = 10;
    private static final int FULL = 0;

    @Test
    void isAbsentAboveTheFadeDepth() {
        assertEquals(0.0F, FogCurve.strength(64.0, OVERWORLD_FLOOR, FADE, FULL));
        assertEquals(0.0F, FogCurve.strength(-54.0, OVERWORLD_FLOOR, FADE, FULL));
    }

    @Test
    void isFullOnTheFloor() {
        assertEquals(1.0F, FogCurve.strength(-64.0, OVERWORLD_FLOOR, FADE, FULL));
    }

    /** The highest bedrock sits at y=-59, halfway down the band. */
    @Test
    void isHalfwayAtTheTopOfTheBedrockLayer() {
        assertEquals(0.5F, FogCurve.strength(-59.0, OVERWORLD_FLOOR, FADE, FULL), 1.0e-6F);
    }

    @Test
    void measuresDepthFromEachDimensionsOwnFloor() {
        assertEquals(1.0F, FogCurve.strength(0.0, 0, FADE, FULL));
        assertEquals(0.0F, FogCurve.strength(10.0, 0, FADE, FULL));
    }

    @Test
    void collapsesToAHardSwitchWhenTheBandIsInverted() {
        assertEquals(1.0F, FogCurve.strength(-64.0, OVERWORLD_FLOOR, FULL, FADE));
        assertEquals(0.0F, FogCurve.strength(-20.0, OVERWORLD_FLOOR, FULL, FADE));
    }

    @Test
    void lerpsTowardsTheFoggedValue() {
        assertEquals(192.0F, FogCurve.lerp(192.0F, 16.0F, 0.0F));
        assertEquals(16.0F, FogCurve.lerp(192.0F, 16.0F, 1.0F));
        assertEquals(104.0F, FogCurve.lerp(192.0F, 16.0F, 0.5F));
    }

    /**
     * The point of approach() over lerp(): half strength has to look half fogged. Straight
     * interpolation leaves 104 blocks of clear view, which reads as no fog at all.
     */
    @Test
    void approachClosesTheViewInByRatio() {
        assertEquals(192.0F, FogCurve.approach(192.0F, 16.0F, 0.0F), 1.0e-3F);
        assertEquals(16.0F, FogCurve.approach(192.0F, 16.0F, 1.0F), 1.0e-3F);

        float half = FogCurve.approach(192.0F, 16.0F, 0.5F);
        assertEquals(Math.sqrt(192.0 * 16.0), half, 1.0e-2F);
        assertTrue(half < 60.0F, "half strength should already be a wall of fog, was " + half);
    }

    @Test
    void approachFallsBackToLerpWhenAnEndIsZero() {
        assertEquals(8.0F, FogCurve.approach(16.0F, 0.0F, 0.5F), 1.0e-4F);
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
