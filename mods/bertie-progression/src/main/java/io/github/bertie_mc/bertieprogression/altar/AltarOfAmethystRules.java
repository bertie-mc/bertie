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
 * <p>berlord's spec (2026-08-02):
 * <ul>
 *   <li><b>Night only, everywhere.</b> A lush cave does not exempt an altar from this.</li>
 *   <li><b>Line of sight to the sky</b>, beacon-style - <i>except</i> in a lush cave.</li>
 *   <li><b>Moon phase</b> scales speed linearly from 100% at new moon to 200% at full.</li>
 *   <li><b>Lush cave</b> doubles speed. With the sky blocked the moon is ignored and it is a flat
 *       200%; with the sky open both apply and it reaches 400%.</li>
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
            // Out in the open: night AND sky are both mandatory, and the moon is the only modifier.
            return night && sky ? moonMultiplier(level) : STOPPED;
        }
        // A lush cave lifts BOTH the sky requirement and the night one (berlord 2026-08-04 -
        // this reverses the earlier "night only everywhere" reading). Flat 2x on its own; the
        // moon only stacks on top when the sky is actually open and it is actually night, so a
        // full moon over an open-air lush cave is 4x.
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
