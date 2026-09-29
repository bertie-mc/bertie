package io.github.bertie_mc.bertieprogression.mixin.combat;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import io.github.bertie_mc.bertieprogression.combat.DamageFamily;
import io.github.bertie_mc.bertieprogression.combat.DamageRedirectContext;
import io.github.bertie_mc.bertieprogression.combat.DamageRoutes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "com.sammy.malum.common.worldevent.DelayedDamageWorldEvent", remap = false)
public abstract class DelayedReflectionFamilyMixin {
    @Unique
    private DamageRoutes.Snapshot bertie$origin;

    @Inject(method = "setMagicDamageType", at = @At("TAIL"), require = 1)
    private void bertie$rememberOriginal(ResourceKey<DamageType> type, CallbackInfoReturnable<Object> cir) {
        DamageSource original = DamageRedirectContext.current();
        if (original != null) bertie$origin = DamageRoutes.snapshot(original);
    }

    @ModifyExpressionValue(
            method = "tick",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lteam/lodestar/lodestone/helpers/DamageTypeHelper;create(Lnet/minecraft/world/level/Level;Lnet/minecraft/resources/ResourceKey;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/entity/Entity;)Lnet/minecraft/world/damagesource/DamageSource;"),
            require = 1)
    private DamageSource bertie$originalFamily(DamageSource created) {
        return bertie$origin == null ? created : DamageRoutes.reflect(bertie$origin, created, created.getEntity());
    }

    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"), require = 1)
    private void bertie$saveOrigin(CompoundTag tag, CallbackInfo ci) {
        if (bertie$origin == null) return;
        tag.putString("BertieDamageFamily", bertie$origin.family().name());
        tag.putInt("BertieDamageProperties", bertie$origin.properties());
        if (bertie$origin.school() != null)
            tag.putString("BertieDamageSchool", bertie$origin.school().toString());
    }

    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"), require = 1)
    private void bertie$readOrigin(CompoundTag tag, CallbackInfo ci) {
        if (!tag.contains("BertieDamageFamily")) return;
        try {
            bertie$origin = new DamageRoutes.Snapshot(
                    DamageFamily.valueOf(tag.getString("BertieDamageFamily")),
                    tag.getInt("BertieDamageProperties") & 127,
                    ResourceLocation.tryParse(tag.getString("BertieDamageSchool")));
        } catch (IllegalArgumentException ignored) {
            bertie$origin = null;
        }
    }
}
