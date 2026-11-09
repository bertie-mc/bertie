package io.github.bertie_mc.bertieprogression.mixin;

import io.github.bertie_mc.bertieprogression.fountain.SturdyDeepslateRules;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockBehaviour.BlockStateBase.class)
public abstract class SturdyHardnessMixin {
    @Shadow
    @Final
    private float destroySpeed;

    @Inject(method = "getDestroySpeed", at = @At("HEAD"), cancellable = true)
    private void bertieprogression$sturdySpeed(BlockGetter level, BlockPos pos, CallbackInfoReturnable<Float> cir) {
        if (SturdyDeepslateRules.applies(((BlockState) (Object) this).getBlock())) {
            cir.setReturnValue(SturdyDeepslateRules.hardness(destroySpeed));
        }
    }
}
