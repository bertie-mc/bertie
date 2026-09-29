package io.github.bertie_mc.fletching.mixin;

import com.fletchery.mod.entity.CustomArrowEntity;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.PotionContents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = CustomArrowEntity.class, remap = false)
public abstract class CoatedArrowMixin implements io.github.bertie_mc.fletching.ArrowRuntime {
    @Unique
    private static final EntityDataAccessor<CompoundTag> BERTIE_COATING =
            SynchedEntityData.defineId(CustomArrowEntity.class, EntityDataSerializers.COMPOUND_TAG);

    @Shadow
    private CompoundTag customProperties;

    @Inject(method = "defineSynchedData", at = @At("TAIL"))
    private void bertie$defineCoating(SynchedEntityData.Builder builder, CallbackInfo ci) {
        builder.define(BERTIE_COATING, new CompoundTag());
    }

    @Inject(
            method = {"setCustomProperties", "readAdditionalSaveData"},
            at = @At("TAIL"))
    private void bertie$syncCoating(CompoundTag ignored, CallbackInfo ci) {
        CustomArrowEntity arrow = (CustomArrowEntity) (Object) this;
        if (!arrow.level().isClientSide) arrow.getEntityData().set(BERTIE_COATING, customProperties.copy());
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void bertie$readCoating(CallbackInfo ci) {
        bertie$syncProperties();
    }

    @Override
    public void bertie$syncProperties() {
        CustomArrowEntity arrow = (CustomArrowEntity) (Object) this;
        if (arrow.level().isClientSide) {
            CompoundTag tag = arrow.getEntityData().get(BERTIE_COATING);
            if (!tag.isEmpty() && !tag.equals(customProperties)) arrow.setCustomProperties(tag);
        }
    }

    @Inject(method = "applyPotionEffects", at = @At("HEAD"), cancellable = true)
    private void bertie$applyCoating(LivingEntity target, CallbackInfo ci) {
        if (!customProperties.getBoolean("bertieCoating")) return;
        CustomArrowEntity arrow = (CustomArrowEntity) (Object) this;
        Entity owner = arrow.getOwner();
        ListTag effects = customProperties.getList("potionEffects", 10);
        for (int i = 0; i < effects.size(); i++) {
            CompoundTag tag = effects.getCompound(i);
            ResourceLocation id = ResourceLocation.tryParse(tag.getString("id"));
            if (id == null) continue;
            Holder<MobEffect> effect =
                    BuiltInRegistries.MOB_EFFECT.getHolder(id).orElse(null);
            if (effect == null) continue;
            int amplifier = tag.getInt("amplifier");
            if (effect.value().isInstantenous())
                effect.value().applyInstantenousEffect(arrow, owner, target, amplifier, 1.0);
            else {
                int duration = tag.getInt("duration");
                target.addEffect(
                        new MobEffectInstance(effect, duration == -1 ? -1 : Math.max(1, duration / 8), amplifier),
                        owner);
            }
        }
        ci.cancel();
    }

    @Inject(method = "getPickupItem", at = @At("RETURN"))
    private void bertie$preserveCoating(CallbackInfoReturnable<ItemStack> cir) {
        if (!customProperties.contains("bertiePotionContents")) return;
        CustomArrowEntity arrow = (CustomArrowEntity) (Object) this;
        PotionContents.CODEC
                .parse(
                        RegistryOps.create(NbtOps.INSTANCE, arrow.registryAccess()),
                        customProperties.get("bertiePotionContents"))
                .result()
                .ifPresent(contents -> cir.getReturnValue().set(DataComponents.POTION_CONTENTS, contents));
    }
}
