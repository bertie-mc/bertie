package io.github.bertie_mc.bertieprogression.mixin;

import io.github.bertie_mc.bertieprogression.warden.WardenState;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.monster.warden.Warden;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Warden.class)
public abstract class WardenStateMixin implements WardenState {
    @Unique
    private static final EntityDataAccessor<Boolean> BERTIE_SHEARED =
            SynchedEntityData.defineId(Warden.class, EntityDataSerializers.BOOLEAN);

    @Unique
    private long bertie$retaliationReadyAt;

    @Inject(method = "defineSynchedData", at = @At("TAIL"))
    private void bertie$define(SynchedEntityData.Builder builder, CallbackInfo ci) {
        builder.define(BERTIE_SHEARED, false);
    }

    @Override
    public boolean bertie$tendrilsSheared() {
        return ((Warden) (Object) this).getEntityData().get(BERTIE_SHEARED);
    }

    @Override
    public void bertie$setTendrilsSheared(boolean value) {
        ((Warden) (Object) this).getEntityData().set(BERTIE_SHEARED, value);
    }

    @Override
    public long bertie$retaliationReadyAt() {
        return bertie$retaliationReadyAt;
    }

    @Override
    public void bertie$setRetaliationReadyAt(long tick) {
        bertie$retaliationReadyAt = tick;
    }

    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    private void bertie$save(CompoundTag tag, CallbackInfo ci) {
        tag.putBoolean("bertieprogression:tendrils_sheared", bertie$tendrilsSheared());
        tag.putLong("bertieprogression:retaliation_ready_at", bertie$retaliationReadyAt);
    }

    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void bertie$load(CompoundTag tag, CallbackInfo ci) {
        bertie$setTendrilsSheared(tag.getBoolean("bertieprogression:tendrils_sheared"));
        bertie$retaliationReadyAt = tag.getLong("bertieprogression:retaliation_ready_at");
    }
}
