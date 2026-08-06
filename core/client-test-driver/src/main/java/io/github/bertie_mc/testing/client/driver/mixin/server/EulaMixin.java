package io.github.bertie_mc.testing.client.driver.mixin.server;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import io.github.bertie_mc.testing.client.driver.server.InProcessDedicatedServer;
import net.minecraft.server.Eula;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Makes owned in-process test servers pass EULA checks without modifying the EULA file. */
@Mixin(Eula.class)
abstract class EulaMixin {
    @ModifyReturnValue(method = "hasAgreedToEULA", at = @At("RETURN"))
    private boolean bertie$acceptEulaForOwnedClientTestServer(boolean agreed) {
        return agreed || InProcessDedicatedServer.ownsCurrentServerLifecycle();
    }
}
