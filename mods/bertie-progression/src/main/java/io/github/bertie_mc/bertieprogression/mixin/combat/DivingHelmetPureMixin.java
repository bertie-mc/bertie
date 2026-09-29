package io.github.bertie_mc.bertieprogression.mixin.combat;

import io.github.bertie_mc.bertieprogression.combat.DamageFamilies;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "org.confluence.terra_curio.common.item.DivingHelmet", remap = false)
public abstract class DivingHelmetPureMixin {
    @Inject(method = "apply", at = @At("HEAD"), cancellable = true, require = 1)
    private static void bertie$pure(
            LivingEntity target, DamageSource source, float amount, CallbackInfoReturnable<Float> cir) {
        if (DamageFamilies.isPure(source)) cir.setReturnValue(amount);
    }
}
