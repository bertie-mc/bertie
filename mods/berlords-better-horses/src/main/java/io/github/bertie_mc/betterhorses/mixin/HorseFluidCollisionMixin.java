package io.github.bertie_mc.betterhorses.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.animal.horse.Horse;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LiquidBlock.class)
public abstract class HorseFluidCollisionMixin {
    @Inject(method = "getCollisionShape", at = @At("HEAD"), cancellable = true)
    private void betterhorses$surface(
            BlockState state,
            BlockGetter level,
            BlockPos pos,
            CollisionContext context,
            CallbackInfoReturnable<VoxelShape> cir) {
        if (context instanceof EntityCollisionContext entityContext
                && entityContext.getEntity() instanceof Horse horse
                && horse.canStandOnFluid(state.getFluidState())) {
            // Vanilla's strider collision is only half a block high, below the visible surface.
            VoxelShape surface = Shapes.box(0, 0, 0, 1, state.getFluidState().getHeight(level, pos), 1);
            cir.setReturnValue(
                    context.isAbove(surface, pos, true)
                                    && context.canStandOnFluid(level.getFluidState(pos.above()), state.getFluidState())
                            ? surface
                            : Shapes.empty());
        }
    }
}
