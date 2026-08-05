package io.github.bertie_mc.testing.client.driver.mixin;

import com.mojang.blaze3d.platform.InputConstants;
import io.github.bertie_mc.testing.client.driver.DefaultTestInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(InputConstants.class)
public abstract class InputConstantsMixin {
    @Inject(method = "isKeyDown", at = @At("HEAD"), cancellable = true)
    private static void bertie$includeSimulatedKeys(
            long window, int keyCode, CallbackInfoReturnable<Boolean> result) {
        if (DefaultTestInput.isKeyDown(keyCode)) {
            result.setReturnValue(true);
        }
    }
}
