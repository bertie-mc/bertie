package io.github.bertie_mc.bertieprogression.cybernetics;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import rearth.oritech.block.base.block.MultiblockMachine;
import rearth.oritech.block.blocks.processing.MachineCoreBlock;
import rearth.oritech.block.entity.MachineCoreEntity;
import rearth.oritech.block.entity.augmenter.AugmentApplicationEntity;
import rearth.oritech.util.Geometry;

public final class CopperAugmentation {
    public static Block casing() {
        return BuiltInRegistries.BLOCK.get(ResourceLocation.parse("create:copper_casing"));
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
                                && level.getBlockState(p).is(MechanicalAdapterRegistry.COPPER_CORE.get())
                                && level.getBlockState(p).getValue(MachineCoreBlock.USED)
                                && level.getBlockEntity(p) instanceof MachineCoreEntity core
                                && core.getControllerPos().equals(center.getBlockPos()));
    }

    public static boolean form(AugmentApplicationEntity center, BlockState state) {
        var level = center.getLevel();
        if (level == null) return false;
        if (intact(center)) return true;
        if (!supports(center).stream()
                .allMatch(p -> level.hasChunkAt(p) && level.getBlockState(p).is(casing()))) return false;
        if (!level.isClientSide) {
            center.getConnectedCores().clear();
            for (var pos : supports(center)) {
                level.setBlockAndUpdate(
                        pos,
                        MechanicalAdapterRegistry.COPPER_CORE
                                .get()
                                .defaultBlockState()
                                .setValue(MachineCoreBlock.USED, true));
                ((MachineCoreEntity) level.getBlockEntity(pos)).setControllerPos(center.getBlockPos());
                center.getConnectedCores().add(pos);
            }
            center.setCoreQuality(1);
            if (!state.getValue(MultiblockMachine.ASSEMBLED)) {
                level.setBlockAndUpdate(center.getBlockPos(), state.setValue(MultiblockMachine.ASSEMBLED, true));
                center.setChanged();
            }
        }
        return true;
    }

    public static Boolean placeNext(AugmentApplicationEntity center, Player player) {
        var held = player.getMainHandItem();
        if (!(held.getItem() instanceof BlockItem block) || block.getBlock() != casing()) return false;
        var next = center.getNextMissingCore();
        if (next == null || !player.mayUseItemAt(next, net.minecraft.core.Direction.UP, held)) return false;
        center.getLevel().setBlockAndUpdate(next, casing().defaultBlockState());
        if (!player.isCreative()) held.shrink(1);
        return true;
    }

    public static void validate(AugmentApplicationEntity center) {
        var level = center.getLevel();
        if (level == null || level.getGameTime() % 20 != 0) return;
        if (supports(center).stream().anyMatch(p -> !level.hasChunkAt(p))) return;
        var state = center.getBlockState();
        if (state.getValue(MultiblockMachine.ASSEMBLED) && !intact(center)) {
            // Convert old assembled centers without charging for the same casings twice.
            if (form(center, state)) return;
            center.onCoreBroken(center.getBlockPos());
            center.setChanged();
        }
    }

    private CopperAugmentation() {}
}
