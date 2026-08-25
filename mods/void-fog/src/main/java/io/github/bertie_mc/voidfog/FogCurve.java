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
        double t = (fadeDepth - depth) / (fadeDepth - (double) fullDepth);
        // Smoothstep, not a straight ramp. A linear band has a corner at each end - the fog starts
        // and stops changing abruptly - and on a five block band that corner is visible as you walk
        // down into it. This flattens the curve where it meets both ends.
        return (float) (t * t * (3.0 - 2.0 * t));
    }

    /** Straight interpolation, for values that may legitimately be zero. */
    public static float lerp(float from, float to, float strength) {
        return from + (to - from) * strength;
    }

    /**
     * The shared 0-to-1 shaping that everything the fog does is driven by.
     *
     * <p>One curve for the view distance and the colour both, so they arrive together rather
     * than on separate schedules. It leaves zero with a finite slope, which is the whole
     * point: a curve that leaves zero steeply - a power below one, say - is continuous on
     * paper and still reads as a step on screen.
     */
    public static float ramp(float strength, double falloff) {
        if (strength <= 0.0F) {
            return 0.0F;
        }
        if (strength >= 1.0F) {
            return 1.0F;
        }
        return (float) (1.0 - Math.pow(1.0 - strength, falloff));
    }

    /**
     * The distance you can see, interpolated from {@code clear} down to {@code thickest}.
     *
     * <p>Geometric, because a view distance is perceived by ratio and not by amount: halfway
     * between 192 blocks and 8 is not 100, it is 39.
     *
     * <p>{@code clear} is the far plane the game was ABOUT to use, and passing anything else
     * is what put a cut in the effect. A fixed reference meant the far plane jumped from the
     * render distance to that reference the moment strength rose above zero - 192 blocks to
     * 28 within a tenth of a block of descent, which is exactly the edge you could see. Start
     * from where the view already was and there is nothing to step over.
     */
    public static float distance(float clear, float thickest, float strength, double falloff) {
        if (clear <= 0.0F || thickest <= 0.0F) {
            return lerp(clear, thickest, ramp(strength, falloff));
        }
        return (float) (clear * Math.pow(thickest / (double) clear, ramp(strength, falloff)));
    }

    /**
     * How much of the world's own colour survives at a given fog strength.
     *
     * <p>Driven by the same ramp as the distance, so the world darkens at the rate it closes
     * in. This used to be its own power curve, which left zero steeply and darkened the view
     * by a sixth the instant you crossed into the band.
     */
    public static float colourKept(float strength, float darkness, double falloff) {
        return 1.0F - darkness * ramp(strength, falloff);
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
