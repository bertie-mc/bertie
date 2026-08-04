package io.github.bertie_mc.testing.client;

import java.util.Properties;
import java.util.function.Consumer;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;

/** Builds deterministic worlds for integrated- and dedicated-server client tests. */
public interface TestWorldBuilder {
    TestWorldBuilder setUseConsistentSettings(boolean useConsistentSettings);

    TestWorldBuilder adjustSettings(Consumer<WorldCreationUiState> settingsAdjuster);

    IntegratedWorldContext create();

    default DedicatedServerContext createServer() {
        return createServer(new Properties());
    }

    DedicatedServerContext createServer(Properties serverProperties);
}
