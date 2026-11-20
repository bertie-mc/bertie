package io.github.bertie_mc.bertieprogression.cybernetics;

import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import rearth.oritech.block.base.block.MultiblockMachine;
import rearth.oritech.block.entity.MachineCoreEntity;
import rearth.oritech.block.entity.augmenter.AugmentApplicationEntity;

public final class MechanicalAdapterBlockEntity extends KineticBlockEntity {
    private ResourceLocation assemblyCore = ResourceLocation.parse("oritech:machine_core_1");

    public ResourceLocation assemblyCore() {
        return assemblyCore;
    }

    public void setAssemblyCore(ResourceLocation core) {
        assemblyCore = core;
        setChanged();
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        tag.putString("AssemblyCore", assemblyCore.toString());
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        if (tag.contains("AssemblyCore")) assemblyCore = ResourceLocation.parse(tag.getString("AssemblyCore"));
    }

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
        BlockPos mount = worldPosition.relative(rear.getOpposite());
        var attached = level.getBlockEntity(mount);
        var center = attached instanceof MachineCoreEntity core
                        && core.getCachedController() instanceof AugmentApplicationEntity controller
                ? controller
                : attached instanceof AugmentApplicationEntity controller ? controller : null;
        if (center == null
                || center.getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING) != rear.getOpposite()
                || !center.getBlockState().getValue(MultiblockMachine.ASSEMBLED)
                || !AugmentationCores.intact(center)) return;
        if (((CenterEnergyAccess) center).bertieprogression$internalEnergy().insert(charge, false) > 0)
            center.setChanged();
    }
}
