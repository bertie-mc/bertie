package com.berlord.hephaestusarchitecture.structure;

/**
 * Minecraft's four horizontal rotations expressed without game types so the
 * coordinate contract can be covered by a plain JVM unit test.
 */
final class RotationMath {

    private RotationMath() {
    }

    static Offset rotate(int x, int y, int z, int clockwiseQuarterTurns) {
        return switch (Math.floorMod(clockwiseQuarterTurns, 4)) {
            case 1 -> new Offset(-z, y, x);
            case 2 -> new Offset(-x, y, -z);
            case 3 -> new Offset(z, y, -x);
            default -> new Offset(x, y, z);
        };
    }

    record Offset(int x, int y, int z) {
    }
}
