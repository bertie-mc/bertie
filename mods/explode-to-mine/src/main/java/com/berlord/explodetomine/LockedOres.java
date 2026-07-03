package com.berlord.explodetomine;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Registry of "locked" ores: ores that drop nothing when hand-mined and must first be exposed
 * by an explosion, which swaps them to a mineable "cracked" twin block.
 */
public final class LockedOres {
    private static final Map<Block, Supplier<? extends Block>> MAP = new HashMap<>();

    public static void register(Block source, Supplier<? extends Block> crackedTwin) {
        MAP.put(source, crackedTwin);
    }

    /** True if this state is a locked ore (hand-mining should yield nothing). */
    public static boolean isLocked(BlockState state) {
        for (Block source : MAP.keySet()) {
            if (state.is(source)) return true;
        }
        return false;
    }

    /** The cracked twin block for this state, or null if it is not a locked ore. */
    public static Block crackedTwinFor(BlockState state) {
        for (Map.Entry<Block, Supplier<? extends Block>> e : MAP.entrySet()) {
            if (state.is(e.getKey())) return e.getValue().get();
        }
        return null;
    }

    private LockedOres() {}
}
