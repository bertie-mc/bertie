package io.github.bertie_mc.bertieprogression.mixin.combat;

import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Pseudo
@Mixin(targets = "net.acetheeldritchking.discerning_the_eldritch.effects.ScorchedSoulPotionEffect", remap = false)
public abstract class ScorchedSoulDamageMixin {
    @Redirect(
            method = "livingDamageEventPre",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/neoforged/neoforge/event/entity/living/LivingDamageEvent$Pre;getOriginalDamage()F"),
            require = 1)
    private static float bertie$remainingDamage(LivingDamageEvent.Pre event) {
        return event.getNewDamage();
    }
}
