package io.github.bertie_mc.fletching.mixin;

import io.github.bertie_mc.fletching.ArrowDamage;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "dev.shadowsoffire.apothic_attributes.impl.AttributeEvents", remap = false)
public abstract class AttributeCriticalMixin {
    @Inject(method = "apothCriticalStrike", at = @At("HEAD"), cancellable = true)
    private void bertie$reuseAttackCritical(LivingIncomingDamageEvent event, CallbackInfo ci) {
        var context = ArrowDamage.CURRENT.get();
        if (context == null) return;
        if (context.inheritedCritical) {
            event.setAmount(event.getAmount() * context.criticalMultiplier);
            ci.cancel();
        } else context.criticalInput = event.getAmount();
    }

    @Inject(method = "apothCriticalStrike", at = @At("RETURN"))
    private void bertie$rememberCritical(LivingIncomingDamageEvent event, CallbackInfo ci) {
        var context = ArrowDamage.CURRENT.get();
        if (context != null && !context.inheritedCritical && context.criticalInput > 0)
            context.criticalMultiplier = event.getAmount() / context.criticalInput;
    }
}
