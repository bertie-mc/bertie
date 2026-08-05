package io.github.bertie_mc.testing.client.world;

import io.github.bertie_mc.testing.client.context.DedicatedServerContext;
import io.github.bertie_mc.testing.client.context.IntegratedWorldContext;
import java.util.Properties;
import java.util.function.Consumer;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;

/**
 * Builds deterministic worlds for integrated- and dedicated-server client tests.
 *
 * <p>Each created context owns a running world or server and is intended for a try-with-resources
 * statement.
 */
public interface TestWorldBuilder {
    /**
     * Selects whether Bertie's deterministic world settings are applied before custom adjustments.
     *
     * @param useConsistentSettings whether to apply the settings described in the root package
     *     documentation
     * @return this builder
     */
    TestWorldBuilder setUseConsistentSettings(boolean useConsistentSettings);

    /**
     * Replaces the custom adjustment applied after the builder's default settings.
     *
     * @param settingsAdjuster an adjustment equivalent to changing the create-world screen
     * @return this builder
     */
    TestWorldBuilder adjustSettings(Consumer<WorldCreationUiState> settingsAdjuster);

    /**
     * Creates and joins an integrated world.
     *
     * @return a context that owns the integrated world
     */
    IntegratedWorldContext create();

    /**
     * Creates an in-process dedicated server with Bertie's default server properties.
     *
     * @return a context that owns the dedicated server
     */
    default DedicatedServerContext createServer() {
        return createServer(new Properties());
    }

    /**
     * Creates an in-process dedicated server with custom property overrides.
     *
     * @param serverProperties properties overriding Bertie's dedicated-server defaults
     * @return a context that owns the dedicated server
     */
    DedicatedServerContext createServer(Properties serverProperties);
}
