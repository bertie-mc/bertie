package io.github.bertie_mc.bertieprogression.mixin.combat;

import auviotre.enigmatic.legacy.handlers.EnigmaticHandler;
import auviotre.enigmatic.legacy.registries.EnigmaticEffects;
import auviotre.enigmatic.legacy.registries.EnigmaticItems;
import io.github.bertie_mc.bertieprogression.combat.DamageFamilies;
import io.github.bertie_mc.bertieprogression.combat.EnergyLayers;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "auviotre.enigmatic.legacy.contents.effect.BlazingMight", remap = false)
public abstract class BlazingMightSubtractionMixin {
    @Inject(method = "onEntityHurt", at = @At("HEAD"), require = 1)
    private void bertie$lateSubtraction(LivingDamageEvent.Pre event, CallbackInfo ci) {
        var state = EnergyLayers.current(event.getEntity());
        var effect = event.getEntity().getEffect(EnigmaticEffects.BLAZING_MIGHT);
        if (state != null
                && effect != null
                && event.getNewDamage() > 0
                && !DamageFamilies.isPure(event.getSource())
                && EnigmaticHandler.hasCurio(event.getEntity(), EnigmaticItems.BERSERK_EMBLEM)) {
            state.subtractLater(2 * effect.getAmplifier() + 1);
        }
    }

    @Redirect(
            method = "onEntityHurt",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/neoforged/neoforge/event/entity/living/LivingDamageEvent$Pre;setNewDamage(F)V"),
            require = 1)
    private void bertie$avoidEarlySubtraction(LivingDamageEvent.Pre event, float amount) {
        if (EnergyLayers.current(event.getEntity()) == null && !DamageFamilies.isPure(event.getSource()))
            event.setNewDamage(amount);
    }
}
