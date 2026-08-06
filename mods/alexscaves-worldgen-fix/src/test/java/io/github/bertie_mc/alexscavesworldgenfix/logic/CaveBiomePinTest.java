package io.github.bertie_mc.alexscavesworldgenfix.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class CaveBiomePinTest {

    private static final int SEA_LEVEL = 63;

    /**
     * Alex's Caves 2.0.10, transcribed from the shipped bytecode: start below sea level, then move
     * before every write, so the first section touched is a full stride down.
     */
    private static int upstreamFirstWrite(int seaLevel, int offset) {
        int y = seaLevel - offset;
        y -= CaveBiomePin.STRIDE;
        return y;
    }

    @Test
    void theWalkStartsAboveTheCaveRoof() {
        int caveTop = -20;

        int offset = CaveBiomePin.offsetBelowSeaLevel(SEA_LEVEL, caveTop);

        assertTrue(
                CaveBiomePin.firstWrittenY(SEA_LEVEL, offset) >= caveTop,
                "the first section written has to be at or above the roof, or the roof keeps the "
                        + "vanilla biome and the seam stays");
    }

    @Test
    void theOffsetMatchesUpstreamsOwnArithmetic() {
        int caveTop = -20;

        int offset = CaveBiomePin.offsetBelowSeaLevel(SEA_LEVEL, caveTop);

        assertEquals(upstreamFirstWrite(SEA_LEVEL, offset), CaveBiomePin.firstWrittenY(SEA_LEVEL, offset));
    }

    @Test
    void clearanceAboveTheRoofIsAFullSection() {
        // HEADROOM minus the stride upstream takes before its first write.
        int caveTop = 5;

        int offset = CaveBiomePin.offsetBelowSeaLevel(SEA_LEVEL, caveTop);

        assertEquals(
                caveTop + CaveBiomePin.HEADROOM - CaveBiomePin.STRIDE, CaveBiomePin.firstWrittenY(SEA_LEVEL, offset));
    }

    @Test
    void aCaveAboveSeaLevelGivesANegativeOffsetAndStillStartsAboveItself() {
        // Nothing clamps the offset upstream; seaLevel - offset simply resolves above sea level.
        int caveTop = SEA_LEVEL + 40;

        int offset = CaveBiomePin.offsetBelowSeaLevel(SEA_LEVEL, caveTop);

        assertTrue(offset < 0, "a roof above sea level has to produce a negative offset");
        assertTrue(CaveBiomePin.firstWrittenY(SEA_LEVEL, offset) >= caveTop);
    }

    @Test
    void aDeepCaveStartsDeepRatherThanAtAFixedDepth() {
        // The four caves that already pin themselves pass a constant (16, 20, 32) and assume the
        // cave sits just under sea level. Deriving it keeps the start close to the actual roof.
        int deepRoof = -50;

        int offset = CaveBiomePin.offsetBelowSeaLevel(SEA_LEVEL, deepRoof);

        assertEquals(SEA_LEVEL - (deepRoof + CaveBiomePin.HEADROOM), offset);
        assertTrue(offset > 32, "a cave this deep needs to start lower than upstream's constants");
    }
}
