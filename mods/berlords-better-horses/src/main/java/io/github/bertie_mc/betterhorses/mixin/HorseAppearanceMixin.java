package io.github.bertie_mc.betterhorses.mixin;

import io.github.bertie_mc.betterhorses.HorseAppearance;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.*;
import net.minecraft.world.entity.animal.horse.Horse;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Horse.class)
public abstract class HorseAppearanceMixin implements HorseAppearance {
    @Unique
    private static final EntityDataAccessor<Integer> betterhorses$APPEARANCE =
            SynchedEntityData.defineId(Horse.class, EntityDataSerializers.INT);

    @Inject(method = "defineSynchedData", at = @At("TAIL"))
    private void betterhorses$defineAppearance(SynchedEntityData.Builder builder, CallbackInfo ci) {
        builder.define(betterhorses$APPEARANCE, Style.NORMAL.ordinal());
    }

    @Override
    public Style betterhorses$appearance() {
        return Style.byId(((Horse) (Object) this).getEntityData().get(betterhorses$APPEARANCE));
    }

    @Override
    public void betterhorses$setAppearance(Style appearance) {
        ((Horse) (Object) this).getEntityData().set(betterhorses$APPEARANCE, appearance.ordinal());
    }

    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    private void betterhorses$saveAppearance(CompoundTag tag, CallbackInfo ci) {
        tag.putInt("BetterHorsesAppearance", betterhorses$appearance().ordinal());
    }

    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void betterhorses$loadAppearance(CompoundTag tag, CallbackInfo ci) {
        betterhorses$setAppearance(Style.byId(tag.getInt("BetterHorsesAppearance")));
    }
}
