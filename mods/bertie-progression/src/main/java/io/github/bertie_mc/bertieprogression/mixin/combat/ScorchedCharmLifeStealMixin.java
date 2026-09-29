package io.github.bertie_mc.bertieprogression.mixin.combat;

import io.github.bertie_mc.bertieprogression.combat.LifeStealRules;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Pseudo
@Mixin(targets = "auviotre.enigmatic.legacy.contents.item.charms.ScorchedCharm$Events", remap = false)
public abstract class ScorchedCharmLifeStealMixin {
    @Redirect(
            method = "onAttack(Lnet/neoforged/neoforge/event/entity/living/LivingDamageEvent$Post;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;heal(F)V"),
            require = 1)
    private static void bertie$collectLifeSteal(LivingEntity attacker, float amount, LivingDamageEvent.Post event) {
        LifeStealRules.collect(event, amount);
    }
}
