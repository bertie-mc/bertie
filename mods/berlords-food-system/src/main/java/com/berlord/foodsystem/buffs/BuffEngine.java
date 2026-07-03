package com.berlord.foodsystem.buffs;

import com.berlord.foodsystem.BFS;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerXpEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Server-side reconciler: keeps mob effects, attribute modifiers and abilities in sync
 * with the foods active in the stomach.
 *
 * Effects use the beacon pattern: finite ambient instances (32s, refreshed after ~8s so
 * night vision never reaches its flicker window), milk-proof via cleared cures, never
 * fighting stronger/longer instances from other sources; removal only touches instances
 * that look like ours (ambient + finite + short).
 */
@EventBusSubscriber(modid = BFS.MODID)
public final class BuffEngine {

    static final int APPLY_TICKS = 32 * 20;
    static final int REFRESH_BELOW = APPLY_TICKS - 8 * 20;

    private static final Set<UUID> FLIGHT_GRANTED = new HashSet<>();

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide()) return;

        var runtime = BuffsConfig.get();
        var activeBuffs = ActiveFoods.activeBuffs(player);

        Map<ResourceLocation, EffectTarget> desiredEffects = new HashMap<>();
        Map<ResourceLocation, BuffsConfig.AttributeDef> desiredModifiers = new HashMap<>();
        boolean desireFlight = false;
        double magnetRadius = 0;

        for (BuffsConfig.FoodBuff buff : activeBuffs) {
            for (BuffsConfig.EffectDef def : buff.effects) {
                desiredEffects.merge(def.effectId(), new EffectTarget(def.effect(), def.amplifier()),
                        (a, b) -> a.amplifier >= b.amplifier ? a : b);
            }
            for (BuffsConfig.AttributeDef def : buff.attributes) {
                desiredModifiers.put(def.modifier().id(), def);
            }
            desireFlight |= buff.abilities.flight;
            magnetRadius = Math.max(magnetRadius, buff.abilities.magnetRadius);
        }

        for (EffectTarget target : desiredEffects.values()) {
            MobEffectInstance current = player.getEffect(target.effect);
            boolean apply;
            if (current == null) {
                apply = true;
            } else if (current.getAmplifier() < target.amplifier) {
                apply = true;
            } else {
                apply = current.getAmplifier() == target.amplifier
                        && !current.isInfiniteDuration()
                        && current.getDuration() < REFRESH_BELOW;
            }
            if (apply) {
                player.addEffect(new MobEffectInstance(target.effect, APPLY_TICKS, target.amplifier, true, false, true));
                MobEffectInstance applied = player.getEffect(target.effect);
                if (applied != null && applied.isAmbient()) {
                    applied.getCures().clear();
                }
            }
        }
        for (Map.Entry<ResourceLocation, Holder<MobEffect>> known : runtime.knownEffects.entrySet()) {
            if (desiredEffects.containsKey(known.getKey())) continue;
            Holder<MobEffect> holder = known.getValue();
            MobEffectInstance current = player.getEffect(holder);
            if (current != null && current.isAmbient() && !current.isInfiniteDuration()
                    && current.getDuration() <= APPLY_TICKS + 5) {
                player.removeEffect(holder);
            }
        }

        for (BuffsConfig.AttributeDef def : desiredModifiers.values()) {
            AttributeInstance instance = player.getAttributes().getInstance(def.attribute());
            if (instance != null && !instance.hasModifier(def.modifier().id())) {
                instance.addTransientModifier(def.modifier());
            }
        }
        for (BuffsConfig.AttributeDef known : runtime.knownModifiers) {
            if (desiredModifiers.containsKey(known.modifier().id())) continue;
            AttributeInstance instance = player.getAttributes().getInstance(known.attribute());
            if (instance != null && instance.hasModifier(known.modifier().id())) {
                instance.removeModifier(known.modifier().id());
            }
        }

        tickFlight(player, desireFlight);

        if (magnetRadius > 0 && player.tickCount % 5 == 0) {
            tickMagnet(player, magnetRadius);
        }
    }

    private record EffectTarget(Holder<MobEffect> effect, int amplifier) {}

    private static void tickFlight(Player player, boolean desired) {
        boolean creativeLike = player.isCreative() || player.isSpectator();
        if (desired) {
            // Track the grant whenever flight is desired on a non-creative player, even if
            // mayfly is already true. After a relog the saved mayfly flag persists but
            // FLIGHT_GRANTED was cleared on logout; without re-tracking, the revoke branch
            // below could never fire and survival flight would stick forever once the food ends.
            if (!creativeLike) {
                FLIGHT_GRANTED.add(player.getUUID());
            }
            if (!player.getAbilities().mayfly) {
                player.getAbilities().mayfly = true;
                player.onUpdateAbilities();
            }
        } else if (FLIGHT_GRANTED.remove(player.getUUID()) && !creativeLike) {
            player.getAbilities().mayfly = false;
            player.getAbilities().flying = false;
            player.onUpdateAbilities();
        }
    }

    private static void tickMagnet(Player player, double radius) {
        AABB box = player.getBoundingBox().inflate(radius);
        double tx = player.getX(), ty = player.getY() + 0.5, tz = player.getZ();
        for (ItemEntity item : player.level().getEntitiesOfClass(ItemEntity.class, box, i -> !i.hasPickUpDelay())) {
            pull(item, tx, ty, tz);
        }
        for (ExperienceOrb orb : player.level().getEntitiesOfClass(ExperienceOrb.class, box)) {
            pull(orb, tx, ty, tz);
        }
    }

    // inlined math: no capturing lambda and no intermediate Vec3 (subtract/normalize/scale) per entity
    private static void pull(Entity e, double tx, double ty, double tz) {
        double dx = tx - e.getX(), dy = ty - e.getY(), dz = tz - e.getZ();
        double len2 = dx * dx + dy * dy + dz * dz;
        if (len2 > 0.5625) { // 0.75^2
            double inv = 0.25 / Math.sqrt(len2);
            e.setDeltaMovement(e.getDeltaMovement().add(dx * inv, dy * inv, dz * inv));
        }
    }

    /**
     * Food buffs never show potion swirls — not even when they merged into an existing
     * visible instance (potion/beacon) of the same effect. Our own instances are already
     * created with visible=false; this catches every remaining path.
     */
    @SubscribeEvent
    public static void onEffectParticles(net.neoforged.neoforge.event.entity.living.EffectParticleModificationEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        ResourceLocation id = BuiltInRegistries.MOB_EFFECT.getKey(event.getEffect().getEffect().value());
        if (id != null && ActiveFoods.grantsEffect(player, id)) {
            event.setVisible(false);
        }
    }

    @SubscribeEvent
    public static void onXpChange(PlayerXpEvent.XpChange event) {
        if (event.getEntity().level().isClientSide()) return;
        int amount = event.getAmount();
        if (amount <= 0) return;
        double mult = ActiveFoods.xpMultiplier(event.getEntity());
        if (mult != 1.0) {
            event.setAmount(Math.max(1, (int) Math.round(amount * mult)));
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        FLIGHT_GRANTED.remove(event.getEntity().getUUID());
    }

    /**
     * enderman_calm ability: canceling this NeoForge hook is exactly what wearing a carved
     * pumpkin does (the vanilla check routes through it), so the enderman never aggros from
     * the player's gaze. No mixin needed.
     */
    @SubscribeEvent
    public static void onEndermanAnger(net.neoforged.neoforge.event.entity.living.EnderManAngerEvent event) {
        Player player = event.getPlayer();
        if (!player.level().isClientSide() && ActiveFoods.hasEndermanCalm(player)) {
            event.setCanceled(true);
        }
    }

    /** food categories are tag-derived; rebuild the cache whenever datapacks/tags reload */
    @SubscribeEvent
    public static void onTagsUpdated(net.neoforged.neoforge.event.TagsUpdatedEvent event) {
        SynergyEngine.invalidate();
    }
}
