package com.berlord.rusticengineerfix.mixin;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.LevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Bug #2: the airship flight procedure turned by doing setYRot(yaw + step), but then
 * overwrote the "previous" rotation fields each tick:
 *   yRotO = yRot;  yBodyRotO = yRot;  yHeadRotO = yRot;
 * The renderer interpolates rotation as lerp(partialTick, yRotO, yRot). Forcing the
 * old value equal to the new value leaves nothing to interpolate, so the model snaps
 * to the new angle 20x/second -> choppy turning.
 *
 * Fix: capture the yaw at the start of the tick (the genuine previous yaw) and, after
 * the procedure runs, restore yRotO/yBodyRotO/yHeadRotO to it. The renderer then
 * interpolates old->new yaw smoothly across frames.
 */
@Mixin(targets = "net.mcreator.rusticengineer.procedures.AirshipFlyingProcedure", remap = false)
public class AirshipFlyingProcedureMixin {

    private static float rusticengineerfix$prevYaw;

    @Inject(
            method = "execute(Lnet/minecraft/world/level/LevelAccessor;DDDLnet/minecraft/world/entity/Entity;)V",
            at = @At("HEAD"),
            remap = false
    )
    private static void rusticengineerfix$captureYaw(LevelAccessor world, double x, double y, double z, Entity entity, CallbackInfo ci) {
        if (entity != null) {
            rusticengineerfix$prevYaw = entity.getYRot();
        }
    }

    @Inject(
            method = "execute(Lnet/minecraft/world/level/LevelAccessor;DDDLnet/minecraft/world/entity/Entity;)V",
            at = @At("RETURN"),
            remap = false
    )
    private static void rusticengineerfix$restoreOldYaw(LevelAccessor world, double x, double y, double z, Entity entity, CallbackInfo ci) {
        if (entity == null) {
            return;
        }
        // Only intervene when the yaw actually changed this tick (a real turn). When the
        // airship is idle / sitting on the ground the yaw is unchanged, so we leave the
        // rotation fields alone — otherwise clobbering yRotO every tick fights the client's
        // idle interpolation and makes the model wobble ~1x/tick on the ground.
        if (entity.getYRot() == rusticengineerfix$prevYaw) {
            return;
        }
        entity.yRotO = rusticengineerfix$prevYaw;
        if (entity instanceof LivingEntity le) {
            le.yBodyRotO = rusticengineerfix$prevYaw;
            le.yHeadRotO = rusticengineerfix$prevYaw;
        }
    }
}
