package com.berlord.foodsystem.compat;

/** Marks eats initiated by a Sophisticated Backpacks feeding upgrade (no slot-replacing). */
public final class FeedingContext {
    private static final ThreadLocal<Boolean> ACTIVE = ThreadLocal.withInitial(() -> false);

    public static void begin() {
        ACTIVE.set(true);
    }

    public static void end() {
        ACTIVE.set(false);
    }

    public static boolean isActive() {
        return ACTIVE.get();
    }

    private FeedingContext() {}
}
