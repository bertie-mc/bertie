package io.github.bertie_mc.bertieprogression.cybernetics;

import com.mojang.serialization.MapCodec;
import com.simibubi.create.content.kinetics.base.KineticBlock;
import com.simibubi.create.foundation.block.IBE;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import rearth.oritech.block.base.block.MultiblockMachine;

public final class MechanicalAdapterBlock extends KineticBlock implements IBE<MechanicalAdapterBlockEntity> {
    private static final MapCodec<MechanicalAdapterBlock> CODEC = simpleCodec(MechanicalAdapterBlock::new);

    public MechanicalAdapterBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState()
                .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH)
                .setValue(MultiblockMachine.ASSEMBLED, false)
                .setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.LOWER));
    }

    @Override
    public MapCodec<MechanicalAdapterBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(
                BlockStateProperties.HORIZONTAL_FACING,
                BlockStateProperties.DOUBLE_BLOCK_HALF,
                MultiblockMachine.ASSEMBLED);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        if (context.getClickedPos().getY() >= context.getLevel().getMaxBuildHeight() - 1) return null;
        return defaultBlockState()
                .setValue(
                        BlockStateProperties.HORIZONTAL_FACING,
                        context.getHorizontalDirection().getOpposite());
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState next, boolean moving) {
        super.onRemove(state, level, pos, next, moving);
        if (state.is(next.getBlock())) return;
        BlockPos other = state.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) == DoubleBlockHalf.LOWER
                ? pos.above()
                : pos.below();
        if (level.isClientSide || !level.getBlockState(other).is(this)) return;
        if (state.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) == DoubleBlockHalf.LOWER)
            level.destroyBlock(other, true);
        else level.setBlockAndUpdate(other, level.getBlockState(other).setValue(MultiblockMachine.ASSEMBLED, false));
    }

    @Override
    public boolean hasShaftTowards(LevelReader level, BlockPos pos, BlockState state, Direction face) {
        return state.getValue(MultiblockMachine.ASSEMBLED)
                && face == state.getValue(BlockStateProperties.HORIZONTAL_FACING);
    }

    @Override
    public Direction.Axis getRotationAxis(BlockState state) {
        return state.getValue(BlockStateProperties.HORIZONTAL_FACING).getAxis();
    }

    @Override
    public Class<MechanicalAdapterBlockEntity> getBlockEntityClass() {
        return MechanicalAdapterBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends MechanicalAdapterBlockEntity> getBlockEntityType() {
        return MechanicalAdapterRegistry.ENTITY.get();
    }
}
