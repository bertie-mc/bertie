package io.github.bertie_mc.bertieprogression.mixin.combat;

import io.github.bertie_mc.bertieprogression.combat.EnergyLayers;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Pseudo
@Mixin(targets = "com.cozary.nameless_trinkets.events.FracturedNullstoneEvents", remap = false)
public abstract class NullstoneEnergyMixin {
    @Redirect(
            method = "reduceMagicDamage",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/neoforged/neoforge/event/entity/living/LivingIncomingDamageEvent;setAmount(F)V"),
            require = 1)
    private static void bertie$energyContribution(LivingIncomingDamageEvent event, float reduced) {
        EnergyLayers.capture(event, reduced);
    }
}
