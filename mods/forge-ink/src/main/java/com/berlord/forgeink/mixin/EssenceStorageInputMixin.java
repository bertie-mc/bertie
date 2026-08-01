package com.berlord.forgeink.mixin;

import com.stal111.forbidden_arcanus.common.block.entity.forge.essence.EssenceType;
import com.stal111.forbidden_arcanus.common.block.entity.forge.input.EssenceStorageInput;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Items with a stored-essence component (pre-filled Essence Utrem Jar items —
 * creative only in stock FA) may no longer pour EXPERIENCE ("Ink") essence.
 * Aureal bottles and other essence types are untouched.
 */
@Mixin(EssenceStorageInput.class)
public abstract class EssenceStorageInputMixin {

    @Inject(method = "canInput", at = @At("HEAD"), cancellable = true, remap = false)
    private void forgeink$noStoredExperience(EssenceType type, ItemStack stack,
                                             CallbackInfoReturnable<Boolean> cir) {
        if (type == EssenceType.EXPERIENCE) {
            cir.setReturnValue(false);
        }
    }
}
