package io.github.bertie_mc.bertieprogression.mixin;

import io.github.bertie_mc.bertieprogression.ModAttachments;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "net.pocketdimension.network.OpenKeyMessage", remap = false)
public abstract class PocketOpenKeyMixin {
    @Inject(method = "pressAction", at = @At("HEAD"), cancellable = true, require = 1)
    private static void bertieprogression$requireEssence(Player player, int eventType, int pressedMs, CallbackInfo ci) {
        // The server sends the original animation packets after accepting a key press.
        // Suppress client prediction so locked attempts also produce no local effects or text.
        if (player.level().isClientSide || !player.getData(ModAttachments.POCKET_UNLOCKED)) {
            ci.cancel();
        }
    }
}
