package io.github.bertie_mc.bertieprogression.cybernetics;

import com.simibubi.create.AllItems;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import rearth.oritech.block.entity.augmenter.AugmentApplicationEntity;

public final class MechanicalAdapterFormation {
    @SubscribeEvent
    public static void form(PlayerInteractEvent.RightClickBlock event) {
        if (event.getHand() != InteractionHand.MAIN_HAND
                || event.getEntity().isSpectator()
                || !event.getItemStack().is(AllItems.WRENCH.get())) return;
        var level = event.getLevel();
        var pos = event.getPos();
        if (!level.getBlockState(pos).is(CopperAugmentation.casing())
                || !level.getBlockState(pos.above()).is(CopperAugmentation.casing())) return;
        for (Direction towardsCenter : Direction.Plane.HORIZONTAL) {
            var centerPos = pos.relative(towardsCenter, 2);
            if (!level.hasChunkAt(centerPos)
                    || !(level.getBlockEntity(centerPos) instanceof AugmentApplicationEntity center)
                    || center.getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING) != towardsCenter
                    || !CopperAugmentation.intact(center)) continue;
            if (!event.getEntity().mayUseItemAt(pos.above(), Direction.UP, event.getItemStack())) return;
            if (!level.isClientSide) {
                CopperAugmentation.form(center, center.getBlockState());
                var lower = MechanicalAdapterRegistry.BLOCK
                        .get()
                        .defaultBlockState()
                        .setValue(BlockStateProperties.HORIZONTAL_FACING, towardsCenter.getOpposite());
                level.setBlockAndUpdate(pos, lower);
                level.setBlockAndUpdate(
                        pos.above(), lower.setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.UPPER));
            }
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
            return;
        }
    }

    private MechanicalAdapterFormation() {}
}
