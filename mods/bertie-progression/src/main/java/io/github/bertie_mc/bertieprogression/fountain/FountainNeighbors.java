package io.github.bertie_mc.bertieprogression.fountain;

import java.util.List;
import java.util.function.Function;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/** One bounded four-position snapshot per ten seconds, independent of output frequency. */
public final class FountainNeighbors {
    private long updated = Long.MIN_VALUE;
    private List<BlockState> states;

    public List<BlockState> get(long tick, Function<Direction, BlockState> reader) {
        if (states == null || tick < updated || tick - updated >= 200) {
            states = List.of(
                    reader.apply(Direction.NORTH),
                    reader.apply(Direction.WEST),
                    reader.apply(Direction.EAST),
                    reader.apply(Direction.SOUTH));
            updated = tick;
        }
        return states;
    }

    public static BlockState common(List<BlockState> states) {
        BlockState first = states.getFirst();
        return states.stream()
                        .allMatch(state -> state.is(first.getBlock())
                                && (state.getFluidState().isEmpty()
                                        || state.getFluidState().isSource()))
                ? first
                : Blocks.AIR.defaultBlockState();
    }
}
