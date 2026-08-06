package io.github.bertie_mc.hephaestusarchitecture.structure;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class ForgeLayoutTest {

    @Test
    void rotatesOffsetsAroundTheForgeAnchor() {
        assertEquals(new RotationMath.Offset(2, 1, 3), RotationMath.rotate(2, 1, 3, 0));
        assertEquals(new RotationMath.Offset(-3, 1, 2), RotationMath.rotate(2, 1, 3, 1));
        assertEquals(new RotationMath.Offset(-2, 1, -3), RotationMath.rotate(2, 1, 3, 2));
        assertEquals(new RotationMath.Offset(3, 1, -2), RotationMath.rotate(2, 1, 3, 3));
    }

    @Test
    void rotationWrapsInBothDirections() {
        assertEquals(RotationMath.rotate(2, 1, 3, 0), RotationMath.rotate(2, 1, 3, 4));
        assertEquals(RotationMath.rotate(2, 1, 3, 3), RotationMath.rotate(2, 1, 3, -1));
    }
}
