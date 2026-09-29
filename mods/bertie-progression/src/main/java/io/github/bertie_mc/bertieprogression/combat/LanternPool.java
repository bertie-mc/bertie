package io.github.bertie_mc.bertieprogression.combat;

import auviotre.enigmatic.legacy.contents.attachement.EnigmaticData;
import auviotre.enigmatic.legacy.contents.item.etherium.EtheriumProperties;
import auviotre.enigmatic.legacy.registries.EnigmaticAttachments;
import auviotre.enigmatic.legacy.registries.EnigmaticSounds;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

public final class LanternPool {
    private LanternPool() {}

    public static void projectileShield(LivingIncomingDamageEvent event) {
        LivingEntity target = event.getEntity();
        if (!DamageFamilies.isPure(event.getSource())
                && event.getSource().is(DamageTypeTags.IS_PROJECTILE)
                && !event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY)
                && EtheriumProperties.hasShield(target)) {
            target.getData(EnigmaticAttachments.ENIGMATIC_DATA).setEtheriumShieldTick(12);
            event.setCanceled(true);
            deflectSound(target);
        }
    }

    public static float consume(LivingEntity target, DamageSource source, float damage) {
        if (!target.hasData(EnigmaticAttachments.ENIGMATIC_DATA)) return damage;
        EnigmaticData data = target.getData(EnigmaticAttachments.ENIGMATIC_DATA);
        float available = data.getEtherealShield();
        if (available <= 0) return damage;
        float spent = Math.min(available, damage);
        data.setEtherealShield(Math.clamp(available - spent, 0, target.getMaxHealth()));
        if (source.getDirectEntity() instanceof LivingEntity attacker) {
            var direction = target.position().subtract(attacker.position()).normalize();
            attacker.knockback(0.55, direction.x, direction.z);
            deflectSound(target);
        }
        return damage - spent;
    }

    private static void deflectSound(LivingEntity target) {
        target.level()
                .playSound(
                        null,
                        target.blockPosition(),
                        EnigmaticSounds.ETHERIUM_SHIELD_DEFLECT.get(),
                        SoundSource.PLAYERS,
                        1,
                        0.9F + target.getRandom().nextFloat() * 0.1F);
    }
}
