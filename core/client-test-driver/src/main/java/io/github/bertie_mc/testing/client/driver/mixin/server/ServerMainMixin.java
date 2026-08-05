package io.github.bertie_mc.testing.client.driver.mixin.server;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import io.github.bertie_mc.testing.client.driver.server.InProcessDedicatedServer;
import java.util.function.Function;
import net.minecraft.Util;
import net.minecraft.server.Main;
import net.minecraft.server.MinecraftServer;
import net.neoforged.neoforge.server.loading.ServerModLoader;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Makes the vanilla/NeoForge server entrypoint reusable inside the client process. */
@Mixin(Main.class)
abstract class ServerMainMixin {
    @ModifyExpressionValue(
            method = "main",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/Eula;hasAgreedToEULA()Z"))
    private static boolean bertie$acceptEulaForOwnedClientTestServer(boolean agreed) {
        return agreed || InProcessDedicatedServer.ownsCurrentServerLifecycle();
    }

    @Redirect(
            method = "main",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/MinecraftServer;spin(Ljava/util/function/Function;)Lnet/minecraft/server/MinecraftServer;",
                    ordinal = 1))
    private static MinecraftServer bertie$trackServerThread(
            Function<Thread, MinecraftServer> serverFactory) {
        MinecraftServer server = MinecraftServer.spin(serverFactory);
        InProcessDedicatedServer.onServerThreadCreated(server);
        return server;
    }

    @Redirect(
            method = "main",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/neoforged/neoforge/server/loading/ServerModLoader;load()V"))
    private static void bertie$doNotReloadMods() {
        if (!InProcessDedicatedServer.ownsCurrentServerLifecycle()) {
            ServerModLoader.load();
        }
    }

    @Redirect(
            method = "main",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/Util;startTimerHackThread()V"))
    private static void bertie$doNotStartAnotherTimerThread() {
        if (!InProcessDedicatedServer.ownsCurrentServerLifecycle()) {
            Util.startTimerHackThread();
        }
    }

    @Redirect(
            method = "main",
            at = @At(
                    value = "INVOKE",
                    target = "Ljava/lang/Runtime;addShutdownHook(Ljava/lang/Thread;)V"))
    private static void bertie$doNotRetainServerShutdownHook(Runtime runtime, Thread hook) {
        if (!InProcessDedicatedServer.ownsCurrentServerLifecycle()) {
            runtime.addShutdownHook(hook);
        }
    }
}
