package io.github.bertie_mc.bertieprogression.combat;

public enum DamageFamily {
    PHYSICAL(true, true),
    MAGIC(true, true),
    ENERGY(false, true),
    PURE(false, false);

    private final boolean armor;
    private final boolean protection;

    DamageFamily(boolean armor, boolean protection) {
        this.armor = armor;
        this.protection = protection;
    }

    public boolean usesArmor() {
        return armor;
    }

    public boolean usesProtection() {
        return protection;
    }
}
