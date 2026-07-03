package com.berlord.rusticengineerfix.mixin;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Dragonfly fixes.
 *
 * #1 (original): forward (W) thrust set Y = pitch * -0.03, so looking down made it sink.
 *    We zero that multiplier so W flies level; vertical stays on space/shift.
 *
 * #2 (new): the original could not do forward + up/down at the same time — the forward
 *    block jumps past the vertical (NBT "MoveY") block, and setDeltaMovement overwrites.
 *    We re-apply MoveY as the Y component at the end of the tick, so forward and vertical
 *    combine. When forward isn't pressed the value is identical (no change); when no
 *    vertical key is held MoveY is 0 and we leave idle/fall behaviour alone.
 */
@Mixin(targets = "net.mcreator.rusticengineer.procedures.DragonflyFlyingProcedure", remap = false)
public class DragonflyFlyingProcedureMixin {

    @ModifyConstant(
            method = "execute(Lnet/minecraft/world/level/LevelAccessor;DDDLnet/minecraft/world/entity/Entity;)V",
            constant = @Constant(doubleValue = -0.03D),
            remap = false
    )
    private static double rusticengineerfix$levelForwardFlight(double original) {
        return 0.0D;
    }

    @Inject(
            method = "execute(Lnet/minecraft/world/level/LevelAccessor;DDDLnet/minecraft/world/entity/Entity;)V",
            at = @At("RETURN"),
            remap = false
    )
    private static void rusticengineerfix$combineVertical(LevelAccessor world, double x, double y, double z, Entity entity, CallbackInfo ci) {
        if (entity == null || !entity.getPersistentData().getBoolean("Montado")) {
            return;
        }
        double moveY = entity.getPersistentData().getDouble("MoveY");
        if (moveY != 0.0D) {
            Vec3 d = entity.getDeltaMovement();
            entity.setDeltaMovement(d.x, moveY, d.z);
        }
    }
}
