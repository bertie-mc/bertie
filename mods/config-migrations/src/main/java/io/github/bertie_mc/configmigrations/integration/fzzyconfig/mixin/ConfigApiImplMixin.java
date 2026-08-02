package io.github.bertie_mc.configmigrations.integration.fzzyconfig.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.github.bertie_mc.configmigrations.integration.MigrationRuntime;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;

/** Wraps Fzzy Config's complete native read, validation, correction, and persistence cycle. */
@Pseudo
@Mixin(targets = "me.fzzyhmstrs.fzzy_config.impl.ConfigApiImpl", remap = false)
abstract class ConfigApiImplMixin {
    @Coerce
    @WrapMethod(
            method = "readOrCreateAndValidate$fzzy_config("
                    + "Lkotlin/jvm/functions/Function0;"
                    + "Lme/fzzyhmstrs/fzzy_config/config/Config;"
                    + "Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)"
                    + "Lme/fzzyhmstrs/fzzy_config/config/Config;")
    private Object configmigrations$runLoad(
            @Coerce Object configClass,
            @Coerce Object classInstance,
            String name,
            String folder,
            String subfolder,
            Operation<Object> original) {
        return MigrationRuntime.runFzzyConfigLoad(
                this,
                classInstance,
                name,
                folder,
                subfolder,
                () -> original.call(configClass, classInstance, name, folder, subfolder));
    }

    @WrapOperation(
            method = "writeFile(Ljava/lang/Object;"
                    + "Lme/fzzyhmstrs/fzzy_config/impl/ConfigApiImpl$FileResult;"
                    + "Ljava/lang/String;Ljava/lang/String;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Ljava/util/concurrent/CompletableFuture;runAsync("
                            + "Ljava/lang/Runnable;Ljava/util/concurrent/Executor;)"
                            + "Ljava/util/concurrent/CompletableFuture;"))
    private CompletableFuture<Void> configmigrations$joinMigrationWrite(
            Runnable task,
            Executor executor,
            Operation<CompletableFuture<Void>> original) {
        CompletableFuture<Void> write = original.call(task, executor);
        MigrationRuntime.joinFzzyConfigWrite(write);
        return write;
    }
}
