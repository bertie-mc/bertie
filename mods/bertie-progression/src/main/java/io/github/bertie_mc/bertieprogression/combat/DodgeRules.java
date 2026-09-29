package io.github.bertie_mc.bertieprogression.combat;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

public final class DodgeRules {
    public record Offer(double chance, Runnable response) {}

    private DodgeRules() {}

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void dodge(LivingIncomingDamageEvent event) {
        LivingEntity target = event.getEntity();
        if (target.level().isClientSide()
                || event.getAmount() <= 0
                || DamageFamilies.of(event.getSource()) != DamageFamily.PHYSICAL
                || event.getSource().is(DamageTypeTags.IS_FALL)
                || event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return;
        List<Offer> offers = new ArrayList<>();
        offer(
                offers,
                CombatRegistry.value(target, "confluence_magic_lib:generic.dodge_chance", 0),
                () -> feedback(target));
        offer(offers, CombatRegistry.value(target, "apothic_attributes:dodge_chance", 0), () -> feedback(target));
        if (ModList.get().isLoaded("terra_curio")) TerraDodge.offers(event, offers);
        if (ModList.get().isLoaded("enigmaticlegacyplus")) EnigmaticDodge.offers(event, offers);
        if (offers.isEmpty()) return;
        double roll = target.getRandom().nextDouble();
        for (Offer offer : offers) {
            if (offer.chance() >= 1 || roll < offer.chance()) {
                event.setCanceled(true);
                offer.response().run();
                return;
            }
            roll = (roll - offer.chance()) / (1 - offer.chance());
        }
    }

    public static void offer(List<Offer> offers, double chance, Runnable response) {
        if (chance > 0) offers.add(new Offer(Math.min(1, chance), response));
    }

    public static double combinedChance(double... chances) {
        double remaining = 1;
        for (double chance : chances) remaining *= 1 - Math.clamp(chance, 0, 1);
        return 1 - remaining;
    }

    public static void feedback(LivingEntity entity) {
        if (entity.level() instanceof ServerLevel level) {
            level.sendParticles(
                    ParticleTypes.LARGE_SMOKE,
                    entity.getX(),
                    entity.getY(0.5),
                    entity.getZ(),
                    6,
                    entity.getBbWidth() * 0.25,
                    entity.getBbHeight() * 0.125,
                    entity.getBbWidth() * 0.25,
                    0);
            level.playSound(
                    null, entity.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.6F, 1.2F);
        }
    }
}
