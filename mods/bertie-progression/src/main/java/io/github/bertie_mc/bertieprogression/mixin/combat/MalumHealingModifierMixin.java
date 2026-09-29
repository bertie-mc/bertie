package io.github.bertie_mc.bertieprogression.mixin.combat;

import net.neoforged.neoforge.event.entity.living.LivingHealEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "com.sammy.malum.core.handlers.MalumAttributeEventHandler", remap = false)
public abstract class MalumHealingModifierMixin {
    @Inject(method = "heal", at = @At("HEAD"), cancellable = true, require = 1)
    private static void bertie$sharedHealing(LivingHealEvent event, CallbackInfo ci) {
        ci.cancel();
    }
}
