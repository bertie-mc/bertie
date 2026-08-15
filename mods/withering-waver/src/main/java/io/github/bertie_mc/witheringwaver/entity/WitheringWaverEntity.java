package io.github.bertie_mc.witheringwaver.entity;

import io.github.bertie_mc.witheringwaver.WitheringWaver;
import io.github.bertie_mc.witheringwaver.WwEntities;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RangedAttackGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.monster.WitherSkeleton;
import net.minecraft.world.entity.monster.piglin.AbstractPiglin;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.WitherSkull;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/**
 * Rare wither skeleton variant. Three abilities on top of a wither-skull ranged attack:
 * reaping (harvests skulls off nearby wither skeletons into an orbiting volley),
 * assimilation (consumes nearby non-wither skeletons into bone armor), and a summon
 * used when no wither skeletons are around to reap.
 */
public class WitheringWaverEntity extends Monster implements RangedAttackMob {
    public static final String TAG_SUMMONED = "witheringwaver_summoned";
    public static final String TAG_REAPED = "witheringwaver_reaped";
    public static final String TAG_ASSIMILATED = "witheringwaver_assimilated";

    public static final byte STATE_IDLE = 0;
    public static final byte STATE_REAP_WINDUP = 1;
    public static final byte STATE_ASSIM_WINDUP = 2;
    public static final byte STATE_VOLLEY_ACCEL = 3;
    public static final byte STATE_VOLLEY_FIRE = 4;
    public static final byte STATE_SUMMON_CAST = 5;

    private static final EntityDataAccessor<Byte> DATA_STATE =
            SynchedEntityData.defineId(WitheringWaverEntity.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Boolean> DATA_ASSIMILATED =
            SynchedEntityData.defineId(WitheringWaverEntity.class, EntityDataSerializers.BOOLEAN);

    public static final float BASE_MAX_HEALTH = 120.0F;
    private static final double BASE_ARMOR = 10.0;
    private static final double BASE_TOUGHNESS = 6.0;
    private static final float MAGIC_RESIST_BASE = 0.30F;
    private static final float MAGIC_RESIST_ASSIMILATED = 0.60F;

    private static final double REAP_RADIUS = 15.0;
    private static final double ASSIM_RADIUS = 20.0;
    private static final double VOLLEY_TRIGGER_RANGE = 35.0;
    private static final int WINDUP_TICKS = 14;
    private static final int SUMMON_CAST_TICKS = 20;
    private static final int VOLLEY_ACCEL_TICKS = 40;
    private static final int VOLLEY_FIRE_INTERVAL = 16;
    private static final int LOS_GRACE_TICKS = 20;
    private static final int REAP_COOLDOWN = 240;
    private static final int ASSIM_COOLDOWN = 500;
    private static final int SUMMON_COOLDOWN = 600;
    private static final float LOW_HEALTH_FRACTION = 0.5F;
    private static final int REAP_REFILL_BELOW = 3;
    private static final int REAP_LOCK_AFTER_SUMMON = 140;
    private static final int GLOBAL_ABILITY_GAP = 40;

    private static final float ORBIT_BASE_SPEED_DEG = 4.0F;
    private static final float ORBIT_MAX_SPEED_DEG = 12.0F;
    private static final float ORBIT_TILT_SIN = Mth.sin((float) Math.toRadians(30.0));
    private static final float ORBIT_TILT_COS = Mth.cos((float) Math.toRadians(30.0));

    private static final ResourceLocation ASSIM_HEALTH_ID = WitheringWaver.id("assimilate_health");
    private static final ResourceLocation ASSIM_ARMOR_ID = WitheringWaver.id("assimilate_armor");
    private static final ResourceLocation ASSIM_TOUGHNESS_ID = WitheringWaver.id("assimilate_toughness");

    private final List<OrbitingSkullEntity> orbitSkulls = new ArrayList<>();
    private final Map<Integer, Map<UUID, Integer>> volleyHits = new LinkedHashMap<>();
    private float orbitAngleDeg;
    private int nextVolleyId;
    private int fireCooldown;
    private int losLostTicks;
    private int stateTicks;
    private int reapCooldown;
    private int assimCooldown;
    private int summonCooldown;
    private int reapLockTicks;
    private int globalAbilityGap;
    private int assimKills;
    private int healPendingTicks;

    // Client-side animation bookkeeping, driven by DATA_STATE changes.
    private int clientStateStartTick;

    public WitheringWaverEntity(EntityType<? extends WitheringWaverEntity> type, Level level) {
        super(type, level);
        this.xpReward = 25;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, BASE_MAX_HEALTH)
                .add(Attributes.ARMOR, BASE_ARMOR)
                .add(Attributes.ARMOR_TOUGHNESS, BASE_TOUGHNESS)
                .add(Attributes.MOVEMENT_SPEED, 0.27)
                .add(Attributes.FOLLOW_RANGE, 40.0)
                .add(Attributes.ATTACK_DAMAGE, 7.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.6)
                .add(Attributes.EXPLOSION_KNOCKBACK_RESISTANCE, 1.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_STATE, STATE_IDLE);
        builder.define(DATA_ASSIMILATED, false);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(2, new RangedAttackGoal(this, 1.0, 40, 35.0F));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 1.0));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(6, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, IronGolem.class, true));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, AbstractPiglin.class, true));
    }

    // --- state sync -------------------------------------------------------------

    public byte getAbilityState() {
        return this.entityData.get(DATA_STATE);
    }

    private void setAbilityState(byte state) {
        this.entityData.set(DATA_STATE, state);
        this.stateTicks = 0;
    }

    public boolean isAssimilated() {
        return this.entityData.get(DATA_ASSIMILATED);
    }

    private void setAssimilated(boolean assimilated) {
        this.entityData.set(DATA_ASSIMILATED, assimilated);
    }

    public int getClientStateStartTick() {
        return this.clientStateStartTick;
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> accessor) {
        super.onSyncedDataUpdated(accessor);
        if (DATA_STATE.equals(accessor) && this.level().isClientSide) {
            this.clientStateStartTick = this.tickCount;
        }
    }

    // --- ticking ----------------------------------------------------------------

    @Override
    public void aiStep() {
        super.aiStep();
        if (!this.level().isClientSide) {
            tickCooldowns();
            tickCrumbleCheck();
        }
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        tickOrbit();
        this.stateTicks++;
        switch (getAbilityState()) {
            case STATE_IDLE -> {
                maybeStartAbility();
                maybeStartVolley();
            }
            case STATE_REAP_WINDUP -> {
                if (this.stateTicks >= WINDUP_TICKS) {
                    reapNow();
                    setAbilityState(STATE_IDLE);
                }
            }
            case STATE_ASSIM_WINDUP -> {
                if (this.stateTicks >= WINDUP_TICKS) {
                    assimilateNow();
                    setAbilityState(STATE_IDLE);
                }
            }
            case STATE_SUMMON_CAST -> {
                if (this.stateTicks >= SUMMON_CAST_TICKS) {
                    summonNow();
                    setAbilityState(STATE_IDLE);
                }
            }
            case STATE_VOLLEY_ACCEL -> {
                if (!volleyTargetStillValid()) {
                    abortVolley();
                } else if (this.stateTicks >= VOLLEY_ACCEL_TICKS) {
                    setAbilityState(STATE_VOLLEY_FIRE);
                    this.fireCooldown = 0;
                }
            }
            case STATE_VOLLEY_FIRE -> {
                if (!volleyTargetStillValid()) {
                    abortVolley();
                } else {
                    if (--this.fireCooldown <= 0 && !this.orbitSkulls.isEmpty()) {
                        launchOneSkull();
                        this.fireCooldown = VOLLEY_FIRE_INTERVAL;
                    }
                    if (this.orbitSkulls.isEmpty()) {
                        setAbilityState(STATE_IDLE);
                    }
                }
            }
            default -> setAbilityState(STATE_IDLE);
        }
    }

    private void tickCooldowns() {
        if (this.reapCooldown > 0) this.reapCooldown--;
        if (this.assimCooldown > 0) this.assimCooldown--;
        if (this.summonCooldown > 0) this.summonCooldown--;
        if (this.reapLockTicks > 0) this.reapLockTicks--;
        if (this.globalAbilityGap > 0) this.globalAbilityGap--;
    }

    // --- ability triggers -------------------------------------------------------

    /**
     * Ability priority: heal-and-stack when hurt, refill the orbit when it runs low,
     * armor up opportunistically when bare, and summon reap fodder when the field is empty.
     */
    private void maybeStartAbility() {
        LivingEntity target = getTarget();
        if (target == null || !target.isAlive() || this.globalAbilityGap > 0) {
            return;
        }
        boolean lowHealth = getHealth() < getMaxHealth() * LOW_HEALTH_FRACTION;
        boolean canAssimilate =
                this.assimCooldown == 0 && !assimilableSkeletonsInRadius().isEmpty();
        List<WitherSkeleton> reapable = witherSkeletonsInRadius();
        boolean canReap = !reapable.isEmpty()
                && this.reapCooldown == 0
                && this.reapLockTicks == 0
                && this.orbitSkulls.size() < REAP_REFILL_BELOW;

        if (lowHealth && canAssimilate) {
            beginState(STATE_ASSIM_WINDUP, SoundEvents.WITHER_SKELETON_AMBIENT, 0.5F);
        } else if (canReap) {
            beginState(STATE_REAP_WINDUP, SoundEvents.WITHER_SKELETON_AMBIENT, 0.6F);
        } else if (canAssimilate && !isAssimilated()) {
            beginState(STATE_ASSIM_WINDUP, SoundEvents.WITHER_SKELETON_AMBIENT, 0.5F);
        } else if (reapable.isEmpty() && this.summonCooldown == 0) {
            beginState(STATE_SUMMON_CAST, SoundEvents.WITHER_SKELETON_AMBIENT, 0.8F);
        }
    }

    private void beginState(byte state, SoundEvent sound, float pitch) {
        setAbilityState(state);
        this.globalAbilityGap = GLOBAL_ABILITY_GAP;
        this.losLostTicks = 0;
        playSound(sound, 1.2F, pitch);
    }

    private void maybeStartVolley() {
        if (this.orbitSkulls.isEmpty() || !allSkullsSeated()) {
            return;
        }
        LivingEntity target = getTarget();
        if (target != null
                && target.isAlive()
                && distanceToSqr(target) <= VOLLEY_TRIGGER_RANGE * VOLLEY_TRIGGER_RANGE
                && getSensing().hasLineOfSight(target)) {
            setAbilityState(STATE_VOLLEY_ACCEL);
            this.losLostTicks = 0;
        }
    }

    private boolean volleyTargetStillValid() {
        LivingEntity target = getTarget();
        if (target == null || !target.isAlive()) {
            return false;
        }
        if (getSensing().hasLineOfSight(target)) {
            this.losLostTicks = 0;
        } else if (++this.losLostTicks > LOS_GRACE_TICKS) {
            return false;
        }
        return true;
    }

    private void abortVolley() {
        setAbilityState(STATE_IDLE);
        this.losLostTicks = 0;
    }

    // --- reaping ----------------------------------------------------------------

    /** Immediately executes the reap on every wither skeleton in radius. */
    public void reapNow() {
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        List<WitherSkeleton> victims = witherSkeletonsInRadius();
        int index = this.orbitSkulls.size();
        for (WitherSkeleton victim : victims) {
            victim.addTag(TAG_REAPED);
            Vec3 head = victim.getEyePosition();
            OrbitingSkullEntity skull = new OrbitingSkullEntity(WwEntities.ORBITING_SKULL.get(), serverLevel);
            skull.setPos(head.x, head.y, head.z);
            skull.setOwner(this);
            skull.setSlotOffsetDeg(index * 137.5F);
            serverLevel.addFreshEntity(skull);
            this.orbitSkulls.add(skull);
            index++;

            serverLevel.sendParticles(ParticleTypes.SCULK_SOUL, head.x, head.y, head.z, 12, 0.2, 0.2, 0.2, 0.02);
            executeAbilityKill(victim);
        }
        if (!victims.isEmpty()) {
            playSound(SoundEvents.WITHER_HURT, 1.0F, 0.7F);
        }
        this.reapCooldown = REAP_COOLDOWN;
    }

    /**
     * Ability kills use the attacker-less /kill damage type on purpose. Attributing the
     * hit to the Waver invited every damage-reaction mechanic in the pack (reflection
     * traits, thorns-likes, counter strikes) to answer an effectively infinite hit back
     * at him — with no attacker on the source, there is nothing for them to react to.
     */
    private void executeAbilityKill(LivingEntity victim) {
        victim.hurt(victim.damageSources().genericKill(), Float.MAX_VALUE);
        if (victim.isAlive()) {
            victim.setHealth(0.0F);
        }
    }

    private List<WitherSkeleton> witherSkeletonsInRadius() {
        return this.level()
                .getEntitiesOfClass(
                        WitherSkeleton.class,
                        getBoundingBox().inflate(REAP_RADIUS),
                        e -> e.isAlive() && !e.isRemoved());
    }

    private List<LivingEntity> assimilableSkeletonsInRadius() {
        return this.level()
                .getEntitiesOfClass(
                        LivingEntity.class,
                        getBoundingBox().inflate(ASSIM_RADIUS),
                        e -> e.isAlive() && !e.isRemoved() && isAssimilable(e));
    }

    /** The whole skeleton family — vanilla, tagged modded variants, and Wavers themselves. */
    public static boolean isSkeletonFamily(net.minecraft.world.entity.Entity entity) {
        return entity instanceof net.minecraft.world.entity.monster.AbstractSkeleton
                || entity instanceof WitheringWaverEntity
                || entity.getType().is(EntityTypeTags.SKELETONS);
    }

    private static boolean isAssimilable(LivingEntity entity) {
        if (entity instanceof WitherSkeleton || entity instanceof WitheringWaverEntity) {
            return false;
        }
        return isSkeletonFamily(entity);
    }

    // --- orbit ------------------------------------------------------------------

    private void tickOrbit() {
        this.orbitSkulls.removeIf(s -> s == null || s.isRemoved());
        if (this.orbitSkulls.isEmpty()) {
            return;
        }
        float speed = ORBIT_BASE_SPEED_DEG;
        byte state = getAbilityState();
        if (state == STATE_VOLLEY_ACCEL) {
            speed = Mth.lerp(
                    Math.min(1.0F, this.stateTicks / (float) VOLLEY_ACCEL_TICKS),
                    ORBIT_BASE_SPEED_DEG,
                    ORBIT_MAX_SPEED_DEG);
        } else if (state == STATE_VOLLEY_FIRE) {
            speed = ORBIT_MAX_SPEED_DEG;
        }
        this.orbitAngleDeg = Mth.wrapDegrees(this.orbitAngleDeg + speed);

        double radius = Mth.clamp(1.2 + 0.3 * this.orbitSkulls.size(), 1.5, 4.0);
        Vec3 center = position().add(0.0, 1.9, 0.0);
        for (OrbitingSkullEntity skull : this.orbitSkulls) {
            float angle = this.orbitAngleDeg + skull.getSlotOffsetDeg();
            Vec3 desired = orbitPos(center, radius, angle);
            Vec3 current = skull.position();
            Vec3 delta = desired.subtract(current);
            double dist = delta.length();
            if (skull.isSeated() || dist < 0.5) {
                skull.setSeated(true);
                skull.moveOrbit(desired);
            } else {
                Vec3 step = delta.normalize().scale(Math.min(0.55, dist));
                skull.moveOrbit(current.add(step));
            }
            skull.setYRot(-angle);
        }
    }

    private static Vec3 orbitPos(Vec3 center, double radius, float angleDeg) {
        float rad = (float) Math.toRadians(angleDeg);
        double cos = Mth.cos(rad);
        double sin = Mth.sin(rad);
        return center.add(radius * cos, radius * sin * ORBIT_TILT_SIN, radius * sin * ORBIT_TILT_COS);
    }

    private boolean allSkullsSeated() {
        for (OrbitingSkullEntity skull : this.orbitSkulls) {
            if (!skull.isSeated()) {
                return false;
            }
        }
        return true;
    }

    public boolean ownsOrbitSkull(OrbitingSkullEntity skull) {
        return this.orbitSkulls.contains(skull);
    }

    public boolean hasOrbitingSkulls() {
        return !this.orbitSkulls.isEmpty();
    }

    private void launchOneSkull() {
        LivingEntity target = getTarget();
        if (target == null) {
            return;
        }
        OrbitingSkullEntity skull = this.orbitSkulls.remove(0);
        skull.launchToward(target, this.nextVolleyId++);
        playSound(SoundEvents.WITHER_SHOOT, 1.0F, 1.4F);
    }

    /** Called by shrapnel pieces when they connect; applies the wither tier rules. */
    public void recordShrapnelHit(int volleyId, int volleyTotal, LivingEntity target) {
        Map<UUID, Integer> perTarget = this.volleyHits.computeIfAbsent(volleyId, k -> new HashMap<>());
        int hits = perTarget.merge(target.getUUID(), 1, Integer::sum);
        int amplifier = hits > volleyTotal * 0.6 ? 2 : 1;
        target.addEffect(new MobEffectInstance(MobEffects.WITHER, 160, amplifier), this);
        while (this.volleyHits.size() > 8) {
            Iterator<Integer> it = this.volleyHits.keySet().iterator();
            it.next();
            it.remove();
        }
    }

    // --- assimilation -----------------------------------------------------------

    /** Immediately assimilates every non-wither skeleton in radius. */
    public void assimilateNow() {
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        List<LivingEntity> victims = assimilableSkeletonsInRadius();
        if (victims.isEmpty()) {
            return;
        }
        for (LivingEntity victim : victims) {
            victim.addTag(TAG_ASSIMILATED);
            spawnBoneStream(serverLevel, victim.position().add(0.0, victim.getBbHeight() * 0.5, 0.0));
            executeAbilityKill(victim);
        }
        applyAssimilationBuffs(victims.size());
        playSound(SoundEvents.BONE_BLOCK_PLACE, 1.4F, 0.6F);
        playSound(SoundEvents.WITHER_SKELETON_AMBIENT, 1.2F, 0.4F);
        this.assimCooldown = ASSIM_COOLDOWN;
    }

    private void spawnBoneStream(ServerLevel level, Vec3 from) {
        Vec3 to = position().add(0.0, getBbHeight() * 0.6, 0.0);
        Vec3 step = to.subtract(from).scale(1.0 / 8.0);
        Vec3 pos = from;
        for (int i = 0; i < 8; i++) {
            level.sendParticles(
                    new BlockParticleOption(ParticleTypes.BLOCK, Blocks.BONE_BLOCK.defaultBlockState()),
                    pos.x,
                    pos.y,
                    pos.z,
                    3,
                    0.1,
                    0.1,
                    0.1,
                    0.0);
            pos = pos.add(step);
        }
    }

    /**
     * Per skeleton: +10% max HP, +2 armor +10% armor, +1 toughness +10% toughness.
     * The health bonus is a MULTIPLY_TOTAL modifier, so it is 10% of the fully scaled
     * max — mob-scaling mods like L2Hostility layer their own multiply-total health
     * modifiers on this mob, and the plating must track the mob they actually built.
     * Recasting while armored stacks on top and repeats the full heal.
     */
    private void applyAssimilationBuffs(int kills) {
        this.assimKills += kills;
        AttributeInstance health = getAttribute(Attributes.MAX_HEALTH);
        if (health != null) {
            health.removeModifier(ASSIM_HEALTH_ID);
            health.addPermanentModifier(new AttributeModifier(
                    ASSIM_HEALTH_ID, 0.10 * this.assimKills, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        }
        double baseArmor = unbuffedValue(Attributes.ARMOR, ASSIM_ARMOR_ID);
        double baseToughness = unbuffedValue(Attributes.ARMOR_TOUGHNESS, ASSIM_TOUGHNESS_ID);
        growAssimModifier(Attributes.ARMOR, ASSIM_ARMOR_ID, kills * (2.0 + baseArmor * 0.10));
        growAssimModifier(Attributes.ARMOR_TOUGHNESS, ASSIM_TOUGHNESS_ID, kills * (1.0 + baseToughness * 0.10));
        // Re-asserted (raise-only) on the next tick: attribute caches under some mods
        // can serve a stale max for the tick the modifier lands on.
        this.healPendingTicks = 1;
        setHealth(getMaxHealth());
        setAssimilated(true);
    }

    /** Current total for an attribute with this mob's own assimilation bonus factored out. */
    private double unbuffedValue(
            net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> attribute,
            ResourceLocation id) {
        AttributeInstance instance = getAttribute(attribute);
        if (instance == null) {
            return 0.0;
        }
        AttributeModifier existing = instance.getModifier(id);
        return instance.getValue() - (existing == null ? 0.0 : existing.amount());
    }

    private void growAssimModifier(
            net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> attribute,
            ResourceLocation id,
            double addition) {
        AttributeInstance instance = getAttribute(attribute);
        if (instance == null) {
            return;
        }
        AttributeModifier existing = instance.getModifier(id);
        double total = (existing == null ? 0.0 : existing.amount()) + addition;
        instance.removeModifier(id);
        instance.addPermanentModifier(new AttributeModifier(id, total, AttributeModifier.Operation.ADD_VALUE));
    }

    private void tickCrumbleCheck() {
        if (!isAssimilated()) {
            return;
        }
        if (this.healPendingTicks > 0) {
            this.healPendingTicks--;
            if (getHealth() < getMaxHealth()) {
                setHealth(getMaxHealth());
            }
            return;
        }
        // The bonus band is the top (10% * kills) slice of the multiplied total; the
        // threshold below it is independent of any attribute-cache staleness.
        double threshold = getMaxHealth() / (1.0 + 0.10 * this.assimKills);
        if (getHealth() <= threshold + 1.0E-3) {
            crumbleArmor();
        }
    }

    private void crumbleArmor() {
        this.assimKills = 0;
        removeAssimModifier(Attributes.MAX_HEALTH, ASSIM_HEALTH_ID);
        removeAssimModifier(Attributes.ARMOR, ASSIM_ARMOR_ID);
        removeAssimModifier(Attributes.ARMOR_TOUGHNESS, ASSIM_TOUGHNESS_ID);
        setAssimilated(false);
        if (getHealth() > getMaxHealth()) {
            setHealth(getMaxHealth());
        }
        if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(
                    new BlockParticleOption(ParticleTypes.BLOCK, Blocks.BONE_BLOCK.defaultBlockState()),
                    getX(),
                    getY() + getBbHeight() * 0.5,
                    getZ(),
                    40,
                    0.4,
                    0.8,
                    0.4,
                    0.0);
        }
        playSound(SoundEvents.BONE_BLOCK_BREAK, 1.4F, 0.7F);
    }

    private void removeAssimModifier(
            net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> attribute,
            ResourceLocation id) {
        AttributeInstance instance = getAttribute(attribute);
        if (instance != null) {
            instance.removeModifier(id);
        }
    }

    // --- summon -----------------------------------------------------------------

    /** Immediately summons three drop-less wither skeletons and locks reaping for 7 s. */
    public void summonNow() {
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        DifficultyInstance difficulty = serverLevel.getCurrentDifficultyAt(blockPosition());
        for (int i = 0; i < 3; i++) {
            WitherSkeleton skeleton = EntityType.WITHER_SKELETON.create(serverLevel);
            if (skeleton == null) {
                continue;
            }
            Vec3 pos = findSummonPos(i);
            skeleton.moveTo(pos.x, pos.y, pos.z, this.random.nextFloat() * 360.0F, 0.0F);
            net.neoforged.neoforge.event.EventHooks.finalizeMobSpawn(
                    skeleton, serverLevel, difficulty, MobSpawnType.MOB_SUMMONED, null);
            skeleton.addTag(TAG_SUMMONED);
            if (getTarget() != null) {
                skeleton.setTarget(getTarget());
            }
            serverLevel.addFreshEntity(skeleton);
            serverLevel.sendParticles(ParticleTypes.SOUL, pos.x, pos.y + 1.0, pos.z, 20, 0.3, 0.8, 0.3, 0.02);
            serverLevel.sendParticles(ParticleTypes.POOF, pos.x, pos.y + 0.2, pos.z, 10, 0.3, 0.2, 0.3, 0.01);
        }
        playSound(SoundEvents.WITHER_SKELETON_AMBIENT, 1.5F, 0.5F);
        this.summonCooldown = SUMMON_COOLDOWN;
        this.reapLockTicks = REAP_LOCK_AFTER_SUMMON;
    }

    private Vec3 findSummonPos(int index) {
        float angle = getYRot() * Mth.DEG_TO_RAD + (index - 1) * 1.1F;
        double distance = 2.5 + this.random.nextDouble() * 1.5;
        double x = getX() - Mth.sin(angle) * distance;
        double z = getZ() + Mth.cos(angle) * distance;
        int baseY = Mth.floor(getY());
        for (int dy = 1; dy >= -2; dy--) {
            var pos = net.minecraft.core.BlockPos.containing(x, baseY + dy, z);
            if (this.level().getBlockState(pos.below()).isSolidRender(this.level(), pos.below())
                    && this.level()
                            .getBlockState(pos)
                            .getCollisionShape(this.level(), pos)
                            .isEmpty()
                    && this.level()
                            .getBlockState(pos.above())
                            .getCollisionShape(this.level(), pos.above())
                            .isEmpty()) {
                return new Vec3(x, baseY + dy, z);
            }
        }
        return position();
    }

    public boolean isReapLocked() {
        return this.reapLockTicks > 0;
    }

    // --- ranged attack ----------------------------------------------------------

    @Override
    public void performRangedAttack(LivingEntity target, float power) {
        if (getAbilityState() != STATE_IDLE) {
            return;
        }
        Vec3 look = new Vec3(target.getX() - getX(), target.getY(0.5) - getEyeY() + 0.2, target.getZ() - getZ());
        Vec3 dir = look.normalize();
        WitherSkull skull = new WitherSkull(this.level(), this, dir);
        skull.setPos(getX() + dir.x * 0.8, getEyeY() - 0.2, getZ() + dir.z * 0.8);
        this.level().addFreshEntity(skull);
        playSound(SoundEvents.WITHER_SHOOT, 1.0F, 1.0F);
        swing(net.minecraft.world.InteractionHand.MAIN_HAND);
    }

    // --- damage & effects -------------------------------------------------------

    @Override
    public boolean hurt(DamageSource source, float amount) {
        // Absolute immunity to anything attributed to himself — his own skull
        // explosions and anything a mod redirects back in his name. Mob-scaling mods
        // multiply his outgoing damage, so even one self-splash can be lethal.
        if (source.getEntity() == this || source.getDirectEntity() == this) {
            return false;
        }
        if (source.is(DamageTypeTags.WITCH_RESISTANT_TO)) {
            amount *= 1.0F - (isAssimilated() ? MAGIC_RESIST_ASSIMILATED : MAGIC_RESIST_BASE);
        }
        return super.hurt(source, amount);
    }

    @Override
    public boolean canBeAffected(MobEffectInstance effect) {
        return !effect.is(MobEffects.WITHER) && super.canBeAffected(effect);
    }

    // --- drops ------------------------------------------------------------------

    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean recentlyHit) {
        super.dropCustomDeathLoot(level, source, recentlyHit);
        if (hasOrbitingSkulls()) {
            spawnAtLocation(Items.WITHER_SKELETON_SKULL);
        }
    }

    // --- sounds -----------------------------------------------------------------

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.WITHER_SKELETON_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.WITHER_SKELETON_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.WITHER_SKELETON_DEATH;
    }

    @Override
    protected void playStepSound(
            net.minecraft.core.BlockPos pos, net.minecraft.world.level.block.state.BlockState state) {
        playSound(SoundEvents.WITHER_SKELETON_STEP, 0.15F, 1.0F);
    }

    // --- persistence ------------------------------------------------------------

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("ReapCooldown", this.reapCooldown);
        tag.putInt("AssimCooldown", this.assimCooldown);
        tag.putInt("SummonCooldown", this.summonCooldown);
        tag.putInt("ReapLock", this.reapLockTicks);
        tag.putBoolean("Assimilated", isAssimilated());
        tag.putInt("AssimKills", this.assimKills);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.reapCooldown = tag.getInt("ReapCooldown");
        this.assimCooldown = tag.getInt("AssimCooldown");
        this.summonCooldown = tag.getInt("SummonCooldown");
        this.reapLockTicks = tag.getInt("ReapLock");
        setAssimilated(tag.getBoolean("Assimilated"));
        this.assimKills = tag.getInt("AssimKills");
    }

    /** Orbit skulls are transient; they are not saved, so drop the list state cleanly. */
    @Override
    public void remove(RemovalReason reason) {
        if (!this.level().isClientSide) {
            for (OrbitingSkullEntity skull : this.orbitSkulls) {
                if (!skull.isRemoved()) {
                    skull.discard();
                }
            }
            this.orbitSkulls.clear();
        }
        super.remove(reason);
    }
}
