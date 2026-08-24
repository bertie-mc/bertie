package io.github.bertie_mc.voidfog.client;

import io.github.bertie_mc.voidfog.FogCurve;
import io.github.bertie_mc.voidfog.VoidFogConfig;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * How much of the fog survives where the player is standing, given how close the nearest way out to
 * the sky is.
 *
 * <p>The original effect gave way under an opening and came back as you walked in, which is what
 * makes a mineshaft mouth feel like shelter. Two things keep that from flickering: the falloff is
 * quadratic in distance rather than a threshold, and the scan result is eased towards over the ticks
 * between scans, so crossing a chunk boundary that changes the answer takes about a second to show.
 */
public final class SkyProximity {
    /** Full strength to none takes about this many ticks - a second and a bit. */
    private static final float EASE_PER_TICK = 0.04F;

    private static float current = 1.0F;
    private static float target = 1.0F;
    private static int countdown;

    private SkyProximity() {}

    /** The eased multiplier, 0 (an opening at hand) to 1 (sealed in). */
    public static float factor() {
        return current;
    }

    public static void reset() {
        current = 1.0F;
        target = 1.0F;
        countdown = 0;
    }

    public static void tick(ClientLevel level, BlockPos eye) {
        int radius = VoidFogConfig.SKY_RADIUS.getAsInt();
        if (radius <= 0) {
            current = 1.0F;
            target = 1.0F;
            return;
        }
        if (--countdown <= 0) {
            countdown = VoidFogConfig.SKY_INTERVAL.getAsInt();
            target = FogCurve.skyFalloff(nearestOpenColumn(level, eye, radius), radius);
        }
        current = FogCurve.ease(current, target, EASE_PER_TICK);
    }

    /**
     * Horizontal distance to the nearest column that is open to the sky at or below the eye, or -1
     * when none is within {@code radius}.
     *
     * <p>This reads heightmaps, not blocks: one lookup per column and no chunk section is touched,
     * which is what makes scanning a 24-block disc cheap enough to do while a player walks. A column
     * counts as open when the highest block that would stop motion is at or below the eye, which is
     * the same test the game uses for daylight.
     */
    private static double nearestOpenColumn(ClientLevel level, BlockPos eye, int radius) {
        int eyeY = eye.getY();
        int best = Integer.MAX_VALUE;
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                int squared = dx * dx + dz * dz;
                if (squared > radius * radius || squared >= best) {
                    continue;
                }
                int x = eye.getX() + dx;
                int z = eye.getZ() + dz;
                if (!level.hasChunkAt(new BlockPos(x, eyeY, z))) {
                    continue;
                }
                if (level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z) <= eyeY) {
                    best = squared;
                }
            }
        }
        return best == Integer.MAX_VALUE ? -1.0 : Math.sqrt(best);
    }
}
