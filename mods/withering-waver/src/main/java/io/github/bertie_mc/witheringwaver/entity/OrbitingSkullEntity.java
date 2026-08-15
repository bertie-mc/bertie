package io.github.bertie_mc.witheringwaver.entity;

import io.github.bertie_mc.witheringwaver.WwEntities;
import java.util.UUID;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * A wither skeleton skull ripped free by the Waver's reap. While orbiting, position is
 * driven entirely by its owner's orbit tick. When the volley launches it, the skull
 * flies off the circle under its own power for a moment and then disintegrates into the
 * shrapnel burst. Never saved: a reload costs the Waver its orbit, which is acceptable
 * for a transient combat state.
 */
public class OrbitingSkullEntity extends Entity {
    private static final int LAUNCH_FLIGHT_TICKS = 5;
    private static final int SHRAPNEL_COUNT = 8;

    private UUID ownerUUID;
    private WitheringWaverEntity cachedOwner;
    private float slotOffsetDeg;
    private boolean seated;
    private boolean launched;
    private int launchTicksLeft;
    private int launchTargetId = -1;
    private int volleyId = -1;

    public OrbitingSkullEntity(EntityType<? extends OrbitingSkullEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    public void setOwner(WitheringWaverEntity owner) {
        this.ownerUUID = owner.getUUID();
        this.cachedOwner = owner;
    }

    public void setSlotOffsetDeg(float degrees) {
        this.slotOffsetDeg = degrees;
    }

    public float getSlotOffsetDeg() {
        return this.slotOffsetDeg;
    }

    public boolean isSeated() {
        return this.seated;
    }

    public void setSeated(boolean seated) {
        this.seated = seated;
    }

    /** Owner-driven movement; also feeds dead reckoning for smooth client motion. */
    public void moveOrbit(Vec3 target) {
        setDeltaMovement(target.subtract(position()));
        setPos(target.x, target.y, target.z);
    }

    /** Fired off the circle: fly toward the target for a beat, then burst. */
    public void launchToward(LivingEntity target, int volleyId) {
        this.launched = true;
        this.launchTicksLeft = LAUNCH_FLIGHT_TICKS;
        this.launchTargetId = target.getId();
        this.volleyId = volleyId;
        setDeltaMovement(
                target.getEyePosition().subtract(position()).normalize().scale(1.6));
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) {
            setPos(position().add(getDeltaMovement()));
            spawnCometTail();
            return;
        }
        if (this.launched) {
            setPos(position().add(getDeltaMovement()));
            if (--this.launchTicksLeft <= 0
                    || !this.level().noCollision(this, getBoundingBox().expandTowards(getDeltaMovement()))) {
                burst();
            }
            return;
        }
        if (this.tickCount % 5 == 0) {
            WitheringWaverEntity owner = getOwner();
            if (owner == null || !owner.isAlive() || !owner.ownsOrbitSkull(this)) {
                if (this.level() instanceof ServerLevel serverLevel) {
                    serverLevel.sendParticles(ParticleTypes.POOF, getX(), getY(), getZ(), 6, 0.1, 0.1, 0.1, 0.01);
                }
                discard();
            }
        }
    }

    /** Disintegrate into the shrapnel pieces that carry the volley's wither payload. */
    private void burst() {
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            discard();
            return;
        }
        Vec3 origin = position();
        Vec3 aim = getDeltaMovement().normalize();
        if (serverLevel.getEntity(this.launchTargetId) instanceof LivingEntity target && target.isAlive()) {
            aim = target.getEyePosition().subtract(origin).normalize();
        }
        WitheringWaverEntity owner = getOwner();
        for (int i = 0; i < SHRAPNEL_COUNT; i++) {
            SkullShrapnelEntity piece = new SkullShrapnelEntity(WwEntities.SKULL_SHRAPNEL.get(), serverLevel);
            if (owner != null) {
                piece.setOwner(owner);
            }
            piece.setVolley(this.volleyId, SHRAPNEL_COUNT);
            piece.setPos(origin.x, origin.y, origin.z);
            piece.setDeltaMovement(aim.add(
                            this.random.triangle(0.0, 0.08),
                            this.random.triangle(0.0, 0.08),
                            this.random.triangle(0.0, 0.08))
                    .normalize()
                    .scale(1.1));
            serverLevel.addFreshEntity(piece);
        }
        serverLevel.sendParticles(ParticleTypes.LARGE_SMOKE, origin.x, origin.y, origin.z, 14, 0.15, 0.15, 0.15, 0.04);
        serverLevel.sendParticles(ParticleTypes.SQUID_INK, origin.x, origin.y, origin.z, 10, 0.1, 0.1, 0.1, 0.08);
        serverLevel.playSound(
                null, origin.x, origin.y, origin.z, SoundEvents.BONE_BLOCK_BREAK, SoundSource.HOSTILE, 1.2F, 0.6F);
        discard();
    }

    /** Black comet tail streaming behind the skull, stretching with speed. */
    private void spawnCometTail() {
        Vec3 back = getDeltaMovement().scale(-1.0);
        if (back.lengthSqr() < 1.0E-4) {
            return;
        }
        for (int i = 1; i <= 3; i++) {
            Vec3 at = position()
                    .add(back.scale(i * 0.8))
                    .add(
                            this.random.triangle(0.0, 0.06),
                            0.1 + this.random.triangle(0.0, 0.06),
                            this.random.triangle(0.0, 0.06));
            var type =
                    switch (i) {
                        case 1 -> ParticleTypes.SQUID_INK;
                        case 2 -> ParticleTypes.SMOKE;
                        default -> ParticleTypes.ASH;
                    };
            this.level().addParticle(type, at.x, at.y, at.z, back.x * 0.05, 0.01, back.z * 0.05);
        }
    }

    private WitheringWaverEntity getOwner() {
        if (this.cachedOwner != null && this.cachedOwner.isAlive()) {
            return this.cachedOwner;
        }
        if (this.ownerUUID != null
                && this.level() instanceof ServerLevel serverLevel
                && serverLevel.getEntity(this.ownerUUID) instanceof WitheringWaverEntity waver) {
            this.cachedOwner = waver;
            return waver;
        }
        return null;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return false;
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {}

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {}

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {}
}
