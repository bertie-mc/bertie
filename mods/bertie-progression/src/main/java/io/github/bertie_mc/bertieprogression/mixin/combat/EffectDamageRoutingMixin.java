package io.github.bertie_mc.bertieprogression.mixin.combat;

import io.github.bertie_mc.bertieprogression.combat.EffectDamageContext;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(LivingEntity.class)
public abstract class EffectDamageRoutingMixin {
    @ModifyVariable(method = "hurt", at = @At("HEAD"), argsOnly = true, require = 1)
    private DamageSource bertie$effectDamage(DamageSource source) {
        return EffectDamageContext.route((LivingEntity) (Object) this, source);
    }
}
