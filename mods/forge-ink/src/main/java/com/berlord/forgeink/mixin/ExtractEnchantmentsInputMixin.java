package com.berlord.forgeink.mixin;

import com.stal111.forbidden_arcanus.common.block.entity.forge.essence.EssenceType;
import com.stal111.forbidden_arcanus.common.block.entity.forge.input.ExtractEnchantmentsInput;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Disenchanting no longer generates essence: the forge runs on ink now, and
 * enchanted gear is not ink. Disabling the input at the source also covers the
 * Essence Utrem Jar right-click path, not just the forge slot.
 */
@Mixin(ExtractEnchantmentsInput.class)
public abstract class ExtractEnchantmentsInputMixin {

    @Inject(method = "canInput", at = @At("HEAD"), cancellable = true, remap = false)
    private void forgeink$disable(EssenceType type, ItemStack stack,
                                  CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(false);
    }
}
