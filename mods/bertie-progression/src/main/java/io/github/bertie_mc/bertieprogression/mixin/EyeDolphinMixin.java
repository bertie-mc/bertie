package io.github.bertie_mc.bertieprogression.mixin;

import io.github.bertie_mc.bertieprogression.ender.EyeAcquisitions;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.animal.Dolphin;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Dolphin.class)
public abstract class EyeDolphinMixin {
    @Unique
    private boolean bertie$tropicalFish;

    @Inject(method = "mobInteract", at = @At("HEAD"))
    private void bertie$captureFish(
            Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        bertie$tropicalFish = player.getItemInHand(hand).is(Items.TROPICAL_FISH);
    }

    @Inject(method = "mobInteract", at = @At("RETURN"))
    private void bertie$exoticEye(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        if (bertie$tropicalFish && cir.getReturnValue().consumesAction()) {
            EyeAcquisitions.dolphinFed((Dolphin) (Object) this);
        }
        bertie$tropicalFish = false;
    }
}
