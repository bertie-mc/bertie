package io.github.bertie_mc.bertieprogression.mixin.combat;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;

@Pseudo
@Mixin(targets = "dev.shadowsoffire.apothic_attributes.impl.AttributeEvents", remap = false)
public abstract class ApothicLifeStealMixin {
    @ModifyExpressionValue(
            method = "lifeStealOverheal",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/world/entity/LivingEntity;getAttributeValue(Lnet/minecraft/core/Holder;)D",
                            ordinal = 0),
            require = 1)
    private double bertie$sharedLifeSteal(double original) {
        // Overheal remains in the native handler; ordinary lifesteal is settled once after all providers.
        return 0;
    }
}
