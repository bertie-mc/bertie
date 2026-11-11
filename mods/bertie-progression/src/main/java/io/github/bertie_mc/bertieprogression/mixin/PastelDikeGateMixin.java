package io.github.bertie_mc.bertieprogression.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "earth.terrarium.pastel.blocks.structure.DikeGateBlock", remap = false)
public abstract class PastelDikeGateMixin {
    @Inject(method = "getCollisionShape", at = @At("HEAD"), cancellable = true, require = 1)
    private void bertie$openOverworldRuinGate(
            BlockState state,
            BlockGetter world,
            BlockPos pos,
            CollisionContext context,
            CallbackInfoReturnable<VoxelShape> cir) {
        if (world instanceof Level level && level.dimension() == Level.OVERWORLD) {
            cir.setReturnValue(Shapes.empty());
        }
    }

    @Inject(method = "punishEntityWithoutAzureDike", at = @At("HEAD"), cancellable = true, require = 1)
    private void bertie$skipRetiredChargeDamage(
            Level world, BlockPos pos, Entity entity, boolean decreasedSounds, CallbackInfo ci) {
        if (world.dimension() == Level.OVERWORLD) {
            ci.cancel();
        }
    }
}
