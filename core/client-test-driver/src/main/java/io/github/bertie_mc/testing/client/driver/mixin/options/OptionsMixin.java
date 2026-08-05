package io.github.bertie_mc.testing.client.driver.mixin.options;

import io.github.bertie_mc.testing.client.driver.ClientTestGameOptions;
import net.minecraft.client.Options;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Applies and captures deterministic client-test options after vanilla initialization. */
@Mixin(Options.class)
abstract class OptionsMixin {
    @Inject(method = "<init>", at = @At("RETURN"))
    private void bertie$initializeClientTestOptions(CallbackInfo callback) {
        ClientTestGameOptions.initialize((Options) (Object) this);
    }
}
