package io.github.bertie_mc.bertieprogression.mixin;

import net.minecraft.core.Holder;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionBrewing;
import net.minecraft.world.item.alchemy.Potions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * The pack has no Mundane Potion. Every mix that would brew one is left out of the brewing table,
 * including the water mix each starting reagent adds, and recipe viewers and Create's mixer read
 * their potion recipes from the same builder.
 */
@Mixin(value = PotionBrewing.Builder.class, remap = false)
public abstract class PotionBrewingBuilderMixin {
    @Inject(method = "addMix", at = @At("HEAD"), cancellable = true, require = 1)
    private void bertie$noMundane(Holder<Potion> input, Item reagent, Holder<Potion> result, CallbackInfo ci) {
        if (result.value() == Potions.MUNDANE.value()) {
            ci.cancel();
        }
    }
}
