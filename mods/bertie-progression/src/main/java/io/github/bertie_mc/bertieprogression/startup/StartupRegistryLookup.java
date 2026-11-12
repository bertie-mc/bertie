package io.github.bertie_mc.bertieprogression.startup;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public final class StartupRegistryLookup {
    public static <T> CompletableFuture<T> completeBeforeProviders(Supplier<T> factory) {
        try {
            return CompletableFuture.completedFuture(factory.get());
        } catch (Throwable failure) {
            // Keep the original future-based failure path used by EE's data providers.
            return CompletableFuture.failedFuture(failure);
        }
    }

    private StartupRegistryLookup() {}
}
