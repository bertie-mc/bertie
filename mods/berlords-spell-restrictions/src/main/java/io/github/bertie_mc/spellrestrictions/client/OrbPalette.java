package io.github.bertie_mc.spellrestrictions.client;

/** Colour transforms for the two existing orb layers. Pixels use NativeImage ABGR. */
public final class OrbPalette {
    public static final int[][] COLORS = {{226, 234, 238}, {76, 202, 84}, {42, 200, 209}, {193, 79, 216}, {246, 185, 45}
    };

    public static int pulse(int abgr, int rarity, double phase) {
        int[] color = COLORS[rarity];
        double amount = (1 - Math.cos(phase * 2 * Math.PI)) / 2 * .86;
        int r = abgr & 255, g = (abgr >>> 8) & 255, b = (abgr >>> 16) & 255;
        int brightness = Math.max(r, Math.max(g, b)), peak = Math.max(color[0], Math.max(color[1], color[2]));
        return (abgr & 0xff000000)
                | blend(r, clamp(brightness * 1.55 * color[0] / peak), amount)
                | blend(g, clamp(brightness * 1.55 * color[1] / peak), amount) << 8
                | blend(b, clamp(brightness * 1.55 * color[2] / peak), amount) << 16;
    }

    public static int orbit(int abgr, int rarity) {
        double shade = (abgr & 255) / 141.0;
        int[] c = COLORS[rarity];
        return (abgr & 0xff000000) | clamp(c[0] * shade) | clamp(c[1] * shade) << 8 | clamp(c[2] * shade) << 16;
    }

    private static int blend(int a, int b, double amount) {
        return (int) Math.round(a + (b - a) * amount);
    }

    private static int clamp(double value) {
        return Math.clamp((int) Math.round(value), 0, 255);
    }

    private OrbPalette() {}
}
