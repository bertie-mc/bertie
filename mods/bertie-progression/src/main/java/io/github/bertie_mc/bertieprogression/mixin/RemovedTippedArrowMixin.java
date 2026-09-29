package io.github.bertie_mc.bertieprogression.mixin;

import io.github.bertie_mc.bertieprogression.RemovedItems;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.TippedArrowRecipe;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(TippedArrowRecipe.class)
public abstract class RemovedTippedArrowMixin {
    @Inject(
            method = "matches(Lnet/minecraft/world/item/crafting/CraftingInput;Lnet/minecraft/world/level/Level;)Z",
            at = @At("HEAD"),
            cancellable = true,
            require = 1)
    private void bertie$noRemovedArrowPayload(CraftingInput input, Level level, CallbackInfoReturnable<Boolean> cir) {
        if (input.items().stream().anyMatch(RemovedItems::isRemovedPotion)) cir.setReturnValue(false);
    }
}
