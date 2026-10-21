package io.github.bertie_mc.bertieprogression.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.github.bertie_mc.bertieprogression.compat.LightningMagnetInputs;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;

@Pseudo
@Mixin(targets = "dev.dubhe.anvilcraft.event.LightningEventListener", remap = false)
public abstract class LightningMagnetInputMixin {
    @WrapOperation(
            method = "onLightingStrike",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/world/level/block/state/BlockState;is(Lnet/minecraft/world/level/block/Block;)Z"),
            require = 1)
    private static boolean bertie$magnetInput(BlockState state, Block block, Operation<Boolean> original) {
        return block == Blocks.IRON_BLOCK ? LightningMagnetInputs.accepts(state) : original.call(state, block);
    }
}
