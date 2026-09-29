package io.github.bertie_mc.bertieprogression.mixin.combat;

import io.github.bertie_mc.bertieprogression.combat.HealingRules;
import net.neoforged.neoforge.event.entity.living.LivingHealEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "dev.xkmc.youkaishomecoming.events.EffectEventHandlers", remap = false)
public abstract class BreathingHealingMixin {
    @Inject(method = "onHeal", at = @At("HEAD"), cancellable = true, require = 1)
    private static void bertie$sharedHealing(LivingHealEvent event, CallbackInfo ci) {
        if (!HealingRules.readingProvider()) {
            ci.cancel();
        }
    }
}
