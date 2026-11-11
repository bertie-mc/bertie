package io.github.bertie_mc.bertieprogression.mixin;

import earth.terrarium.pastel.api.energy.color.InkColor;
import io.github.bertie_mc.bertieprogression.pastel.DyeFuel;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "earth.terrarium.pastel.api.energy.InkPowered", remap = false)
public interface PastelInkPoweredMixin {
    @Inject(method = "canUse(Lnet/minecraft/world/entity/player/Player;)Z", at = @At("HEAD"), cancellable = true)
    private static void bertie$allowDyeFuel(Player player, CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(player != null);
    }

    @Inject(
            method =
                    "tryDrainEnergy(Lnet/minecraft/world/entity/player/Player;Learth/terrarium/pastel/api/energy/color/InkColor;J)Z",
            at = @At("HEAD"),
            cancellable = true)
    private static void bertie$payWithDyes(
            Player player, InkColor color, long amount, CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(DyeFuel.drain(player, color.getDyeColor().orElse(null), amount));
    }

    @Inject(method = "getAvailableInk", at = @At("HEAD"), cancellable = true)
    private static void bertie$countDyes(Player player, InkColor color, CallbackInfoReturnable<Long> cir) {
        cir.setReturnValue(DyeFuel.available(player, color.getDyeColor().orElse(null)));
    }

    @Inject(
            method =
                    "hasAvailableInk(Lnet/minecraft/world/entity/player/Player;Learth/terrarium/pastel/api/energy/color/InkColor;J)Z",
            at = @At("HEAD"),
            cancellable = true)
    private static void bertie$checkDyes(
            Player player, InkColor color, long amount, CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(DyeFuel.available(player, color.getDyeColor().orElse(null)) >= amount);
    }
}
