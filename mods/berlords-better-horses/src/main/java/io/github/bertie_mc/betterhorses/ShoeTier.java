package io.github.bertie_mc.betterhorses;

public enum ShoeTier {
    NONE(0, 0, 0, 0),
    IRON(1, 4, 0, 0),
    GOLD(2, 8, 1, 0),
    DIAMOND(3, 12, 2, 1),
    NETHERITE(4, 16, 3, 2);
    public final int speed, safeFall, jumpHeight, stepHeight;

    ShoeTier(int speed, int safeFall, int jumpHeight, int stepHeight) {
        this.speed = speed;
        this.safeFall = safeFall;
        this.jumpHeight = jumpHeight;
        this.stepHeight = stepHeight;
    }

    public boolean waterWalking() {
        return ordinal() >= DIAMOND.ordinal();
    }

    public boolean lavaWalking() {
        return this == NETHERITE;
    }
}
