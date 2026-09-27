package io.github.bertie_mc.spellrestrictions;

import java.util.HashSet;
import java.util.Set;

/** Permanent discoveries and an independently earned output rarity ceiling. */
public final class UnlockState {
    private final Set<String> spells;
    private int tier;

    public UnlockState(Set<String> spells, int tier) {
        this.spells = new HashSet<>(spells);
        this.tier = Math.clamp(tier, -1, 4);
    }

    public Set<String> spells() {
        return Set.copyOf(spells);
    }

    public int tier() {
        return tier;
    }

    public boolean knows(String spell) {
        return spells.contains(spell);
    }

    public boolean discover(String spell) {
        return spells.add(spell);
    }

    public boolean unlockTier(int rank) {
        if (rank < -1 || rank > 4 || rank <= tier) return false;
        tier = rank;
        return true;
    }

    public boolean permits(String spell, int rarity, boolean discoveryGate, boolean rarityGate) {
        return (!discoveryGate || knows(spell)) && (!rarityGate || rarity <= tier);
    }
}
