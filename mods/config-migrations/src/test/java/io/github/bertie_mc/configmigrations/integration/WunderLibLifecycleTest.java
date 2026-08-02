package io.github.bertie_mc.configmigrations.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonParser;
import de.ambertation.wunderlib.configs.ConfigFile;
import de.ambertation.wunderlib.utils.Version;
import java.nio.file.Files;
import java.nio.file.Path;
import net.neoforged.fml.loading.FMLPaths;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;

@ResourceLock("neoforge-config-migration-launch")
class WunderLibLifecycleTest {
    private static final String NAMESPACE = "configmigrations-wunderlib-test";
    private static final String CATEGORY = "client";

    @Test
    void constructorMigratesBeforeTheConcreteConfigConsumesValues() throws Exception {
        Path gameDirectory = FMLPaths.GAMEDIR.get().toAbsolutePath().normalize();
        Path manifest = gameDirectory.resolve(
                "config/config-migrations/migrations/wunderlib/tests/client.toml");
        Path target = FMLPaths.CONFIGDIR.get().resolve(NAMESPACE).resolve(CATEGORY + ".json");
        Path state = gameDirectory.resolve("config/config-migrations/state")
                .resolve(gameDirectory.relativize(target.toAbsolutePath().normalize()))
                .resolveSibling(CATEGORY + ".json.version");
        Files.createDirectories(manifest.getParent());
        Files.createDirectories(target.getParent());
        Files.writeString(manifest, manifest());
        Files.writeString(target, "{\"settings\":{\"enabled\":true,\"kept\":\"player\"}}");
        Files.deleteIfExists(state);
        MigrationRuntime.resetForTests();
        MigrationRuntime.initializeLaunch(false);

        try {
            ConsumingConfig first = new ConsumingConfig();

            assertFalse(first.enabledWhenConstructed);
            assertEquals(1, first.fixtureSaveCount());
            assertEquals("player", first.fixtureRoot()
                    .getAsJsonObject("settings")
                    .get("kept")
                    .getAsString());
            assertEquals("2\n", Files.readString(state));
            assertFalse(JsonParser.parseString(Files.readString(target))
                    .getAsJsonObject()
                    .getAsJsonObject("settings")
                    .get("enabled")
                    .getAsBoolean());

            Files.writeString(target, "{\"settings\":{\"enabled\":true}}");
            ConsumingConfig second = new ConsumingConfig();

            assertTrue(second.enabledWhenConstructed);
            assertEquals(0, second.fixtureSaveCount());
        } finally {
            MigrationRuntime.resetForTests();
            Files.deleteIfExists(state);
            Files.deleteIfExists(target);
            Files.deleteIfExists(manifest);
        }
    }

    private static String manifest() {
        return """
                id = "%s:%s"

                [[changes]]
                version = 2
                op = "merge"
                [changes.fragment.settings]
                enabled = false
                """.formatted(NAMESPACE, CATEGORY);
    }

    private static final class ConsumingConfig extends ConfigFile {
        private final boolean enabledWhenConstructed;

        private ConsumingConfig() {
            super(new Provider(), NAMESPACE, CATEGORY);
            enabledWhenConstructed = fixtureRoot()
                    .getAsJsonObject("settings")
                    .get("enabled")
                    .getAsBoolean();
        }
    }

    private static final class Provider implements Version.ModVersionProvider {
        @Override
        public String getNamespace() {
            return NAMESPACE;
        }
    }
}
