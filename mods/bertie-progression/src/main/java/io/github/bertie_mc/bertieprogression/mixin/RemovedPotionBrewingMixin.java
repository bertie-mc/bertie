package io.github.bertie_mc.bertieprogression.mixin;

import io.github.bertie_mc.bertieprogression.RemovedItems;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.PotionBrewing;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PotionBrewing.class)
public abstract class RemovedPotionBrewingMixin {
    @Inject(method = "hasMix", at = @At("HEAD"), cancellable = true, require = 1)
    private void bertie$noLegacyPotionConversions(
            ItemStack input, ItemStack reagent, CallbackInfoReturnable<Boolean> cir) {
        if (RemovedItems.isRemovedPotion(input)) cir.setReturnValue(false);
    }
}
