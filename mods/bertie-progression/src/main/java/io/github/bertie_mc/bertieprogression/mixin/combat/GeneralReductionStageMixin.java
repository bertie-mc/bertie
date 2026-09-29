package io.github.bertie_mc.bertieprogression.mixin.combat;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import io.github.bertie_mc.bertieprogression.combat.EnergyLayers;
import io.github.bertie_mc.bertieprogression.combat.GeneralReductions;
import net.minecraft.core.Holder;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class GeneralReductionStageMixin {
    @WrapMethod(method = "getDamageAfterMagicAbsorb")
    private float bertie$nativeMagicStage(DamageSource source, float amount, Operation<Float> original) {
        var state = EnergyLayers.current((LivingEntity) (Object) this);
        if (state == null) return original.call(source, amount);
        boolean previous = state.inMagicStage();
        state.inMagicStage(true);
        try {
            return original.call(source, amount);
        } finally {
            state.inMagicStage(previous);
        }
    }

    @Inject(method = "getEffect", at = @At("HEAD"), cancellable = true, require = 1)
    private void bertie$avoidLostOrDuplicateReduction(
            Holder<MobEffect> effect, CallbackInfoReturnable<MobEffectInstance> cir) {
        var key = effect.unwrapKey();
        if (key.isEmpty() || !GeneralReductions.isMovedEffect(key.get().location())) return;
        var state = EnergyLayers.current((LivingEntity) (Object) this);
        if (state != null && state.inMagicStage()) cir.setReturnValue(null);
    }
}
