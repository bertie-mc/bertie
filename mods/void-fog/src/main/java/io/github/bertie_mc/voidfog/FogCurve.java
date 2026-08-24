package io.github.bertie_mc.voidfog;

/**
 * The maths behind the fog, kept free of Minecraft so how deep is "deep" can be checked without a
 * client.
 *
 * <p>Depth is measured from the dimension's own floor rather than from y=0, so one pair of settings
 * behaves the same in the overworld (floor -64) and in a dimension whose floor is 0.
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

    /** Straight interpolation, for values that may legitimately be zero. */
    public static float lerp(float from, float to, float strength) {
        return from + (to - from) * strength;
    }

    /**
     * Interpolates a view distance the way it is perceived: by ratio, not by amount.
     *
     * <p>Halfway between a 192-block view and a 16-block one is not 104 blocks - that still looks
     * completely clear. Geometrically it is 55, which is the wall of fog a player expects at half
     * strength. This is the difference between the fog appearing over the last two blocks of descent
     * and appearing across the whole band.
     */
    public static float approach(float from, float to, float strength) {
        if (from <= 0.0F || to <= 0.0F) {
            return lerp(from, to, strength);
        }
        return (float) (from * Math.pow(to / (double) from, strength));
    }

    /**
     * How much the fog survives being near a column that is open to the sky.
     *
     * <p>Zero when a sky-lit column is at hand and one once the nearest is {@code radius} away or
     * there is none at all, easing in so that stepping one block sideways under an opening does not
     * flip the fog on. {@code nearest} is a horizontal distance in blocks; a negative value means
     * nothing open was found.
     */
    public static float skyFalloff(double nearest, int radius) {
        if (nearest < 0.0 || radius <= 0) {
            return 1.0F;
        }
        if (nearest >= radius) {
            return 1.0F;
        }
        double t = nearest / radius;
        return (float) (t * t);
    }

    /** Moves {@code current} towards {@code target} by at most {@code step}. */
    public static float ease(float current, float target, float step) {
        if (current < target) {
            return Math.min(target, current + step);
        }
        return Math.max(target, current - step);
    }
}
