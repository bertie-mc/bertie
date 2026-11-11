package io.github.bertie_mc.bertieprogression.mixin;

import io.github.bertie_mc.bertieprogression.ender.EyeAcquisitions;
import net.minecraft.world.entity.monster.piglin.Piglin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Piglin.class)
public abstract class EyePiglinConversionMixin {
    // Vanilla discards the admired offhand item before emitting the conversion event.
    @Inject(method = "finishConversion", at = @At("HEAD"), require = 1)
    private void bertie$holdEyeThroughConversion(CallbackInfo ci) {
        EyeAcquisitions.beginConversion((Piglin) (Object) this);
    }

    @Inject(method = "finishConversion", at = @At("RETURN"), require = 1)
    private void bertie$restoreAfterFailedConversion(CallbackInfo ci) {
        EyeAcquisitions.finishConversion((Piglin) (Object) this);
    }
}
