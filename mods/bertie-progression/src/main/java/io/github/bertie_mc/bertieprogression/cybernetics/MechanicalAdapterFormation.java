package io.github.bertie_mc.bertieprogression.cybernetics;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import rearth.oritech.block.base.block.MultiblockMachine;

public final class MechanicalAdapterFormation {
    @SubscribeEvent
    public static void form(PlayerInteractEvent.RightClickBlock event) {
        if (event.getHand() != InteractionHand.MAIN_HAND || event.getEntity().isSpectator()) return;
        var level = event.getLevel();
        var pos = event.getPos();
        var state = level.getBlockState(pos);
        if (!state.is(MechanicalAdapterRegistry.BLOCK.get())
                || state.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) != DoubleBlockHalf.LOWER
                || state.getValue(MultiblockMachine.ASSEMBLED)
                || !level.getBlockState(pos.above()).is(CopperAugmentation.casing())
                || !event.getEntity().mayUseItemAt(pos.above(), net.minecraft.core.Direction.UP, event.getItemStack()))
            return;
        if (!level.isClientSide) {
            var formed = state.setValue(MultiblockMachine.ASSEMBLED, true);
            level.setBlockAndUpdate(pos, formed);
            level.setBlockAndUpdate(
                    pos.above(), formed.setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.UPPER));
        }
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
    }

    private MechanicalAdapterFormation() {}
}
