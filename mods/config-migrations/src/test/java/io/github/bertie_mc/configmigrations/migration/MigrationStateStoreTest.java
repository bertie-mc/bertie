package io.github.bertie_mc.configmigrations.migration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.bertie_mc.configmigrations.ConfigMigrationException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class MigrationStateStoreTest {
    @TempDir
    Path gameDirectory;

    @Test
    void mirrorsThePhysicalConfigPathUnderTheStateDirectory() throws Exception {
        Path stateDirectory = gameDirectory.resolve("config/config-migrations/state");
        MigrationStateStore state = new MigrationStateStore(gameDirectory, stateDirectory);
        Path config = gameDirectory.resolve("saves/World/serverconfig/example.toml");

        assertEquals(0, state.read(config));
        state.write(config, 7);

        Path version = stateDirectory.resolve("saves/World/serverconfig/example.toml.version");
        assertEquals(version, state.versionPath(config));
        assertEquals("7\n", Files.readString(version));
        assertEquals(7, state.read(config));
    }

    @Test
    void rejectsPathsOutsideTheGameDirectory() {
        MigrationStateStore state = new MigrationStateStore(
                gameDirectory, gameDirectory.resolve("config/config-migrations/state"));

        assertThrows(
                ConfigMigrationException.class,
                () -> state.read(gameDirectory.getParent().resolve("outside.toml")));
    }
}
