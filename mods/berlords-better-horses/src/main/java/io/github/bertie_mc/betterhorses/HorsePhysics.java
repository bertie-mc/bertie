package io.github.bertie_mc.betterhorses;

public final class HorsePhysics {
    private HorsePhysics() {}
    // Vanilla full-forward, steady-state riding speed on ordinary dry ground.
    public static final double BLOCKS_PER_SECOND_PER_ATTRIBUTE = 20.0 * 0.98 / (1.0 - 0.6 * 0.91);

    public static double apex(double velocity, double gravity) {
        double height = 0;
        for (int tick = 0; tick < 10000 && velocity > 0; tick++) {
            height += velocity;
            velocity = (velocity - gravity) * 0.98;
        }
        return height;
    }

    public static double boostedJump(double velocity, double extraHeight, double gravity) {
        if (extraHeight <= 0 || gravity <= 0) return velocity;
        double target = apex(velocity, gravity) + extraHeight;
        double low = velocity, high = velocity + extraHeight + 1;
        for (int i = 0; i < 48; i++) {
            double mid = (low + high) / 2;
            if (apex(mid, gravity) < target) low = mid;
            else high = mid;
        }
        return (low + high) / 2;
    }
}
