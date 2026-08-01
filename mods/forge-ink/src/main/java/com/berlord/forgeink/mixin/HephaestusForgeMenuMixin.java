package com.berlord.forgeink.mixin;

import com.berlord.forgeink.ForgeInk;
import com.stal111.forbidden_arcanus.common.block.entity.forge.essence.EssenceType;
import com.stal111.forbidden_arcanus.common.inventory.HephaestusForgeMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Same gate as {@link HephaestusForgeBlockEntityMixin}, applied to the menu's own
 * validation path (client-side slot checks and shift-click routing), so wrong-tier
 * inks are rejected consistently on both sides without ghost-stack flicker.
 */
@Mixin(HephaestusForgeMenu.class)
public abstract class HephaestusForgeMenuMixin {

    @Inject(method = "canInput", at = @At("HEAD"), cancellable = true, remap = false)
    private void forgeink$onlyMatchingInk(Level level, EssenceType type, ItemStack stack,
                                          CallbackInfoReturnable<Boolean> cir) {
        HephaestusForgeMenu self = (HephaestusForgeMenu) (Object) this;
        if (type == EssenceType.EXPERIENCE && !ForgeInk.allowExperienceInput(stack, self.getLevel())) {
            cir.setReturnValue(false);
        }
    }
}
