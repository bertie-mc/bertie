package io.github.bertie_mc.bertieprogression.startup;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletionException;
import org.junit.jupiter.api.Test;

class StartupRegistryLookupTest {
    @Test
    void registrySnapshotFinishesOnCallerBeforeProviderRegistrationMutatesIt() {
        Thread caller = Thread.currentThread();
        Map<String, String> registries = new HashMap<>();
        registries.put("base", "ready");
        var lookup = StartupRegistryLookup.completeBeforeProviders(() -> {
            assertSame(caller, Thread.currentThread());
            return Map.copyOf(registries);
        });
        assertTrue(lookup.isDone());
        registries.put("provider", "registered afterward");
        assertEquals(Map.of("base", "ready"), lookup.join());
        assertEquals(2, registries.size());
    }

    @Test
    void invalidRegistryDataStillFailsTheProviderFutureWithItsOriginalCause() {
        var original = new IllegalStateException("invalid registry data");
        var lookup = StartupRegistryLookup.completeBeforeProviders(() -> {
            throw original;
        });
        assertTrue(lookup.isCompletedExceptionally());
        var failure = assertThrows(CompletionException.class, lookup::join);
        assertSame(original, failure.getCause());
    }
}
