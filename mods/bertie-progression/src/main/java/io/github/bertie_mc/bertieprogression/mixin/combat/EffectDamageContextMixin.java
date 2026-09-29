package io.github.bertie_mc.bertieprogression.mixin.combat;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.github.bertie_mc.bertieprogression.combat.EffectDamageContext;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(MobEffectInstance.class)
public abstract class EffectDamageContextMixin {
    @Shadow
    public abstract Holder<MobEffect> getEffect();

    @WrapOperation(
            method = "tick",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/world/effect/MobEffect;applyEffectTick(Lnet/minecraft/world/entity/LivingEntity;I)Z"),
            require = 1)
    private boolean bertie$effectOrigin(
            MobEffect effect, LivingEntity target, int amplifier, Operation<Boolean> original) {
        EffectDamageContext.begin(target, getEffect());
        try {
            return original.call(effect, target, amplifier);
        } finally {
            EffectDamageContext.end();
        }
    }
}
