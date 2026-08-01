package io.github.bertie_mc.hephaestusarchitecture.mixin;

import io.github.bertie_mc.hephaestusarchitecture.structure.PedestalRouter;
import com.stal111.forbidden_arcanus.common.block.pedestal.effect.UpdateForgeIngredientsEffect;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(UpdateForgeIngredientsEffect.class)
public abstract class UpdateForgeIngredientsEffectMixin {

    @Inject(method = "execute", at = @At("HEAD"), cancellable = true, remap = false)
    private void hephaestusarchitecture$routeByTierLayout(ServerLevel level,
                                                          BlockPos pos,
                                                          ItemStack stack,
                                                          CallbackInfo ci) {
        PedestalRouter.route(level, pos, stack);
        ci.cancel();
    }
}
