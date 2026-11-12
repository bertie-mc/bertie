package io.github.bertie_mc.bertieprogression.mixin.startup;

import com.mojang.logging.LogUtils;
import io.github.bertie_mc.bertieprogression.startup.StartupRegistryLookup;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Supplier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Pseudo
@Mixin(targets = "com.ridanisaurus.emendatusenigmatica.loader.EELoader", remap = false)
public abstract class EmendatusRegistryLookupMixin {
    // Provider construction can initialize registries. Finish traversing the root registry first.
    @Redirect(
            method = "lambda$registerDatagen$2",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Ljava/util/concurrent/CompletableFuture;supplyAsync(Ljava/util/function/Supplier;Ljava/util/concurrent/Executor;)Ljava/util/concurrent/CompletableFuture;"),
            require = 1)
    private <T> CompletableFuture<T> bertie$lookupBeforeProviders(Supplier<T> factory, Executor executor) {
        var lookup = StartupRegistryLookup.completeBeforeProviders(factory);
        if (!lookup.isCompletedExceptionally()) {
            LogUtils.getLogger()
                    .info(
                            "Emendatus registry lookup prepared before providers on {}",
                            Thread.currentThread().getName());
        }
        return lookup;
    }
}
