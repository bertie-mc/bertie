package com.berlord.hephaestusarchitecture.mixin;

import com.stal111.forbidden_arcanus.common.block.HephaestusForgeBlock;
import com.stal111.forbidden_arcanus.common.block.entity.forge.ritual.result.UpgradeTierResult;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(UpgradeTierResult.class)
public abstract class UpgradeTierResultMixin {

    @Inject(method = "executeLevelEffect", at = @At("TAIL"), remap = false)
    private void hephaestusarchitecture$validateUpgradedLayout(Level level,
                                                               BlockPos pos,
                                                               CallbackInfo ci) {
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof HephaestusForgeBlock forgeBlock) {
            forgeBlock.updateState(state, level, pos);
        }
    }
}
