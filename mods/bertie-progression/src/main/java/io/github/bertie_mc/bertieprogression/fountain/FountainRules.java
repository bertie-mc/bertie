package io.github.bertie_mc.bertieprogression.fountain;

import java.util.function.DoubleSupplier;

/** Conditional, first-success fountain rolls and linear booster weights. */
public final class FountainRules {
    public enum Result {
        CORE,
        VOID,
        ORE,
        STURDY
    }

    public static Result roll(double core, double voidChance, DoubleSupplier random) {
        if (core > 0 && random.getAsDouble() < Math.min(1, core)) return Result.CORE;
        if (voidChance > 0 && random.getAsDouble() < Math.min(1, voidChance)) return Result.VOID;
        return random.getAsDouble() < 0.5 ? Result.ORE : Result.STURDY;
    }

    public static double specialChance(double base, int blocks) {
        return Math.clamp(base + 0.03 * blocks, 0, 1);
    }

    public static double oreWeight(double base, int blocks) {
        return base * (1 + 0.75 * blocks);
    }

    private FountainRules() {}
}
