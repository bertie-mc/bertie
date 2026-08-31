package io.github.bertie_mc.hephaestusarchitecture.structure;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class ForgeStructurePlacementTest {

    /**
     * The native base transcribes F&amp;A's {@code BASE_HEPHAESTUS_PATTERN}, which is symmetric
     * under 90 degree rotation; a transcription slip breaks either the symmetry or the counts.
     * The gametest suite checks the transcription against F&amp;A's real pattern matcher.
     */
    @Test
    void nativeBaseKeepsForbiddenArcanusGeometry() {
        String[] base = ForgeStructurePlacement.NATIVE_BASE;
        assertEquals(9, base.length);

        int[] counts = new int[4];
        for (int z = 0; z < 9; z++) {
            assertEquals(9, base[z].length(), "row " + z);
            for (int x = 0; x < 9; x++) {
                assertEquals(base[z].charAt(x), base[x].charAt(8 - z), "rotated copy of " + x + "," + z);
                switch (base[z].charAt(x)) {
                    case 'P' -> counts[0]++;
                    case 'A' -> counts[1]++;
                    case 'C' -> counts[2]++;
                    case '*' -> counts[3]++;
                    default -> throw new AssertionError("unmapped key at " + x + "," + z);
                }
            }
        }
        assertEquals(48, counts[0], "polished darkstone");
        assertEquals(9, counts[1], "gilded chiseled polished darkstone");
        assertEquals(4, counts[2], "chiseled arcane polished darkstone");
        assertEquals(20, counts[3], "unconstrained positions");
    }
}
