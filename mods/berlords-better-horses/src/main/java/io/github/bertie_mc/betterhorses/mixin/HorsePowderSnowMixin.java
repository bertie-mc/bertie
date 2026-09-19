package io.github.bertie_mc.betterhorses.mixin;

import io.github.bertie_mc.betterhorses.HorseEquipment;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.horse.Horse;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.PowderSnowBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PowderSnowBlock.class)
public abstract class HorsePowderSnowMixin {
    @Inject(method = "canEntityWalkOnPowderSnow", at = @At("HEAD"), cancellable = true)
    private static void betterhorses$walk(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        if (entity instanceof Horse
                && ((HorseEquipment) entity).betterhorses$tier().waterWalking()) cir.setReturnValue(true);
    }

    @Inject(method = "getCollisionShape", at = @At("HEAD"), cancellable = true)
    private void betterhorses$surface(
            BlockState state,
            BlockGetter level,
            BlockPos pos,
            CollisionContext context,
            CallbackInfoReturnable<VoxelShape> cir) {
        if (context instanceof EntityCollisionContext collision
                && collision.getEntity() instanceof Horse horse
                && ((HorseEquipment) horse).betterhorses$tier().waterWalking()
                && context.isAbove(Shapes.block(), pos, false)) cir.setReturnValue(Shapes.block());
    }
}
