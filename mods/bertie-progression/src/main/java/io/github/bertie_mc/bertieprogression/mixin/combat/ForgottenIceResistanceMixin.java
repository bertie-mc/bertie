package io.github.bertie_mc.bertieprogression.mixin.combat;

import io.github.bertie_mc.bertieprogression.combat.DamageFamilies;
import io.github.bertie_mc.bertieprogression.combat.EnergyLayers;
import net.minecraft.tags.DamageTypeTags;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Pseudo
@Mixin(targets = "auviotre.enigmatic.legacy.contents.item.spellstones.ForgottenIce$Events", remap = false)
public abstract class ForgottenIceResistanceMixin {
    @Redirect(
            method = "onDamage",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/neoforged/neoforge/event/entity/living/LivingDamageEvent$Pre;setNewDamage(F)V",
                            ordinal = 1),
            require = 1)
    private static void bertie$separateResistances(LivingDamageEvent.Pre event, float ignored) {
        float before = event.getNewDamage();
        if (DamageFamilies.isEnergy(event.getSource())) {
            EnergyLayers.capture(event, before * 0.7F);
        }
        if (event.getSource().is(DamageTypeTags.IS_PROJECTILE)) {
            event.setNewDamage(event.getNewDamage() * 0.7F);
        }
    }
}
