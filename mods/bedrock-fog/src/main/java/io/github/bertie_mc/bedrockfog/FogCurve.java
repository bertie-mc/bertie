package io.github.bertie_mc.bedrockfog;

/**
 * The depth-to-strength curve behind the fog. Dependency-free, so how deep is "deep" can be
 * checked without a client.
 *
 * <p>Depth is measured from the dimension's own floor rather than from y=0, so one pair of
 * settings behaves the same in the overworld (floor -64) and the nether (floor 0).
 */
public final class FogCurve {
    private FogCurve() {}

    /**
     * Fog strength, 0 to 1, for a camera at {@code y} in a dimension whose floor is {@code floorY}.
     *
     * <p>Zero at or above {@code fadeDepth} blocks over the floor, one at or below {@code fullDepth},
     * linear between. The order of the two early returns also covers a config where {@code fadeDepth}
     * is not above {@code fullDepth}: that band is a hard switch, not a division by zero.
     */
    public static float strength(double y, int floorY, int fadeDepth, int fullDepth) {
        double depth = y - floorY;
        if (depth <= fullDepth) {
            return 1.0F;
        }
        if (depth >= fadeDepth) {
            return 0.0F;
        }
        return (float) ((fadeDepth - depth) / (fadeDepth - (double) fullDepth));
    }

    /** Interpolates between the vanilla value and the fogged one. */
    public static float lerp(float from, float to, float strength) {
        return from + (to - from) * strength;
    }
}
