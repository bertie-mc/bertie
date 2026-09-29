package io.github.bertie_mc.bertieprogression.mixin.combat;

import io.github.bertie_mc.bertieprogression.combat.DamageFamilies;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(
        targets = {
            "auviotre.enigmatic.legacy.contents.item.scrolls.NightScroll$Events",
            "auviotre.enigmatic.legacy.contents.item.spellstones.EyeOfNebula$Events",
            "auviotre.enigmatic.legacy.contents.item.spellstones.ForgottenIce$Events"
        },
        remap = false)
public abstract class EnigmaticConditionalPureMixin {
    @Inject(method = "onDamage", at = @At("HEAD"), cancellable = true, require = 1)
    private static void bertie$pure(LivingDamageEvent.Pre event, CallbackInfo ci) {
        if (DamageFamilies.isPure(event.getSource())) ci.cancel();
    }
}
