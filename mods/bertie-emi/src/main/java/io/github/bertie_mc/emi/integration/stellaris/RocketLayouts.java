package io.github.bertie_mc.emi.integration.stellaris;

import java.util.List;

final class RocketLayouts {
    record Position(int x, int y) {}

    // Rocket Station menu slots 0-13, preserving the nose, paired hull, fins and engine.
    static final List<Position> CONSTRUCTION = List.of(
            new Position(47, 12),
            new Position(38, 30),
            new Position(56, 30),
            new Position(38, 48),
            new Position(56, 48),
            new Position(38, 66),
            new Position(56, 66),
            new Position(20, 84),
            new Position(38, 84),
            new Position(56, 84),
            new Position(74, 84),
            new Position(20, 102),
            new Position(47, 102),
            new Position(74, 102));

    enum UpgradeSlot {
        MOTOR("Motor", 82),
        TANK("Tank", 100),
        SKIN("Skin", 118),
        MODEL("Model", 136);

        final String label;
        final int x;
        final int y = 74;

        UpgradeSlot(String label, int x) {
            this.label = label;
            this.x = x;
        }
    }

    private RocketLayouts() {}
}
