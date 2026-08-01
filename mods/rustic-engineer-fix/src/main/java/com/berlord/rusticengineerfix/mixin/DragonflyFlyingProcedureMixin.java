package com.berlord.rusticengineerfix.mixin;

import com.berlord.rusticengineerfix.logic.FlightAdjustmentPolicy;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Keeps forward flight level and composes it with the rider's vertical input. */
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
        if (entity == null) {
            return;
        }
        double moveY = entity.getPersistentData().getDouble("MoveY");
        if (FlightAdjustmentPolicy.shouldApplyVertical(
                entity.getPersistentData().getBoolean("Montado"), moveY)) {
            Vec3 d = entity.getDeltaMovement();
            entity.setDeltaMovement(d.x, moveY, d.z);
        }
    }
}
