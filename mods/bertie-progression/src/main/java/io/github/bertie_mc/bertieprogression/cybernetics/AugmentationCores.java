package io.github.bertie_mc.bertieprogression.cybernetics;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import rearth.oritech.block.base.block.MultiblockMachine;
import rearth.oritech.block.blocks.processing.MachineCoreBlock;
import rearth.oritech.block.entity.MachineCoreEntity;
import rearth.oritech.block.entity.augmenter.AugmentApplicationEntity;
import rearth.oritech.util.Geometry;

public final class AugmentationCores {
    public static Block basic() {
        return BuiltInRegistries.BLOCK.get(ResourceLocation.parse("oritech:machine_core_1"));
    }

    public static List<BlockPos> supports(AugmentApplicationEntity center) {
        return center.getCorePositions().stream()
                .map(p -> center.getBlockPos().offset(Geometry.rotatePosition(p, center.getFacingForMultiblock())))
                .toList();
    }

    public static boolean intact(AugmentApplicationEntity center) {
        var level = center.getLevel();
        return level != null
                && supports(center).stream()
                        .allMatch(p -> level.hasChunkAt(p)
                                && level.getBlockState(p).getBlock() instanceof MachineCoreBlock
                                && !level.getBlockState(p).is(MechanicalAdapterRegistry.COPPER_CORE.get())
                                && level.getBlockState(p).getValue(MachineCoreBlock.USED)
                                && level.getBlockEntity(p) instanceof MachineCoreEntity core
                                && center.getBlockPos().equals(core.getControllerPos()));
    }

    /** Preserve existing assemblies while retiring the former Copper Casing substitute. */
    public static boolean restoreLegacy(AugmentApplicationEntity center) {
        var level = center.getLevel();
        if (level == null || supports(center).stream().anyMatch(p -> !level.hasChunkAt(p))) return false;
        var legacy = supports(center).stream()
                .filter(p -> level.getBlockState(p).is(MechanicalAdapterRegistry.COPPER_CORE.get()))
                .toList();
        if (legacy.isEmpty()) return true;
        center.onControllerBroken();
        level.setBlockAndUpdate(
                center.getBlockPos(), center.getBlockState().setValue(MultiblockMachine.ASSEMBLED, false));
        for (var pos : legacy) level.setBlockAndUpdate(pos, basic().defaultBlockState());
        center.initMultiblock(center.getBlockState());
        center.setChanged();
        return true;
    }

    private AugmentationCores() {}
}
