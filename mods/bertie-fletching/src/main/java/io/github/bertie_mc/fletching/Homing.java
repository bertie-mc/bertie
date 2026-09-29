package io.github.bertie_mc.fletching;

import net.minecraft.world.phys.Vec3;

public final class Homing {
    public static final double CONE_COS = Math.cos(Math.toRadians(30));
    public static final double TURN_PER_BLOCK = Math.toRadians(3);

    private Homing() {}

    public static boolean inCone(Vec3 originalDirection, Vec3 toward) {
        return toward.lengthSqr() > 1e-9 && originalDirection.dot(toward.normalize()) >= CONE_COS;
    }

    public static Vec3 turn(Vec3 velocity, Vec3 aim, boolean gravity) {
        double speed = velocity.length();
        if (speed < 1e-6 || aim.lengthSqr() < 1e-9) return velocity;
        Vec3 from = velocity.scale(1 / speed), to = aim.normalize();
        double angle = Math.acos(Math.clamp(from.dot(to), -1, 1));
        if (angle < 1e-6) return velocity;
        double step = Math.min(angle, TURN_PER_BLOCK * speed);
        Vec3 tangent = to.subtract(from.scale(from.dot(to))).normalize();
        Vec3 result =
                from.scale(Math.cos(step)).add(tangent.scale(Math.sin(step))).scale(speed);
        // Guidance can redirect momentum, but cannot replenish upward ballistic velocity.
        if (gravity && result.y > velocity.y) {
            double horizontal = Math.sqrt(Math.max(0, speed * speed - velocity.y * velocity.y));
            result = result.multiply(1, 0, 1).normalize().scale(horizontal).add(0, velocity.y, 0);
        }
        return result;
    }
}
