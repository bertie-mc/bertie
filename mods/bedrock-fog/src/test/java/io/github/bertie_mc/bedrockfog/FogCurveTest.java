package io.github.bertie_mc.bedrockfog;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class FogCurveTest {
    private static final int OVERWORLD_FLOOR = -64;
    private static final int FADE = 32;
    private static final int FULL = 6;

    @Test
    void isAbsentAboveTheFadeDepth() {
        assertEquals(0.0F, FogCurve.strength(64.0, OVERWORLD_FLOOR, FADE, FULL));
        assertEquals(0.0F, FogCurve.strength(-32.0, OVERWORLD_FLOOR, FADE, FULL));
    }

    @Test
    void isFullAtBedrockLevel() {
        assertEquals(1.0F, FogCurve.strength(-58.0, OVERWORLD_FLOOR, FADE, FULL));
        assertEquals(1.0F, FogCurve.strength(-64.0, OVERWORLD_FLOOR, FADE, FULL));
    }

    @Test
    void ramps() {
        // Midpoint of the -32..-58 band.
        assertEquals(0.5F, FogCurve.strength(-45.0, OVERWORLD_FLOOR, FADE, FULL), 1.0e-6F);
    }

    @Test
    void measuresDepthFromEachDimensionsOwnFloor() {
        // The nether floor is 0, so the same settings put full fog at y=6 there.
        assertEquals(1.0F, FogCurve.strength(6.0, 0, FADE, FULL));
        assertEquals(0.0F, FogCurve.strength(32.0, 0, FADE, FULL));
    }

    @Test
    void collapsesToAHardSwitchWhenTheBandIsInverted() {
        assertEquals(1.0F, FogCurve.strength(-58.0, OVERWORLD_FLOOR, FULL, FADE));
        assertEquals(0.0F, FogCurve.strength(-20.0, OVERWORLD_FLOOR, FULL, FADE));
    }

    @Test
    void lerpsTowardsTheFoggedValue() {
        assertEquals(192.0F, FogCurve.lerp(192.0F, 16.0F, 0.0F));
        assertEquals(16.0F, FogCurve.lerp(192.0F, 16.0F, 1.0F));
        assertEquals(104.0F, FogCurve.lerp(192.0F, 16.0F, 0.5F));
    }
}
