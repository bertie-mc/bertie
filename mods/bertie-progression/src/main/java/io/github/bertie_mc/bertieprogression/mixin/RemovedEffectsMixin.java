package io.github.bertie_mc.bertieprogression.mixin;

import io.github.bertie_mc.bertieprogression.RemovedItems;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class RemovedEffectsMixin {
    @Shadow
    protected abstract void onEffectRemoved(MobEffectInstance effect);

    @Inject(
            method = "addEffect(Lnet/minecraft/world/effect/MobEffectInstance;Lnet/minecraft/world/entity/Entity;)Z",
            at = @At("HEAD"),
            cancellable = true,
            require = 1)
    private void bertie$rejectRemovedEffect(
            MobEffectInstance effect, Entity source, CallbackInfoReturnable<Boolean> cir) {
        if (RemovedItems.isRemovedEffect(effect.getEffect())) cir.setReturnValue(false);
    }

    @Inject(method = "forceAddEffect", at = @At("HEAD"), cancellable = true, require = 1)
    private void bertie$rejectForcedRemovedEffect(MobEffectInstance effect, Entity source, CallbackInfo ci) {
        if (RemovedItems.isRemovedEffect(effect.getEffect())) ci.cancel();
    }

    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"), require = 1)
    private void bertie$removeSavedEffects(CompoundTag tag, CallbackInfo ci) {
        LivingEntity entity = (LivingEntity) (Object) this;
        for (MobEffectInstance effect : List.copyOf(entity.getActiveEffects())) {
            if (RemovedItems.isRemovedEffect(effect.getEffect())) {
                entity.removeEffectNoUpdate(effect.getEffect());
                onEffectRemoved(effect);
            }
        }
        // Older forced-removal paths could leave a modifier after discarding the effect itself.
        for (var id : RemovedItems.removedEffectIds()) {
            BuiltInRegistries.MOB_EFFECT
                    .getHolder(id)
                    .ifPresent(effect -> effect.value().removeAttributeModifiers(entity.getAttributes()));
        }
    }
}
