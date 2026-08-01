package com.berlord.forgeink.mixin;

import com.berlord.forgeink.ForgeInk;
import com.stal111.forbidden_arcanus.common.block.entity.forge.essence.EssenceType;
import com.stal111.forbidden_arcanus.common.block.entity.forge.input.EssenceDataInput;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * The generic essence-component input: as far as EXPERIENCE ("Ink") is concerned,
 * only actual inks count — even if something else still carries an EXPERIENCE
 * essence component (Xpetrified Orb keeps its for the right-click XP redeem).
 * This covers every consumer of the input registry: the forge itself and the
 * Essence Utrem Jars.
 */
@Mixin(EssenceDataInput.class)
public abstract class EssenceDataInputMixin {

    @Inject(method = "canInput", at = @At("HEAD"), cancellable = true, remap = false)
    private void forgeink$onlyInkForExperience(EssenceType type, ItemStack stack,
                                               CallbackInfoReturnable<Boolean> cir) {
        if (type == EssenceType.EXPERIENCE && ForgeInk.inkTier(stack.getItem()) == -1) {
            cir.setReturnValue(false);
        }
    }
}
