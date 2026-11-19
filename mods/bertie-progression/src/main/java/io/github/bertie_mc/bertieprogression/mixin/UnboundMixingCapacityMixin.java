package io.github.bertie_mc.bertieprogression.mixin;

import com.simibubi.create.content.kinetics.mixer.MixingRecipe;
import net.minecraft.core.registries.BuiltInRegistries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = MixingRecipe.class, remap = false)
public abstract class UnboundMixingCapacityMixin {
    @Inject(method = "getMaxInputCount", at = @At("HEAD"), cancellable = true, require = 1)
    private void bertie$batchMixing(CallbackInfoReturnable<Integer> cir) {
        var results = ((MixingRecipe) (Object) this).getRollableResults();
        if (results.size() != 1) return;
        var id = BuiltInRegistries.ITEM
                .getKey(results.getFirst().getStack().getItem())
                .toString();
        if (id.equals("unbound:glass_blend") || id.equals("unbound:lens_blank")) cir.setReturnValue(24);
    }
}
