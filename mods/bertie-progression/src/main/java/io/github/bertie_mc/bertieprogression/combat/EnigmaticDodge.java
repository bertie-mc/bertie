package io.github.bertie_mc.bertieprogression.combat;

import auviotre.enigmatic.legacy.api.item.ISpellstone;
import auviotre.enigmatic.legacy.contents.item.charms.ScorchedCharm;
import auviotre.enigmatic.legacy.contents.item.etherium.EtheriumProperties;
import auviotre.enigmatic.legacy.contents.item.rings.RedemptionRing;
import auviotre.enigmatic.legacy.contents.item.spellstones.EyeOfNebula;
import auviotre.enigmatic.legacy.handlers.EnigmaticHandler;
import auviotre.enigmatic.legacy.registries.EnigmaticItems;
import io.github.bertie_mc.bertieprogression.mixin.combat.NebulaTeleportInvoker;
import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

public final class EnigmaticDodge {
    private EnigmaticDodge() {}

    public static void offers(LivingIncomingDamageEvent event, List<DodgeRules.Offer> offers) {
        LivingEntity entity = event.getEntity();
        if (EnigmaticHandler.hasCurio(entity, EnigmaticItems.SCORCHED_CHARM)
                && RedemptionRing.Helper.canUseRelic(entity)) {
            DodgeRules.offer(
                    offers,
                    ScorchedCharm.resistanceProbability.get() * (entity.isInLava() ? 0.02 : 0.01),
                    () -> DodgeRules.feedback(entity));
        }
        if (ISpellstone.get(entity).is(EnigmaticItems.EYE_OF_NEBULA)) {
            DodgeRules.offer(offers, EyeOfNebula.dodgeProbability.get() * 0.01, () -> {
                if (event.getSource().getEntity() != null && entity.level() instanceof ServerLevel level) {
                    NebulaTeleportInvoker.bertie$teleport(
                            level, event.getSource().getEntity(), entity);
                }
                entity.invulnerableTime = 20;
            });
        }
        if (entity.getWeaponItem().is(EnigmaticItems.ETHERIUM_SWORD)) {
            double threshold = EtheriumProperties.getShieldThreshold(entity);
            if (entity.isUsingItem() && entity.getUseItem().is(EnigmaticItems.ETHERIUM_SWORD)) threshold *= 1.5;
            DodgeRules.offer(offers, 0.01 + threshold / 2, () -> {
                if (event.getSource().getDirectEntity() instanceof LivingEntity attacker) {
                    var direction = entity.position()
                            .subtract(attacker.position())
                            .normalize()
                            .scale(0.25);
                    attacker.knockback(0.4, direction.x, direction.z);
                }
                entity.invulnerableTime += 20;
                DodgeRules.feedback(entity);
            });
        }
    }
}
