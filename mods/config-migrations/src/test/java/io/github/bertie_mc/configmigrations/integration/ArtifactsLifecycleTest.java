package io.github.bertie_mc.configmigrations.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import artifacts.config.ConfigManager;
import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.core.file.FileNotFoundAction;
import com.electronwill.nightconfig.toml.TomlFormat;
import java.nio.file.Files;
import java.nio.file.Path;
import net.neoforged.fml.loading.FMLPaths;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;

@ResourceLock("neoforge-config-migration-launch")
class ArtifactsLifecycleTest {
    private static final String CONFIG_NAME = "configmigrations-test-items";

    @Test
    void migrationRunsAfterNativeLoadAndBeforeArtifactsConsumesValues() throws Exception {
        Path gameDirectory = FMLPaths.GAMEDIR.get().toAbsolutePath().normalize();
        Path manifest = gameDirectory.resolve("config/config-migrations/migrations/artifacts/tests/items.toml");
        Path target = gameDirectory.resolve("config/artifacts/configmigrations-test-items.toml");
        Path state = gameDirectory.resolve(
                "config/config-migrations/state/config/artifacts/configmigrations-test-items.toml.version");

        Files.createDirectories(manifest.getParent());
        Files.createDirectories(target.getParent());
        Files.writeString(manifest, manifest());
        Files.writeString(target, "[settings]\nenabled = true\n");
        Files.deleteIfExists(state);
        MigrationRuntime.resetForTests();
        MigrationRuntime.initializeLaunch(false);

        try {
            try (ConfigManager manager = new ConfigManager(CONFIG_NAME, target, state)) {
                manager.load();

                assertFalse(manager.enabledWhenConsumed());
                assertEquals(3, manager.countWhenConsumed(), "Artifacts' spec corrects the merged invalid value");
                assertEquals("4", manager.stateWhenConsumed(), "state is committed before values are consumed");
                assertEquals("4", Files.readString(state).strip());
                assertFalse(read(target).<Boolean>get("settings.enabled"));
                assertEquals(3, read(target).<Number>get("settings.count").intValue());
            }

            Files.writeString(target, "[settings]\nenabled = true\ncount = 6\n");
            try (ConfigManager manager = new ConfigManager(CONFIG_NAME, target, state)) {
                manager.load();

                assertTrue(manager.enabledWhenConsumed(), "an applied migration is not policy-enforced");
                assertEquals(6, manager.countWhenConsumed());
                assertEquals("4", manager.stateWhenConsumed());
            }
        } finally {
            MigrationRuntime.resetForTests();
            Files.deleteIfExists(state);
            Files.deleteIfExists(target);
            Files.deleteIfExists(manifest);
        }
    }

    private static CommentedConfig read(Path path) {
        return TomlFormat.instance().createParser().parse(path, FileNotFoundAction.THROW_ERROR);
    }

    private static String manifest() {
        return """
                config = "%s"

                [[changes]]
                version = 4
                op = "merge"

                [changes.fragment.settings]
                enabled = false
                count = 99
                """.formatted(CONFIG_NAME);
    }
}
