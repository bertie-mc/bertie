package io.github.bertie_mc.bertieprogression.mixin.combat;

import io.github.bertie_mc.bertieprogression.combat.DamageFamilies;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "net.acetheeldritchking.discerning_the_eldritch.effects.AbracadabraPotionEffect", remap = false)
public abstract class AbracadabraDamageCapMixin {
    @Redirect(
            method = "damageCapEvent",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/neoforged/neoforge/event/entity/living/LivingDamageEvent$Pre;getOriginalDamage()F"),
            require = 1)
    private static float bertie$remainingDamage(LivingDamageEvent.Pre event) {
        return event.getNewDamage();
    }

    @Inject(method = "damageCapEvent", at = @At("HEAD"), cancellable = true, require = 1)
    private static void bertie$pureSkipsCap(LivingDamageEvent.Pre event, CallbackInfo ci) {
        if (DamageFamilies.isPure(event.getSource())) ci.cancel();
    }
}
