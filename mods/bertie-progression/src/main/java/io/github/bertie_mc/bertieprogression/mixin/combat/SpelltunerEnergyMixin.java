package io.github.bertie_mc.bertieprogression.mixin.combat;

import io.github.bertie_mc.bertieprogression.combat.EnergyLayers;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Pseudo
@Mixin(targets = "auviotre.enigmatic.legacy.contents.item.spellstones.other.Spelltuner$Events", remap = false)
public abstract class SpelltunerEnergyMixin {
    @Redirect(
            method = "onDamage",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/neoforged/neoforge/event/entity/living/LivingDamageEvent$Pre;setNewDamage(F)V",
                            ordinal = 0),
            require = 1)
    private static void bertie$energyContribution(LivingDamageEvent.Pre event, float reduced) {
        EnergyLayers.capture(event, reduced);
    }
}
