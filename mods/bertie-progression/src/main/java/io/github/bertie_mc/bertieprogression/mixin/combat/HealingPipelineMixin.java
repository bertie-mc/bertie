package io.github.bertie_mc.bertieprogression.mixin.combat;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import io.github.bertie_mc.bertieprogression.combat.HealingRules;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class HealingPipelineMixin {
    @Unique
    private static final ResourceLocation BERTIE_BLEEDING =
            ResourceLocation.fromNamespaceAndPath("simplymore", "bleed");

    @WrapMethod(method = "heal")
    private void bertie$healing(float amount, Operation<Void> original) {
        LivingEntity entity = (LivingEntity) (Object) this;
        float modified = HealingRules.modifiedAmount(entity, amount);
        HealingRules.begin(entity);
        try {
            original.call(modified);
        } finally {
            HealingRules.end();
        }
    }

    @Inject(method = "hasEffect", at = @At("HEAD"), cancellable = true, require = 1)
    private void bertie$avoidBleedingHealthWrite(Holder<MobEffect> effect, CallbackInfoReturnable<Boolean> cir) {
        // The penalty has already been included. This only suppresses the upstream heal() shortcut;
        // the effect remains installed, ticking, visible and available to all other systems.
        if (effect.is(BERTIE_BLEEDING) && HealingRules.suppressNativeBleeding((LivingEntity) (Object) this)) {
            cir.setReturnValue(false);
        }
    }
}
