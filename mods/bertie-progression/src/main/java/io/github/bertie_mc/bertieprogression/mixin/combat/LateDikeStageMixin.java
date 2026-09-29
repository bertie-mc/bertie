package io.github.bertie_mc.bertieprogression.mixin.combat;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import io.github.bertie_mc.bertieprogression.combat.DamagePools;
import io.github.bertie_mc.bertieprogression.combat.EnergyLayers;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({LivingEntity.class, Player.class})
public abstract class LateDikeStageMixin {
    @Inject(method = "actuallyHurt", at = @At("HEAD"), require = 1)
    private void bertie$enteredDamagePipeline(DamageSource source, float amount, CallbackInfo ci) {
        var state = EnergyLayers.current((LivingEntity) (Object) this);
        if (state != null) state.beforeVanillaDamage(false);
    }

    @ModifyExpressionValue(
            method = "actuallyHurt",
            at =
                    @At(
                            value = "INVOKE",
                            target = "Lnet/neoforged/neoforge/common/damagesource/DamageContainer;getNewDamage()F",
                            ordinal = 3),
            require = 1)
    private float bertie$dikeAfterAbsorption(float damage, @Local(argsOnly = true) DamageSource source) {
        return DamagePools.afterAbsorption((LivingEntity) (Object) this, source, damage);
    }
}
