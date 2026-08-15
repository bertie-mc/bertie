package io.github.bertie_mc.witheringwaver.entity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * One black shard of a disintegrated wither skull. Pieces of one skull share a volley id;
 * the owning Waver counts hits per volley to decide between wither II and wither III.
 */
public class SkullShrapnelEntity extends Projectile {
    private static final float DAMAGE = 2.0F;
    private static final int MAX_LIFE_TICKS = 60;

    private int volleyId = -1;
    private int volleyTotal = 8;
    private int life;

    public SkullShrapnelEntity(EntityType<? extends SkullShrapnelEntity> type, Level level) {
        super(type, level);
    }

    public void setVolley(int volleyId, int volleyTotal) {
        this.volleyId = volleyId;
        this.volleyTotal = volleyTotal;
    }

    @Override
    public void tick() {
        super.tick();

        HitResult hit = ProjectileUtil.getHitResultOnMoveVector(this, this::canHitEntity);
        if (hit.getType() != HitResult.Type.MISS) {
            onHit(hit);
        }
        if (isRemoved()) {
            return;
        }

        Vec3 delta = getDeltaMovement();
        setPos(getX() + delta.x, getY() + delta.y, getZ() + delta.z);
        setDeltaMovement(delta.scale(0.99).add(0.0, -0.02, 0.0));
        updateRotation();

        if (!this.level().isClientSide && ++this.life > MAX_LIFE_TICKS) {
            discard();
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        if (this.level().isClientSide) {
            return;
        }
        if (WitheringWaverEntity.isSkeletonFamily(result.getEntity())) {
            discard();
            return;
        }
        LivingEntity attacker = getOwner() instanceof LivingEntity living ? living : null;
        DamageSource source = damageSources().mobProjectile(this, attacker);
        result.getEntity().hurt(source, DAMAGE);
        // Burst pieces land within each other's hurt-invulnerability window, so most of
        // them deal no damage — but a connecting piece still counts toward the wither
        // tier. The >60% rule is about pieces hitting, not pieces damaging.
        if (result.getEntity() instanceof LivingEntity target) {
            if (getOwner() instanceof WitheringWaverEntity waver) {
                waver.recordShrapnelHit(this.volleyId, this.volleyTotal, target);
            } else {
                target.addEffect(new MobEffectInstance(MobEffects.WITHER, 160, 1), attacker);
            }
        }
        discard();
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        super.onHitBlock(result);
        if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.SMOKE, getX(), getY(), getZ(), 3, 0.05, 0.05, 0.05, 0.01);
            discard();
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return false;
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    /** Spin for the renderer, derived from age so no extra sync is needed. */
    public float getSpin(float partialTicks) {
        return (this.tickCount + partialTicks) * 35.0F * Mth.DEG_TO_RAD;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {}

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
    }
}
