package io.github.bertie_mc.configmigrations.integration.neoforge.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.github.bertie_mc.configmigrations.integration.MigrationRuntime;
import java.nio.file.Path;
import net.neoforged.fml.config.ConfigTracker;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Bounds each world-specific SERVER config transaction around the native load call. */
@Mixin(value = ServerLifecycleHooks.class, remap = false)
abstract class ServerLifecycleHooksMixin {
    @WrapOperation(
            method = "handleServerAboutToStart(Lnet/minecraft/server/MinecraftServer;)V",
            at =
                    @At(
                            value = "INVOKE",
                            target = "Lnet/neoforged/fml/config/ConfigTracker;loadConfigs("
                                    + "Lnet/neoforged/fml/config/ModConfig$Type;Ljava/nio/file/Path;"
                                    + "Ljava/nio/file/Path;)V"))
    private static void configmigrations$runServerLoad(
            ConfigTracker tracker, ModConfig.Type type, Path basePath, Path overridePath, Operation<Void> original) {
        MigrationRuntime.runNeoForgeLoadPhase(type, () -> original.call(tracker, type, basePath, overridePath));
    }
}
