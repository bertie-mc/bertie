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
     * The distance you can see at a given fog strength, in blocks.
     *
     * <p>Geometric between {@code clear} and {@code thickest}, because a view distance is
     * perceived by ratio and not by amount: halfway between 48 blocks and 12 is not 30, it is
     * 24.
     *
     * <p>{@code clear} is a fixed reference, NOT the render distance, and that is the whole
     * point. Interpolating from the render distance meant a player on the bedrock - where the
     * strength works out at about a third - still saw eighty blocks down a tunnel, because a
     * third of the way from 192 is nowhere near fogged. Anchoring to a reference makes the
     * strength mean the same thing whatever the render distance is set to.
     */
    public static float distance(float clear, float thickest, float strength) {
        if (clear <= 0.0F || thickest <= 0.0F) {
            return lerp(clear, thickest, strength);
        }
        return (float) (clear * Math.pow(thickest / (double) clear, strength));
    }

    /**
     * How much of the world's own colour survives at a given fog strength.
     *
     * <p>Deliberately NOT linear. Linear meant that standing on the ordinary bedrock floor -
     * which works out around a third of full strength - kept two thirds of the original fog
     * colour, so distance faded to grey and a torch-lit tunnel stayed perfectly readable
     * thirty blocks down. Raising the strength to a power below one drives the colour to
     * black across the top of the band instead of only at the very bottom, which is what
     * makes light stop rescuing you.
     */
    public static float colourKept(float strength, float darkness) {
        if (strength <= 0.0F) {
            return 1.0F;
        }
        return 1.0F - darkness * (float) Math.pow(Math.min(1.0F, strength), COLOUR_FALLOFF);
    }

    /** Lower drives the fog to black sooner in the band. */
    private static final double COLOUR_FALLOFF = 0.4;

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
