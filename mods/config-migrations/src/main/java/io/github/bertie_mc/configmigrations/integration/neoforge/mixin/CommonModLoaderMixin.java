package io.github.bertie_mc.configmigrations.integration.neoforge.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.github.bertie_mc.configmigrations.integration.MigrationRuntime;
import java.nio.file.Path;
import java.util.concurrent.Executor;
import net.neoforged.fml.config.ConfigTracker;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.internal.CommonModLoader;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Establishes launch policy and bounds each native configuration-loading phase. */
@Mixin(value = CommonModLoader.class, remap = false)
abstract class CommonModLoaderMixin {
    @Inject(method = "begin(Ljava/lang/Runnable;Z)V", at = @At("HEAD"))
    private static void configmigrations$initializeLaunchPolicy(
            Runnable periodicTask, boolean datagen, CallbackInfo callbackInfo) {
        MigrationRuntime.initializeLaunch(datagen);
    }

    @WrapOperation(
            method = "begin(Ljava/lang/Runnable;Z)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/neoforged/fml/ModLoader;gatherAndInitializeMods("
                            + "Ljava/util/concurrent/Executor;Ljava/util/concurrent/Executor;"
                            + "Ljava/lang/Runnable;)V"))
    private static void configmigrations$runRegistrationPhase(
            Executor syncExecutor,
            Executor parallelExecutor,
            Runnable periodicTask,
            Operation<Void> original) {
        MigrationRuntime.runNeoForgeRegistrationPhase(
                () -> original.call(syncExecutor, parallelExecutor, periodicTask));
    }

    @WrapOperation(
            method = "lambda$begin$1()V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/neoforged/fml/config/ConfigTracker;loadConfigs("
                            + "Lnet/neoforged/fml/config/ModConfig$Type;Ljava/nio/file/Path;)V"))
    private static void configmigrations$runClientOrCommonLoad(
            ConfigTracker tracker,
            ModConfig.Type type,
            Path basePath,
            Operation<Void> original) {
        MigrationRuntime.runNeoForgeLoadPhase(
                type, () -> original.call(tracker, type, basePath));
    }
}
