package io.github.bertie_mc.alexscavesworldgenfix;

import com.mojang.logging.LogUtils;
import io.github.bertie_mc.alexscavesworldgenfix.logic.BiomeDecorationFailure;
import java.util.concurrent.atomic.AtomicLong;
import net.minecraft.world.level.ChunkPos;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

/** Keeps worldgen alive through Alex's Caves' empty-list biome-decoration clamp. */
@Mod(AlexsCavesWorldgenFix.MOD_ID)
public class AlexsCavesWorldgenFix {

    public static final String MOD_ID = "alexscavesworldgenfix";
    public static final Logger LOGGER = LogUtils.getLogger();

    private static final AtomicLong ABSORBED = new AtomicLong();
    private static final AtomicLong SUBSTITUTED = new AtomicLong();

    public AlexsCavesWorldgenFix() {
        LOGGER.info("[{}] loaded", MOD_ID);
    }

    /**
     * Record one feature the sorter could not identify, skipped instead of placed.
     *
     * <p>Alex's Caves' biomes declare features in the {@code STRONGHOLDS} step, which nothing else
     * populates, so that step's sorted list is empty and the lookup misses. Skipping the feature
     * costs that one feature; before this, the miss threw and cost every later step in the chunk.
     */
    public static void recordSubstitutedFeature() {
        long count = SUBSTITUTED.incrementAndGet();
        if (!BiomeDecorationFailure.shouldReport(count)) {
            return;
        }
        LOGGER.warn(
                "[{}] skipped a decoration feature the sorter could not identify "
                        + "(occurrence {}). Remaining steps for the chunk still run. "
                        + "Upstream: Raguto/AlexsCaves-1.21.1#172",
                MOD_ID,
                count);
    }

    /** How many features have been skipped this run, for diagnostics. */
    public static long substitutedFeatureCount() {
        return SUBSTITUTED.get();
    }

    /**
     * Record one absorbed failure.
     *
     * <p>The chunk keeps whatever decoration ran before the fault and loses the rest, which shows
     * up as missing ores or trees in that chunk alone. That is the trade: a cosmetic gap instead of
     * a dead worldgen thread.
     */
    public static void recordAbsorbedFailure(ChunkPos pos, Throwable thrown) {
        long count = ABSORBED.incrementAndGet();
        if (!BiomeDecorationFailure.shouldReport(count)) {
            return;
        }
        LOGGER.warn(
                "[{}] absorbed Alex's Caves' empty-list decoration clamp at chunk {},{} "
                        + "(occurrence {}). The chunk is under-decorated but worldgen survives. "
                        + "Upstream: Raguto/AlexsCaves-1.21.1#172",
                MOD_ID,
                pos.x,
                pos.z,
                count,
                thrown);
    }

    /** How many failures have been absorbed this run, for diagnostics. */
    public static long absorbedFailureCount() {
        return ABSORBED.get();
    }
}
