package io.github.bertie_mc.testing.client.driver.mixin.screenshot;

import io.github.bertie_mc.testing.client.driver.screenshot.ClientTestScreenshots;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Completes pending client-test screenshot requests after a frame is rendered. */
@Mixin(Minecraft.class)
abstract class MinecraftMixin {
    @Inject(
            method = "runTick",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/client/renderer/GameRenderer;render(Lnet/minecraft/client/DeltaTracker;Z)V",
                            shift = At.Shift.AFTER))
    private void bertie$captureScreenshotAfterRender(boolean renderLevel, CallbackInfo callback) {
        ClientTestScreenshots.afterRender((Minecraft) (Object) this);
    }
}
