package io.github.bertie_mc.fletching;

import static org.junit.jupiter.api.Assertions.*;

import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

class HomingTest {
    @Test
    void forwardConeExcludesBehindAndWideAngles() {
        Vec3 forward = new Vec3(0, 0, 1);
        assertTrue(Homing.inCone(forward, new Vec3(.3, 0, 1)));
        assertFalse(Homing.inCone(forward, new Vec3(1, 0, 1)));
        assertFalse(Homing.inCone(forward, new Vec3(0, 0, -1)));
    }

    @Test
    void turningScalesWithDistanceAndDoesNotAddEnergy() {
        for (double speed : new double[] {.2, 1, 3, 6}) {
            Vec3 turned = Homing.turn(new Vec3(0, 0, speed), new Vec3(1, 0, 1), false);
            assertEquals(speed, turned.length(), 1e-9);
            assertEquals(Homing.TURN_PER_BLOCK * speed, Math.acos(turned.normalize().z), 1e-9);
        }
    }

    @Test
    void FallingArrowCannotClimbToTarget() {
        Vec3 falling = new Vec3(.5, -.7, 1);
        Vec3 turned = Homing.turn(falling, new Vec3(0, 10, 1), true);
        assertTrue(turned.y <= falling.y);
        assertEquals(falling.length(), turned.length(), 1e-9);
    }
}
