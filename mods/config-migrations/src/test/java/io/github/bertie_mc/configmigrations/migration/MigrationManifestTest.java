package io.github.bertie_mc.configmigrations.migration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.bertie_mc.configmigrations.ConfigMigrationException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class MigrationManifestTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void discoversShardedTargetsAndLoadsVersionedMergeFragments() throws IOException {
        Path directory = temporaryDirectory.resolve("neoforge");
        Path path = directory.resolve("create/server.toml");
        Files.createDirectories(path.getParent());
        Files.writeString(path, """
                mod = "create"
                type = "SERVER"
                file = "create-server.toml"
                explanation = "extra metadata is harmless"

                [[changes]]
                version = 2
                op = "merge"

                [changes.fragment.recipes]
                allowRegularCraftingInCrafter = false
                maxRotationSpeed = 256
                """);

        List<MigrationManifest> manifests = MigrationManifest.loadDirectory(directory);

        assertEquals(1, manifests.size());
        MigrationManifest manifest = manifests.getFirst();
        assertEquals(path, manifest.source());
        assertEquals("create", manifest.text("mod"));
        assertEquals(1, manifest.changes().size());
        assertEquals(2, manifest.changes().getFirst().version());
        assertEquals(
                false,
                manifest.changes().getFirst().fragment().get("recipes.allowRegularCraftingInCrafter"));
        assertEquals(
                256,
                manifest.changes().getFirst().fragment().<Number>get("recipes.maxRotationSpeed").intValue());
    }

    @Test
    void aMissingIntegrationDirectoryMeansThereAreNoMigrations() {
        assertEquals(List.of(), MigrationManifest.loadDirectory(temporaryDirectory.resolve("missing")));
    }

    @Test
    void rejectsAnOperationTheRuntimeCannotApply() throws IOException {
        Path path = temporaryDirectory.resolve("target.toml");
        Files.writeString(path, """
                [[changes]]
                version = 1
                op = "rename"

                [changes.fragment]
                old = "new"
                """);

        assertThrows(ConfigMigrationException.class, () -> MigrationManifest.load(path));
    }

    @Test
    void versionsMustIncrease() throws IOException {
        Path path = temporaryDirectory.resolve("target.toml");
        Files.writeString(path, """
                [[changes]]
                version = 2
                op = "merge"
                [changes.fragment]
                first = true

                [[changes]]
                version = 2
                op = "merge"
                [changes.fragment]
                second = true
                """);

        assertThrows(ConfigMigrationException.class, () -> MigrationManifest.load(path));
    }
}
