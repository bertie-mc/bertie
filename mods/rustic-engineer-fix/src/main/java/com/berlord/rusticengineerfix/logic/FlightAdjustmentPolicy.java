package com.berlord.rusticengineerfix.logic;

/** Dependency-free decisions shared by the injected adapters and JVM tests. */
public final class FlightAdjustmentPolicy {

    private FlightAdjustmentPolicy() {
    }

    public static boolean yawChanged(float previousYaw, float currentYaw) {
        return previousYaw != currentYaw;
    }

    public static boolean shouldApplyVertical(boolean mounted, double moveY) {
        return mounted && moveY != 0.0D;
    }
}
