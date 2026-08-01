package com.berlord.rusticengineerfix.mixin;

import com.berlord.rusticengineerfix.logic.FlightAdjustmentPolicy;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.LevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Preserves the pre-turn yaw so the renderer can interpolate instead of snapping. */
@Mixin(targets = "net.mcreator.rusticengineer.procedures.AirshipFlyingProcedure", remap = false)
public class AirshipFlyingProcedureMixin {

    @Unique
    private static final ThreadLocal<Float> rusticengineerfix$prevYaw = new ThreadLocal<>();

    @Inject(
            method = "execute(Lnet/minecraft/world/level/LevelAccessor;DDDLnet/minecraft/world/entity/Entity;)V",
            at = @At("HEAD"),
            remap = false
    )
    private static void rusticengineerfix$captureYaw(LevelAccessor world, double x, double y, double z, Entity entity, CallbackInfo ci) {
        if (entity != null) {
            rusticengineerfix$prevYaw.set(entity.getYRot());
        } else {
            rusticengineerfix$prevYaw.remove();
        }
    }

    @Inject(
            method = "execute(Lnet/minecraft/world/level/LevelAccessor;DDDLnet/minecraft/world/entity/Entity;)V",
            at = @At("RETURN"),
            remap = false
    )
    private static void rusticengineerfix$restoreOldYaw(LevelAccessor world, double x, double y, double z, Entity entity, CallbackInfo ci) {
        Float previousYaw = rusticengineerfix$prevYaw.get();
        rusticengineerfix$prevYaw.remove();
        if (entity == null || previousYaw == null) {
            return;
        }
        if (!FlightAdjustmentPolicy.yawChanged(previousYaw, entity.getYRot())) {
            return;
        }
        entity.yRotO = previousYaw;
        if (entity instanceof LivingEntity le) {
            le.yBodyRotO = previousYaw;
            le.yHeadRotO = previousYaw;
        }
    }
}
