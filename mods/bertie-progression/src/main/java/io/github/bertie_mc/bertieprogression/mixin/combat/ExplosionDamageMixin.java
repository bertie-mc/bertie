package io.github.bertie_mc.bertieprogression.mixin.combat;

import io.github.bertie_mc.bertieprogression.combat.DamageFamilies;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.level.Explosion;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Explosion.class)
public abstract class ExplosionDamageMixin {
    @Shadow
    @Final
    @Mutable
    private DamageSource damageSource;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void bertie$actualExplosion(CallbackInfo ci) {
        damageSource = DamageFamilies.explosion(damageSource);
    }
}
