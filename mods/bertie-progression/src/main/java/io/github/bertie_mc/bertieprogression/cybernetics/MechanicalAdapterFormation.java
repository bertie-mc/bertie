package io.github.bertie_mc.bertieprogression.cybernetics;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import rearth.oritech.block.base.block.MultiblockMachine;
import rearth.oritech.block.blocks.processing.MachineCoreBlock;

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
                || !event.getEntity().mayUseItemAt(pos.above(), net.minecraft.core.Direction.UP, event.getItemStack()))
            return;
        var upper = level.getBlockState(pos.above());
        boolean place = upper.canBeReplaced()
                && event.getItemStack().getItem() instanceof BlockItem held
                && held.getBlock() instanceof MachineCoreBlock
                && !held.getBlock().equals(MechanicalAdapterRegistry.COPPER_CORE.get());
        if (place)
            upper = ((BlockItem) event.getItemStack().getItem()).getBlock().defaultBlockState();
        if (!(upper.getBlock() instanceof MachineCoreBlock)
                || upper.getValue(MachineCoreBlock.USED)
                || upper.is(MechanicalAdapterRegistry.COPPER_CORE.get())) return;
        if (!level.isClientSide) {
            var core = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(upper.getBlock());
            var formed = state.setValue(MultiblockMachine.ASSEMBLED, true);
            level.setBlockAndUpdate(pos, formed);
            level.setBlockAndUpdate(
                    pos.above(), formed.setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.UPPER));
            ((MechanicalAdapterBlockEntity) level.getBlockEntity(pos.above())).setAssemblyCore(core);
            for (var part : java.util.List.of(pos, pos.above()))
                ((MechanicalAdapterBlockEntity) level.getBlockEntity(part)).attachKinetics();
            if (place && !event.getEntity().isCreative()) event.getItemStack().shrink(1);
        }
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
    }

    private MechanicalAdapterFormation() {}
}
