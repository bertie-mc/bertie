package com.berlord.bushtweaks.mixin;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.SweetBerryBushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Vanilla sweet berry bushes: crouching skips the in-bush slowdown.
 * Sneak Through Berries already removes the damage while crouching but its
 * injection point is after makeStuckInBlock, so the slow stays — this fixes
 * that half. (Redirect would conflict with another mod redirecting the same
 * call; nothing in the pack does — STB uses @Inject.)
 */
@Mixin(SweetBerryBushBlock.class)
public class SweetBerryBushBlockMixin {

    @Redirect(
            method = "entityInside",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/Entity;makeStuckInBlock(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/phys/Vec3;)V"
            )
    )
    private void bushtweaks$noSlowdownWhileCrouching(Entity entity, BlockState state, Vec3 multiplier) {
        if (entity.isSteppingCarefully()) {
            // keep the fall-damage cushion the slowdown would have given
            entity.resetFallDistance();
            return;
        }
        entity.makeStuckInBlock(state, multiplier);
    }
}
