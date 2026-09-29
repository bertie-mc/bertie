package io.github.bertie_mc.bertieprogression.combat;

/** Damage and recovery calculations shared by the compatibility adapters. */
public final class CombatMath {
    private CombatMath() {}

    public static double armorPressure(double damage) {
        double excess = Math.max(damage - 20.0, 0.0);
        return 8.0 + 8.0 * (excess / (excess + 40.0));
    }

    public static double armorRemaining(double damage, double armor) {
        double pressure = armorPressure(damage);
        return pressure / (pressure + Math.max(armor, 0.0));
    }

    public static double protectionRemaining(double points) {
        return Math.pow(10.0, -Math.max(points, 0.0) / 40.0);
    }

    public static double toughnessCounter(double toughness) {
        return Math.clamp(toughness * 0.02, 0.0, 0.6);
    }

    public static double counterArmorPenalty(double penalty, double toughness) {
        return penalty * (1.0 - toughnessCounter(toughness));
    }

    /** Weakening subtracts percentage points after independent resistance sources combine. */
    public static double resistanceRemaining(double combinedRemaining, double weakening) {
        return Math.max(0.0, combinedRemaining + weakening);
    }

    public static double combineRemaining(double remaining, double resistance) {
        return remaining * Math.max(0.0, 1.0 - resistance);
    }

    public static double subtractDamage(double damage, double subtraction) {
        return Math.max(0.0, damage - Math.max(0.0, subtraction));
    }

    public static double eligibleLifeStealDamage(double healthDamage, double healthBefore) {
        return Math.max(0.0, Math.min(healthDamage, healthBefore));
    }

    public static double healingMultiplier(double increase, double reduction) {
        return Math.max(0.0, 1.0 + increase - reduction);
    }
}
