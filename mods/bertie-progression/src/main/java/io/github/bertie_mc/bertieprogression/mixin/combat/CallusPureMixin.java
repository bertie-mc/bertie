package io.github.bertie_mc.bertieprogression.mixin.combat;

import io.github.bertie_mc.bertieprogression.combat.DamageFamilies;
import net.minecraft.world.damagesource.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "com.cozary.nameless_trinkets.events.CallusHandler", remap = false)
public abstract class CallusPureMixin {
    @Inject(method = "onPlayerHurt", at = @At("HEAD"), cancellable = true, require = 1)
    private static void bertie$pure(DamageSource source, float amount, CallbackInfoReturnable<Float> cir) {
        if (DamageFamilies.isPure(source)) cir.setReturnValue(amount);
    }
}
