package io.github.bertie_mc.bertieprogression.mixin;

import io.github.bertie_mc.bertieprogression.cybernetics.AdaptiveImmuneSystem;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "me.moltea.unbound.life.Breathing", remap = false)
public abstract class UnboundVacuumMixin {
    @Inject(method = "begin", at = @At("HEAD"), cancellable = true)
    private static void bertieprogression$protectBreath(LivingEntity entity, CallbackInfoReturnable<Boolean> cir) {
        if (AdaptiveImmuneSystem.protects(entity)) cir.setReturnValue(false);
    }

    @Inject(method = "supplied", at = @At("HEAD"), cancellable = true)
    private static void bertieprogression$hasAir(LivingEntity entity, CallbackInfoReturnable<Boolean> cir) {
        if (AdaptiveImmuneSystem.protects(entity)) cir.setReturnValue(true);
    }

    @Inject(method = "end", at = @At("HEAD"), cancellable = true)
    private static void bertieprogression$preventVacuumDamage(LivingEntity entity, CallbackInfo ci) {
        if (AdaptiveImmuneSystem.protects(entity)) ci.cancel();
    }
}
