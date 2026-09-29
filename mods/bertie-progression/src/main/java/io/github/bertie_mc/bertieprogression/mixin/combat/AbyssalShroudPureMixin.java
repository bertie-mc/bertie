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
@Mixin(targets = "io.redspace.ironsspellbooks.effect.AbyssalShroudEffect", remap = false)
public abstract class AbyssalShroudPureMixin {
    @Inject(method = "doEffect", at = @At("HEAD"), cancellable = true, require = 1)
    private static void bertie$pure(LivingEntity entity, DamageSource source, CallbackInfoReturnable<Boolean> cir) {
        if (DamageFamilies.isPure(source)) cir.setReturnValue(false);
    }
}
