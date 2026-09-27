package io.github.bertie_mc.fletching.mixin;

import com.fletchery.mod.inventory.FletchingInventoryManager;
import io.github.bertie_mc.fletching.TankStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BlockBehaviour.class)
public abstract class TableRemovalMixin {
    @Inject(method = "onRemove", at = @At("HEAD"), remap = false)
    private void bertie$removeTable(
            BlockState oldState, Level level, BlockPos pos, BlockState newState, boolean moving, CallbackInfo ci) {
        if (!oldState.is(Blocks.FLETCHING_TABLE)
                || newState.is(Blocks.FLETCHING_TABLE)
                || !(level instanceof ServerLevel server)) return;
        var manager = FletchingInventoryManager.get(server);
        var inventory = manager.getInventory(pos);
        for (int i = 0; i < 4; i++) Block.popResource(level, pos, inventory.removeItemNoUpdate(i));
        manager.removeInventory(pos);
        TankStorage.get(server).remove(pos);
    }
}
