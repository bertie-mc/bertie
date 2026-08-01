package com.berlord.witheredhearts.logic;

/** Dependency-free Wither damage and per-heart rendering policy. */
public final class HeartRenderPolicy {

    private HeartRenderPolicy() {
    }

    public static int remainingHalfHearts(int durationTicks, int amplifier) {
        int damageInterval = Math.max(40 >> amplifier, 10);
        return Math.max(durationTicks, 0) / damageInterval;
    }

    public static Draw nextDraw(boolean container, int remainingHalfHearts, boolean halfHeart) {
        if (container || remainingHalfHearts <= 0) {
            return new Draw(false, Math.max(remainingHalfHearts, 0));
        }
        int consumed = remainingHalfHearts == 1 || halfHeart ? 1 : 2;
        return new Draw(true, remainingHalfHearts - consumed);
    }

    public record Draw(boolean withered, int remainingHalfHearts) {
    }
}
