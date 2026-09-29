package io.github.bertie_mc.fletching.mixin;

import com.fletchery.mod.entity.CustomArrowEntity;
import io.github.bertie_mc.fletching.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = CustomArrowEntity.class, remap = false)
public abstract class RedesignedProjectileMixin extends AbstractArrow implements ArrowRuntime {
    @Inject(method = "getPickupItem", at = @At("HEAD"), cancellable = true)
    private void bertie$cleanPickup(CallbackInfoReturnable<net.minecraft.world.item.ItemStack> cir) {
        if (ArrowProfile.redesigned(bertie$self().getCustomProperties()))
            cir.setReturnValue(ArrowRecipe.fromData(bertie$self().getCustomProperties(), 1, registryAccess()));
    }

    @Unique
    private FlightState bertie$state;

    @Unique
    private Vec3 bertie$impact;

    protected RedesignedProjectileMixin(EntityType<? extends AbstractArrow> type, Level level) {
        super(type, level);
    }

    @Unique
    private CustomArrowEntity bertie$self() {
        return (CustomArrowEntity) (Object) this;
    }

    @Override
    public FlightState bertie$flight() {
        if (bertie$state == null) bertie$state = new FlightState(bertie$self().getCustomProperties());
        return bertie$state;
    }

    @Override
    public void bertie$grounded(boolean value) {
        inGround = value;
        if (!value) {
            shakeTime = 0;
            inGroundTime = 0;
        }
        hasImpulse = true;
    }

    @Inject(
            method = {"setCustomProperties", "readAdditionalSaveData"},
            at = @At("TAIL"))
    private void bertie$reset(CompoundTag tag, CallbackInfo ci) {
        bertie$state = null;
    }

    @Inject(method = "addAdditionalSaveData", at = @At("HEAD"))
    private void bertie$save(CompoundTag tag, CallbackInfo ci) {
        if (ArrowProfile.redesigned(bertie$self().getCustomProperties()))
            bertie$flight().save(bertie$self().getCustomProperties());
    }

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void bertie$tick(CallbackInfo ci) {
        CustomArrowEntity arrow = bertie$self();
        bertie$syncProperties();
        if (!ArrowProfile.redesigned(arrow.getCustomProperties())) return;
        Vec3 start = position();
        bertie$flight().redirectStart = null;
        bertie$impact = null;
        if (!inGround && ArrowFlight.beforeTick(arrow)) {
            ci.cancel();
            return;
        }
        super.tick();
        // Vanilla advances after onHit; trace a redirected segment on the next tick instead.
        if (bertie$flight().redirectStart != null && !isRemoved()) setPos(bertie$flight().redirectStart);
        ArrowFlight.afterTick(arrow, start, bertie$impact == null ? position() : bertie$impact);
        ci.cancel();
    }

    @Inject(method = "onHitEntity", at = @At("HEAD"), cancellable = true)
    private void bertie$entityHit(EntityHitResult hit, CallbackInfo ci) {
        CustomArrowEntity arrow = bertie$self();
        if (!ArrowProfile.redesigned(arrow.getCustomProperties())) return;
        bertie$impact = hit.getLocation();
        FlightState state = bertie$flight();
        state.peak = Math.max(state.peak, getY());
        state.travelledTo(hit.getLocation());
        Vec3 incoming = getDeltaMovement();
        Vec3 direction = incoming.normalize();
        state.hit.add(hit.getEntity().getUUID());
        LivingEntity target = ArrowDamage.living(hit.getEntity());
        if (target != null) state.hit.add(target.getUUID());
        CompoundTag data = arrow.getCustomProperties().copy();
        data.putBoolean("bertieNoRecovery", pickup != Pickup.ALLOWED);
        ArrowDamage.Context context = new ArrowDamage.Context(data, getOwner(), 0);
        context.direct = arrow;
        context.vanillaCrit = isCritArrow() ? 1.5f : 1f;
        if (target != null) ArrowEffects.beforeHit(arrow, target);
        ArrowDamage.with(context, () -> {
            super.onHitEntity(hit);
            return null;
        });
        if (target != null) ArrowEffects.hit(arrow, target, context, direction);
        if (!state.tracing && state.ricochets < state.profile.ricochets()) {
            // An immune target reverses vanilla arrows; ricochets retain their incoming speed.
            setDeltaMovement(incoming);
            if (!ArrowFlight.ricochet(arrow, hit.getLocation())) discard();
        }
        ci.cancel();
    }

    @Inject(method = "onHitBlock", at = @At("HEAD"), cancellable = true)
    private void bertie$blockHit(BlockHitResult hit, CallbackInfo ci) {
        if (!ArrowProfile.redesigned(bertie$self().getCustomProperties())) return;
        bertie$impact = hit.getLocation();
        if (!ArrowFlight.block(bertie$self(), hit)) super.onHitBlock(hit);
        ci.cancel();
    }

    @Inject(method = "getWaterInertia", at = @At("HEAD"), cancellable = true)
    private void bertie$water(CallbackInfoReturnable<Float> cir) {
        if (ArrowProfile.redesigned(bertie$self().getCustomProperties()))
            cir.setReturnValue(bertie$flight().profile.water() ? 1f : super.getWaterInertia());
    }
}
