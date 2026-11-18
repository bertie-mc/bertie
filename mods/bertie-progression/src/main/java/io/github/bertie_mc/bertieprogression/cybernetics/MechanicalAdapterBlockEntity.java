package io.github.bertie_mc.bertieprogression.cybernetics;

import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import rearth.oritech.block.base.block.MultiblockMachine;
import rearth.oritech.block.entity.augmenter.AugmentApplicationEntity;

public final class MechanicalAdapterBlockEntity extends KineticBlockEntity {
    public MechanicalAdapterBlockEntity(BlockPos pos, BlockState state) {
        super(MechanicalAdapterRegistry.ENTITY.get(), pos, state);
    }

    @Override
    public float calculateStressApplied() {
        lastStressApplied = 8;
        return lastStressApplied;
    }

    public static long chargeRate(float lower, float upper, boolean lowerDriven, boolean upperDriven) {
        if (!lowerDriven
                || !upperDriven
                || !Float.isFinite(lower)
                || !Float.isFinite(upper)
                || lower == 0
                || upper == 0
                || Math.signum(lower) != Math.signum(upper)) return 0;
        return (long) Math.floor(32d * Math.min(Math.abs(lower), Math.abs(upper)));
    }

    private boolean driven(Direction rear) {
        return !isOverStressed()
                && hasSource()
                && worldPosition.relative(rear).equals(source)
                && level.getBlockEntity(source) instanceof KineticBlockEntity external
                && !external.isOverStressed()
                && external.getSpeed() != 0;
    }

    @Override
    public void tick() {
        super.tick();
        if (level == null
                || level.isClientSide
                || !getBlockState().getValue(MultiblockMachine.ASSEMBLED)
                || getBlockState().getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) != DoubleBlockHalf.LOWER) return;
        Direction rear = getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING);
        if (!(level.getBlockEntity(worldPosition.above()) instanceof MechanicalAdapterBlockEntity upper)
                || upper.getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING) != rear
                || upper.getBlockState().getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) != DoubleBlockHalf.UPPER)
            return;
        long charge = chargeRate(getSpeed(), upper.getSpeed(), driven(rear), upper.driven(rear));
        if (charge == 0) return;
        BlockPos centerPos = worldPosition.relative(rear.getOpposite(), 2);
        if (!level.hasChunkAt(centerPos)
                || !(level.getBlockEntity(centerPos) instanceof AugmentApplicationEntity center)
                || center.getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING) != rear.getOpposite()
                || !center.getBlockState().getValue(MultiblockMachine.ASSEMBLED)
                || !CopperAugmentation.intact(center)) return;
        if (((CenterEnergyAccess) center).bertieprogression$internalEnergy().insert(charge, false) > 0)
            center.setChanged();
    }
}
