package com.berlord.forgeink;

import java.util.List;

/** Dependency-free policy shared by the Minecraft adapter and fast JVM tests. */
final class InkPolicy {

    static final int BASE_ESSENCE_PER_INK = 40;

    private static final List<String> INK_IDS = List.of(
            "irons_spellbooks:common_ink",
            "irons_spellbooks:uncommon_ink",
            "irons_spellbooks:rare_ink",
            "irons_spellbooks:epic_ink",
            "irons_spellbooks:legendary_ink"
    );

    private InkPolicy() {
    }

    static List<String> inkIds() {
        return INK_IDS;
    }

    static int inkTier(String itemId) {
        int index = INK_IDS.indexOf(itemId);
        return index < 0 ? -1 : index + 1;
    }

    static int essenceAmount(int tier) {
        if (tier < 1 || tier > INK_IDS.size()) {
            throw new IllegalArgumentException("Ink tier must be between 1 and 5: " + tier);
        }
        return BASE_ESSENCE_PER_INK << (tier - 1);
    }

    static boolean allowsInkTier(int inkTier, int forgeTier) {
        return inkTier >= 1 && inkTier <= INK_IDS.size() && inkTier == forgeTier;
    }
}
