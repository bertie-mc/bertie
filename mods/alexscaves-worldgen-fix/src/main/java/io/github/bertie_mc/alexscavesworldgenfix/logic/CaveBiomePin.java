package io.github.bertie_mc.alexscavesworldgenfix.logic;

/**
 * Works out the argument Alex's Caves' own {@code replaceBiomes} wants.
 *
 * <p>{@code AbstractCaveGenerationStructurePiece#replaceBiomes(level, biome, offset)} starts at
 * {@code seaLevel - offset}, steps down {@value #STRIDE} blocks at a time, and overwrites each
 * chunk section's biome container until it reaches the bottom of the world. The first write lands a
 * full stride <i>below</i> the start, because upstream moves before it writes.
 *
 * <p>The four pieces that call it pass a fixed number - 16, 20 or 32 - which assumes the cave sits
 * at a predictable depth below sea level. Deriving the offset from the piece's own bounding box
 * instead means the replacement always begins above the cave's roof, wherever the generator put it,
 * and never starts higher than it needs to.
 */
public final class CaveBiomePin {

    /** Upstream's descent per iteration. */
    public static final int STRIDE = 8;

    /** Blocks of clearance kept above the cave's bounding box, so the roof's section is covered. */
    public static final int HEADROOM = 16;

    private CaveBiomePin() {
    }

    /**
     * The {@code offset} that makes {@code replaceBiomes} begin {@value #HEADROOM} blocks above
     * {@code caveTopY}.
     *
     * <p>Negative when the cave's roof is above sea level, which upstream handles: the start is
     * simply {@code seaLevel - offset} and the walk is always downward.
     */
    public static int offsetBelowSeaLevel(int seaLevel, int caveTopY) {
        return seaLevel - (caveTopY + HEADROOM);
    }

    /** The first Y upstream actually writes for a given offset. Mirrors its move-then-write order. */
    public static int firstWrittenY(int seaLevel, int offset) {
        return seaLevel - offset - STRIDE;
    }
}
