package io.github.bertie_mc.testing.client.driver.mixin.server;

import io.github.bertie_mc.testing.client.driver.server.InProcessDedicatedServer;
import net.minecraft.Util;
import net.minecraft.server.dedicated.DedicatedServer;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.IEventBus;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Keeps process-wide client services alive when an in-process server stops. */
@Mixin(DedicatedServer.class)
abstract class DedicatedServerMixin {
    @Inject(
            method = "initServer",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/server/dedicated/DedicatedServer;loadLevel()V"))
    private void bertie$captureReadyServer(CallbackInfoReturnable<Boolean> callback) {
        InProcessDedicatedServer.onServerReadyToLoad((DedicatedServer) (Object) this);
    }

    @Redirect(method = "initServer", at = @At(value = "INVOKE", target = "Ljava/lang/Thread;start()V", ordinal = 0))
    private void bertie$doNotStartConsoleHandler(Thread thread) {
        if (!InProcessDedicatedServer.ownsCurrentServerLifecycle()) {
            thread.start();
        }
    }

    @Redirect(
            method = "stopServer",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/neoforged/bus/api/IEventBus;post(Lnet/neoforged/bus/api/Event;)Lnet/neoforged/bus/api/Event;"))
    private Event bertie$doNotPostPhysicalShutdown(IEventBus eventBus, Event event) {
        if (InProcessDedicatedServer.ownsCurrentServerLifecycle()) {
            return event;
        }
        return eventBus.post(event);
    }

    @Redirect(method = "stopServer", at = @At(value = "INVOKE", target = "Lnet/minecraft/Util;shutdownExecutors()V"))
    private void bertie$doNotStopClientExecutors() {
        if (!InProcessDedicatedServer.ownsCurrentServerLifecycle()) {
            Util.shutdownExecutors();
        }
    }
}
