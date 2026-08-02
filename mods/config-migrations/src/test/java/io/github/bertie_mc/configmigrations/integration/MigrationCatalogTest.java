package io.github.bertie_mc.configmigrations.integration;

import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.bertie_mc.configmigrations.ConfigMigrationException;
import io.github.bertie_mc.configmigrations.integration.neoforge.NeoForgeIntegration;
import io.github.bertie_mc.configmigrations.migration.MigrationManager;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class MigrationCatalogTest {
    @TempDir
    Path gameDirectory;

    @Test
    void duplicateNeoForgeSelectorsAreRejected() throws Exception {
        Path directory = gameDirectory.resolve("migrations/neoforge");
        Files.createDirectories(directory.resolve("nested"));
        Files.writeString(directory.resolve("first.toml"), manifest());
        Files.writeString(directory.resolve("nested/second.toml"), manifest());

        assertThrows(
                ConfigMigrationException.class,
                () -> NeoForgeIntegration.load(MigrationManager.load(gameDirectory), directory));
    }

    @Test
    void unknownIntegrationDirectoriesAreRejected() throws Exception {
        Path migrations = gameDirectory.resolve("migrations");
        Files.createDirectories(migrations.resolve("neoforgee"));

        assertThrows(
                ConfigMigrationException.class,
                () -> MigrationRuntime.validateIntegrationDirectories(migrations));
    }

    private static String manifest() {
        return """
                mod = "example"
                type = "COMMON"
                file = "example.toml"

                [[changes]]
                version = 1
                op = "merge"
                [changes.fragment]
                enabled = false
                """;
    }
}
