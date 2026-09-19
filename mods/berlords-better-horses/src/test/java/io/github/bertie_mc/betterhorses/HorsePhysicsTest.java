package io.github.bertie_mc.betterhorses;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class HorsePhysicsTest {
    @Test
    void jumpBonusAddsBlocksAcrossHorseStrengths() {
        for (double power : new double[] {0.4, 0.55, 0.7, 1.0})
            for (int height = 1; height <= 3; height++) {
                double result = HorsePhysics.boostedJump(power, height, 0.08);
                assertEquals(HorsePhysics.apex(power, 0.08) + height, HorsePhysics.apex(result, 0.08), 1e-8);
            }
    }

    @Test
    void noBonusAndZeroGravityStayUnchanged() {
        assertEquals(0.7, HorsePhysics.boostedJump(0.7, 0, 0.08));
        assertEquals(0.7, HorsePhysics.boostedJump(0.7, 3, 0));
    }
}
