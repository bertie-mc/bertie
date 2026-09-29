package io.github.bertie_mc.bertieprogression.mixin.combat;

import dev.xkmc.l2damagetracker.contents.attack.DamageData;
import io.github.bertie_mc.bertieprogression.combat.DamageRoutes;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "dev.xkmc.l2hostility.content.traits.common.ReflectTrait", remap = false)
public abstract class L2ReflectionFamilyMixin {
    @Inject(method = "onHurtByMax", at = @At("HEAD"), cancellable = true, require = 1)
    private void bertie$noReflectionLoop(
            int level, LivingEntity reflector, DamageData.OffenceMax data, CallbackInfo ci) {
        if (DamageRoutes.redirected(data.getSource())) ci.cancel();
    }

    @ModifyVariable(method = "onHurtByMax", at = @At("STORE"), require = 1)
    private DamageSource bertie$retainFamily(
            DamageSource nativeCounter, int level, LivingEntity reflector, DamageData.OffenceMax data) {
        return DamageRoutes.reflect(data.getSource(), nativeCounter, reflector);
    }
}
