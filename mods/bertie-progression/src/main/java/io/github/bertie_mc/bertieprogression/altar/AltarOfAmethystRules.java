package io.github.bertie_mc.bertieprogression.altar;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biomes;

/**
 * When Cataclysm's Altar of Amethyst runs, and how fast.
 *
 * <p>Pure arithmetic on the level and position - no mixin state - so the rules can be read and
 * tested on their own. {@link AltarOfAmethystMixin} is the only caller.
 *
 * <p>Rules:
 * <ul>
 *   <li>Outside a lush cave, the altar requires both night and line of sight to the sky.</li>
 *   <li>Moon phase scales speed linearly from 100% at new moon to 200% at full.</li>
 *   <li>A lush cave always runs at 200%, without requiring night or sky access.</li>
 *   <li>At night with sky access, the lush-cave and moon multipliers stack up to 400%.</li>
 * </ul>
 */
public final class AltarOfAmethystRules {

    private AltarOfAmethystRules() {
    }

    /** Tooltip line on the altar item. */
    public static final ResourceLocation ALTAR_ITEM =
            ResourceLocation.parse("cataclysm:altar_of_amethyst");

    /** Multiplier meaning "does not run at all". */
    public static final float STOPPED = 0.0F;

    /**
     * Speed multiplier for an altar at {@code pos}, or {@link #STOPPED} if it should not tick.
     *
     * <p>1.0 is Cataclysm's stock rate.
     */
    public static float speedAt(Level level, BlockPos pos) {
        boolean lush = level.getBiome(pos).is(Biomes.LUSH_CAVES);
        // Beacon-style: the check is on the column above the block, not the block itself.
        boolean sky = level.canSeeSky(pos.above());
        boolean night = isNight(level);

        if (!lush) {
            // Outside a lush cave, night and sky access are mandatory; only the moon modifies
            // speed.
            return night && sky ? moonMultiplier(level) : STOPPED;
        }
        // A lush cave runs at 2x. Its multiplier stacks with the moon only at night with sky
        // access.
        return sky && night ? 2.0F * moonMultiplier(level) : 2.0F;
    }

    /**
     * 1.0 at new moon rising linearly to 2.0 at full.
     *
     * <p>{@code getMoonPhase()} is 0 at FULL moon and 4 at new, so distance-from-full is
     * {@code min(phase, 8 - phase)} over the eight-phase cycle.
     */
    public static float moonMultiplier(Level level) {
        int fromFull = Math.min(level.getMoonPhase(), 8 - level.getMoonPhase());
        return 2.0F - (fromFull / 4.0F);
    }

    /**
     * Night by the same reckoning the game uses for mob spawning and bed use.
     *
     * <p>Deliberately not {@code Level#isDay}, which is driven by the client's sky-darken value and
     * is affected by weather - a thunderstorm at noon would otherwise count as night.
     */
    public static boolean isNight(Level level) {
        long timeOfDay = level.getDayTime() % 24000L;
        return timeOfDay >= 13000L && timeOfDay < 23000L;
    }
}
