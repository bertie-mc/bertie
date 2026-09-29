package io.github.bertie_mc.fletching;

import com.fletchery.mod.arrow.ChainBindManager;
import com.fletchery.mod.entity.CustomArrowEntity;
import io.github.bertie_mc.fletching.mixin.ArrowAccess;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.monster.piglin.Piglin;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class ArrowEffects {
    private ArrowEffects() {}

    public static List<LivingEntity> nearby(CustomArrowEntity arrow, Vec3 center, double range) {
        return new ArrayList<>(arrow.level()
                .getEntitiesOfClass(
                        LivingEntity.class,
                        new AABB(center, center).inflate(range),
                        e -> ArrowDamage.eligible(arrow.getOwner(), e) && e.distanceToSqr(center) <= range * range));
    }

    public static void beforeHit(CustomArrowEntity arrow, LivingEntity target) {
        var p = ((ArrowRuntime) arrow).bertie$flight().profile;
        if (p.tip("iron") && target.isBlocking()) {
            target.stopUsingItem();
            if (target instanceof Player player && !player.getOffhandItem().isEmpty())
                player.getCooldowns().addCooldown(player.getOffhandItem().getItem(), 100);
        }
    }

    public static void hit(CustomArrowEntity arrow, LivingEntity target, ArrowDamage.Context context, Vec3 direction) {
        if (!(arrow.level() instanceof ServerLevel level)) return;
        var state = ((ArrowRuntime) arrow).bertie$flight();
        var p = state.profile;
        if (context.damaged) {
            if (p.feather("resplendent")) {
                CombatBridge.primordial(target);
                launch(target, 10, Vec3.ZERO);
            }
            if (p.feather("amphithere")) target.knockback(1.8, -direction.x, -direction.z);
            int burn = p.feather("sun") ? 400 : (p.extra("blaze_powder") || p.extra("fiery_blood")) ? 100 : 0;
            if (burn > 0) target.setRemainingFireTicks(Math.max(target.getRemainingFireTicks(), burn));
            if (p.feather("sun")) {
                target.addEffect(new MobEffectInstance(MobEffects.GLOWING, 400));
                ArrowDamage.secondary(context.data, arrow.getOwner(), target, 10, "fire");
            }
            if (p.tip("echo")) target.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 100));
            if (p.tip("permafrost")) target.addEffect(new MobEffectInstance(ArrowStatus.FROZEN, 40));
            if (p.tip("gold") && target instanceof Piglin) {
                for (Piglin piglin : level.getEntitiesOfClass(
                        Piglin.class, target.getBoundingBox().inflate(128))) {
                    piglin.getBrain().eraseMemory(MemoryModuleType.ANGRY_AT);
                    piglin.getBrain().eraseMemory(MemoryModuleType.ATTACK_TARGET);
                    piglin.setTarget(null);
                }
            }
            if (p.shaft("fishing") && arrow.getOwner() != null && target.distanceToSqr(arrow.getOwner()) > 4) {
                Vec3 pull = arrow.getOwner()
                        .position()
                        .subtract(target.position())
                        .normalize()
                        .scale(Math.min(3, target.distanceTo(arrow.getOwner()) * .2));
                target.setDeltaMovement(pull.x, .5, pull.z);
                target.hurtMarked = true;
            }
            if (p.shaft("chain")) {
                List<LivingEntity> targets = nearby(arrow, target.position(), 4);
                targets.remove(target);
                targets.addFirst(target);
                for (int i = 0; i < Math.min(6, targets.size()); i++)
                    ChainBindManager.bind(targets.get(i), target.position(), 100);
            }
            if (p.extra("honey")) target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40));
            if (p.extra("dread")) target.addEffect(new MobEffectInstance(ArrowStatus.FRIGHTENED, 60));
            if (p.extra("blood") || p.extra("fiery_blood")) CombatBridge.bleed(target);
            if (p.extra("lightning")) {
                LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
                if (bolt != null) {
                    bolt.setPos(target.position());
                    if (arrow.getOwner() instanceof ServerPlayer player) bolt.setCause(player);
                    level.addFreshEntity(bolt);
                }
            }
            if (p.extra("lapis")) {
                var options = List.of(
                        MobEffects.MOVEMENT_SPEED,
                        MobEffects.MOVEMENT_SLOWDOWN,
                        MobEffects.DIG_SPEED,
                        MobEffects.DAMAGE_BOOST,
                        MobEffects.HEAL,
                        MobEffects.HARM,
                        MobEffects.JUMP,
                        MobEffects.CONFUSION,
                        MobEffects.REGENERATION,
                        MobEffects.DAMAGE_RESISTANCE,
                        MobEffects.FIRE_RESISTANCE,
                        MobEffects.WATER_BREATHING,
                        MobEffects.INVISIBILITY,
                        MobEffects.BLINDNESS,
                        MobEffects.NIGHT_VISION,
                        MobEffects.HUNGER,
                        MobEffects.WEAKNESS,
                        MobEffects.POISON,
                        MobEffects.WITHER,
                        MobEffects.HEALTH_BOOST,
                        MobEffects.ABSORPTION,
                        MobEffects.SATURATION,
                        MobEffects.GLOWING,
                        MobEffects.LEVITATION,
                        MobEffects.LUCK,
                        MobEffects.SLOW_FALLING,
                        MobEffects.CONDUIT_POWER,
                        MobEffects.DOLPHINS_GRACE,
                        MobEffects.BAD_OMEN,
                        MobEffects.HERO_OF_THE_VILLAGE,
                        MobEffects.DARKNESS);
                target.addEffect(new MobEffectInstance(
                        options.get(level.random.nextInt(options.size())),
                        100 + level.random.nextInt(200),
                        level.random.nextInt(3)));
            }
            if (p.extra("wind")) ArrowDamage.secondary(context.data, arrow.getOwner(), target, 1, "secondary");
            ((ArrowAccess) arrow).bertie$potions(target);
            if (p.execute()) ArrowDamage.execute(context.data, arrow.getOwner(), target);
            if (state.depth == 0) {
                if (p.feather("stymphalian")) radial(arrow, target, context.raw);
                if (p.shaft("bone")) shrapnel(arrow, target, direction, context.raw);
                if (p.tip("mnemonic"))
                    ArrowCombatEvents.delay(target, context.data, arrow.getOwner(), context.raw * .5f);
                if (p.tip("resonance")) resonance(arrow, target, context.raw);
                if (p.tip("reinforced_echo")) {
                    nearby(arrow, target.position(), 24).stream()
                            .filter(e -> e != target)
                            .min(Comparator.comparingDouble(e -> e.distanceToSqr(target)))
                            .ifPresent(other -> {
                                sonic(level, target.position(), other.position());
                                ArrowDamage.secondary(context.data, arrow.getOwner(), other, context.raw, "secondary");
                            });
                }
            }
            // These begin after the triggering hit and all its immediate secondary damage.
            if (p.shaft("blaze")) target.addEffect(new MobEffectInstance(ArrowStatus.VULNERABLE, 200));
            if (p.shaft("dark") && target.isAlive()) {
                var candidates = nearby(arrow, target.position(), 8);
                candidates.remove(target);
                if (!candidates.isEmpty())
                    ArrowCombatEvents.link(target, candidates.get(level.random.nextInt(candidates.size())));
            }
        }
        impact(arrow, target.position());
    }

    public static void impact(CustomArrowEntity arrow, Vec3 point) {
        if (!(arrow.level() instanceof ServerLevel level)) return;
        var p = ((ArrowRuntime) arrow).bertie$flight().profile;
        if (p.extra("pearl")) teleport(arrow, point);
        if (p.extra("tnt")) level.explode(arrow, point.x, point.y, point.z, 4, Level.ExplosionInteraction.TNT);
        if (p.extra("dragon")) {
            AreaEffectCloud cloud = new AreaEffectCloud(level, point.x, point.y, point.z);
            cloud.setRadius(3);
            cloud.setDuration(60);
            cloud.setParticle(ParticleTypes.DRAGON_BREATH);
            cloud.addEffect(new MobEffectInstance(MobEffects.HARM, 1));
            if (arrow.getOwner() instanceof LivingEntity living) cloud.setOwner(living);
            level.addFreshEntity(cloud);
        }
        if (p.extra("wind")) {
            for (LivingEntity target : nearby(arrow, point, 7))
                launch(
                        target,
                        7,
                        target.position()
                                .subtract(point)
                                .multiply(1, 0, 1)
                                .normalize()
                                .scale(.6));
            level.playSound(
                    null, BlockPos.containing(point), SoundEvents.WIND_CHARGE_BURST.value(), SoundSource.PLAYERS, 1, 1);
        }
    }

    private static void launch(LivingEntity target, double height, Vec3 horizontal) {
        target.setDeltaMovement(horizontal.x, ArrowDamage.launchSpeed(height), horizontal.z);
        target.hurtMarked = true;
    }

    private static void teleport(CustomArrowEntity arrow, Vec3 point) {
        if (!(arrow.getOwner() instanceof ServerPlayer player)) return;
        for (int y = 0; y < 4; y++) {
            Vec3 destination = point.add(0, y, 0);
            if (player.level().getWorldBorder().isWithinBounds(BlockPos.containing(destination))
                    && player.level()
                            .noCollision(
                                    player, player.getBoundingBox().move(destination.subtract(player.position())))) {
                player.teleportTo(destination.x, destination.y, destination.z);
                return;
            }
        }
    }

    private static void radial(CustomArrowEntity arrow, LivingEntity target, float raw) {
        ServerLevel level = (ServerLevel) arrow.level();
        for (int i = 0; i < 8; i++) {
            double angle = i * Math.PI / 4;
            Vec3 direction =
                    new Vec3(Math.cos(angle) * Math.cos(Math.PI / 6), -.5, Math.sin(angle) * Math.cos(Math.PI / 6));
            CustomArrowEntity child =
                    new CustomArrowEntity(com.fletchery.mod.registry.ModRegistries.CUSTOM_ARROW_ENTITY.get(), level);
            CompoundTag data = arrow.getCustomProperties().copy();
            data.remove("bertieFlight");
            data.putInt("bertieChildDepth", 1);
            data.putDouble("bertieFixedDamage", raw * .5);
            child.setOwner(arrow.getOwner());
            child.setCustomProperties(data);
            child.pickup = AbstractArrow.Pickup.DISALLOWED;
            child.setPos(target.position()
                    .add(0, target.getBbHeight() * .5, 0)
                    .add(direction.scale(Math.max(.7, target.getBbWidth()))));
            child.setDeltaMovement(direction.scale(2));
            ((ArrowRuntime) child).bertie$flight().hit.add(target.getUUID());
            level.addFreshEntity(child);
        }
    }

    private static void shrapnel(CustomArrowEntity arrow, LivingEntity target, Vec3 direction, float raw) {
        ServerLevel level = (ServerLevel) arrow.level();
        Vec3 start = target.position().add(0, target.getBbHeight() * .5, 0);
        Vec3 axis = direction.normalize(), right = axis.cross(new Vec3(0, 1, 0)).normalize();
        if (right.lengthSqr() < .01) right = new Vec3(1, 0, 0);
        Vec3 up = right.cross(axis).normalize();
        for (int i = 0; i < 8; i++) {
            double angle = i * Math.PI / 4, spread = Math.tan(i % 2 == 0 ? Math.PI / 12 : Math.PI / 6);
            Vec3 ray = axis.add(right.scale(Math.cos(angle) * spread))
                    .add(up.scale(Math.sin(angle) * spread))
                    .normalize();
            Vec3 end = start.add(ray.scale(4));
            var hits = ArrowFlight.targets(arrow, start, end, .3);
            hits.removeIf(e -> e == target);
            if (!hits.isEmpty())
                ArrowDamage.secondary(
                        arrow.getCustomProperties(), arrow.getOwner(), hits.getFirst(), raw * .1f, "secondary");
            for (int j = 0; j < 4; j++) {
                Vec3 particle = start.lerp(end, j / 4.0);
                level.sendParticles(ParticleTypes.CRIT, particle.x, particle.y, particle.z, 1, 0, 0, 0, 0);
            }
        }
    }

    private static void resonance(CustomArrowEntity arrow, LivingEntity first, float raw) {
        var visited = new java.util.HashSet<java.util.UUID>();
        visited.add(first.getUUID());
        LivingEntity previous = first;
        float damage = raw;
        for (int i = 0; i < 8; i++) {
            var options = nearby(arrow, previous.position(), 12);
            options.removeIf(e -> visited.contains(e.getUUID()));
            if (options.isEmpty()) break;
            LivingEntity next = options.get(arrow.level().random.nextInt(options.size()));
            damage *= .6f;
            sonic((ServerLevel) arrow.level(), previous.position(), next.position());
            ArrowDamage.secondary(arrow.getCustomProperties(), arrow.getOwner(), next, damage, "secondary");
            visited.add(next.getUUID());
            previous = next;
        }
    }

    public static void sonic(ServerLevel level, Vec3 start, Vec3 end) {
        int count = Math.max(1, Math.min(64, (int) start.distanceTo(end)));
        for (int i = 0; i < count; i++) {
            Vec3 p = start.lerp(end, (double) i / count);
            level.sendParticles(ParticleTypes.SONIC_BOOM, p.x, p.y, p.z, 1, 0, 0, 0, 0);
        }
        level.playSound(
                null, BlockPos.containing(start), SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, .7f, 1.2f);
    }
}
