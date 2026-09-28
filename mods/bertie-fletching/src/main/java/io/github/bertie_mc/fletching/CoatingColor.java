package io.github.bertie_mc.fletching;

import java.util.OptionalInt;

/** Bridges the upstream potion-name rendering field without creating invalid resource paths. */
public final class CoatingColor {
    private static final String PREFIX = "bertie_rgb_";

    private CoatingColor() {}

    public static String encode(int color) {
        return PREFIX + Integer.toHexString((color & 0xffffff) | 0x1000000).substring(1);
    }

    public static boolean isEncoded(String name) {
        return name != null && (name.startsWith(PREFIX) || name.startsWith("#"));
    }

    public static OptionalInt decode(String name) {
        if (!isEncoded(name)) return OptionalInt.empty();
        String hex = name.substring(name.startsWith("#") ? 1 : PREFIX.length());
        if (hex.isEmpty() || hex.length() > 8) return OptionalInt.empty();
        try {
            return OptionalInt.of(Integer.parseUnsignedInt(hex, 16) & 0xffffff);
        } catch (NumberFormatException ignored) {
            return OptionalInt.empty();
        }
    }
}
