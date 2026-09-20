package io.github.bertie_mc.creatures.server.entity.living;

import io.github.bertie_mc.creatures.BertieCreatures;
import io.github.bertie_mc.creatures.server.entity.ACEntityRegistry;
import io.github.bertie_mc.creatures.server.entity.ai.GroundPathNavigatorNoSpin;
import io.github.bertie_mc.creatures.server.entity.item.NuclearExplosionEntity;
import io.github.bertie_mc.creatures.server.entity.util.ActivatesSirens;
import io.github.bertie_mc.creatures.server.misc.ACSoundRegistry;
import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class NucleeperEntity extends Monster implements ActivatesSirens, PowerableMob {

    private float closeProgress;
    private float prevCloseProgress;
    private float explodeProgress;
    private float prevExplodeProgress;
    private float sirenAngle;
    private float prevSirenAngle;
    private int catScareTime = 0;

    private boolean spawnedExplosion = false;
    private static final EntityDataAccessor<Boolean> TRIGGERED =
            SynchedEntityData.defineId(NucleeperEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> CLOSE_TIME =
            SynchedEntityData.defineId(NucleeperEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> EXPLODING =
            SynchedEntityData.defineId(NucleeperEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> CHARGED =
            SynchedEntityData.defineId(NucleeperEntity.class, EntityDataSerializers.BOOLEAN);

    public NucleeperEntity(EntityType<? extends Monster> entityType, Level level) {
        super(entityType, level);
    }

    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));

        this.goalSelector.addGoal(2, new MeleeGoal());
        this.goalSelector.addGoal(3, new RandomStrollGoal(this, 1.0D, 45));
        this.goalSelector.addGoal(4, new LookAtPlayerGoal(this, Player.class, 15.0F));
        this.goalSelector.addGoal(5, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true, false));
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MOVEMENT_SPEED, 0.2D)
                .add(Attributes.MAX_HEALTH, 40.0D)
                .add(Attributes.ARMOR, 4.0D);
    }

    protected PathNavigation createNavigation(Level level) {
        return new GroundPathNavigatorNoSpin(this, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(TRIGGERED, false);
        builder.define(CLOSE_TIME, 0);
        builder.define(EXPLODING, false);
        builder.define(CHARGED, false);
    }

    public int getCloseTime() {
        return this.entityData.get(CLOSE_TIME);
    }

    public void setCloseTime(int time) {
        this.entityData.set(CLOSE_TIME, time);
    }

    public boolean isTriggered() {
        return this.entityData.get(TRIGGERED);
    }

    public void setTriggered(boolean triggered) {
        this.entityData.set(TRIGGERED, triggered);
    }

    public boolean isExploding() {
        return this.entityData.get(EXPLODING);
    }

    public void setExploding(boolean explode) {
        this.entityData.set(EXPLODING, explode);
    }

    public boolean isCharged() {
        return this.entityData.get(CHARGED);
    }

    public void setCharged(boolean explode) {
        this.entityData.set(CHARGED, explode);
    }

    public void addAdditionalSaveData(CompoundTag compoundTag) {
        super.addAdditionalSaveData(compoundTag);
        compoundTag.putBoolean("Charged", this.isCharged());
        compoundTag.putInt("CloseTime", this.getCloseTime());
    }

    public void readAdditionalSaveData(CompoundTag compoundTag) {
        super.readAdditionalSaveData(compoundTag);
        this.setCharged(compoundTag.getBoolean("Charged"));
        this.setCloseTime(compoundTag.getInt("CloseTime"));
    }

    public void thunderHit(ServerLevel serverLevel, LightningBolt lightningBolt) {
        super.thunderHit(serverLevel, lightningBolt);
        this.setCharged(true);
    }

    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack itemstack = player.getItemInHand(hand);
        if (itemstack.is(ItemTags.CREEPER_IGNITERS)) {
            SoundEvent soundevent =
                    itemstack.is(Items.FIRE_CHARGE) ? SoundEvents.FIRECHARGE_USE : SoundEvents.FLINTANDSTEEL_USE;
            this.level()
                    .playSound(
                            player,
                            this.getX(),
                            this.getY(),
                            this.getZ(),
                            soundevent,
                            this.getSoundSource(),
                            1.0F,
                            this.random.nextFloat() * 0.4F + 0.8F);
            if (!this.level().isClientSide) {
                this.setTriggered(true);
                itemstack.hurtAndBreak(
                        1, player, hand == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
            }

            return InteractionResult.sidedSuccess(this.level().isClientSide);
        } else {
            return super.mobInteract(player, hand);
        }
    }

    @Override
    public void tick() {
        super.tick();
        prevCloseProgress = closeProgress;
        prevExplodeProgress = explodeProgress;
        prevSirenAngle = sirenAngle;
        int time = this.getCloseTime();
        if (this.isExploding() && explodeProgress < 5F) {
            explodeProgress++;
        }
        if (!this.isExploding() && explodeProgress > 0F) {
            explodeProgress--;
        }
        if (this.isTriggered() && !level().isClientSide) {
            if (this.catScareTime > 0 && !this.isExploding()) {
                if (time > 0) {
                    this.setCloseTime(time - 1);
                } else {
                    this.setTriggered(false);
                }
            } else if (time < BertieCreatures.COMMON_CONFIG.nucleeperFuseTime.get()) {
                this.setCloseTime(time + 1);
            } else if (this.isAlive()) {
                this.setExploding(true);
            }
        }
        if (this.isTriggered() && this.isAlive()) {
            BertieCreatures.PROXY.playWorldSound(this, (byte) 1);
        }
        sirenAngle += (10F + 30F * closeProgress) % 360F;
        closeProgress = (float) time / BertieCreatures.COMMON_CONFIG.nucleeperFuseTime.get();
        if (this.catScareTime > 0) {
            this.catScareTime--;
        }
        if (this.isExploding() && explodeProgress >= 5F) {
            this.discard();
            if (!this.level().isClientSide && !spawnedExplosion) {
                this.explode();
                spawnedExplosion = true;
            }
        }
        if (this.isCharged() && this.isAlive() && this.tickCount % 150 == 0) {
            this.heal(1);
        }
    }

    public void remove(Entity.RemovalReason removalReason) {
        BertieCreatures.PROXY.clearSoundCacheFor(this);
        super.remove(removalReason);
    }

    private void explode() {
        NuclearExplosionEntity explosion =
                ACEntityRegistry.NUCLEAR_EXPLOSION.get().create(level());
        explosion.copyPosition(this);
        explosion.setSize(isCharged() ? 1.75F : 1F);
        if (!level().getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING)) {
            explosion.setNoGriefing(true);
        }
        level().addFreshEntity(explosion);
    }

    public float getCloseProgress(float partialTick) {
        return (prevCloseProgress + (closeProgress - prevCloseProgress) * partialTick);
    }

    public float getSirenAngle(float partialTick) {
        return (prevSirenAngle + (sirenAngle - prevSirenAngle) * partialTick);
    }

    public float getExplodeProgress(float partialTick) {
        return (prevExplodeProgress + (explodeProgress - prevExplodeProgress) * partialTick) * 0.2F;
    }

    public void calculateEntityAnimation(boolean flying) {
        float f1 = (float) Mth.length(this.getX() - this.xo, flying ? this.getY() - this.yo : 0, this.getZ() - this.zo);
        float f2 = Math.min(f1 * 8.0F, 1.0F);
        this.walkAnimation.update(f2, 0.4F);
    }

    public float getStepHeight() {
        return 1.1F;
    }

    protected SoundEvent getAmbientSound() {
        return ACSoundRegistry.NUCLEEPER_IDLE.get();
    }

    protected SoundEvent getHurtSound(DamageSource damageSource) {
        return ACSoundRegistry.NUCLEEPER_HURT.get();
    }

    protected SoundEvent getDeathSound() {
        return ACSoundRegistry.NUCLEEPER_DEATH.get();
    }

    protected void playStepSound(BlockPos pos, BlockState state) {
        if (!this.isBaby()) {
            this.playSound(ACSoundRegistry.NUCLEEPER_STEP.get(), 1.0F, 1.0F);
        }
    }

    @Override
    public boolean shouldStopBlaringSirens() {
        return !this.isTriggered() && !this.isExploding() || this.isRemoved();
    }

    @Override
    public boolean isPowered() {
        return this.isCharged();
    }

    private class MeleeGoal extends Goal {

        public MeleeGoal() {
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity target = NucleeperEntity.this.getTarget();
            return target != null && target.isAlive();
        }

        @Override
        public void tick() {
            LivingEntity target = NucleeperEntity.this.getTarget();
            if (target != null && target.isAlive()) {
                NucleeperEntity.this.getNavigation().moveTo(target, 1.0D);
                if (NucleeperEntity.this.distanceTo(target) < 3.5F + target.getBbWidth()) {
                    NucleeperEntity.this.setTriggered(true);
                }
            }
        }
    }
}
