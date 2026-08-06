package io.github.bertie_mc.configmigrations.migration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.toml.TomlFormat;
import io.github.bertie_mc.configmigrations.ConfigMigrationException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class MigrationManagerTest {
    @TempDir
    Path gameDirectory;

    @Test
    void anExistingFileWithoutStateAppliesTheWholeHistoryOnce() throws IOException {
        MigrationManifest manifest = manifest("""
                [[changes]]
                version = 1
                op = "merge"
                [changes.fragment.recipes]
                enabled = false

                [[changes]]
                version = 4
                op = "merge"
                [changes.fragment.recipes]
                count = 7
                """);
        Path target = gameDirectory.resolve("config/example-common.toml");
        Files.createDirectories(target.getParent());
        Files.writeString(target, "[recipes]\nenabled = true\ncount = 3\n");
        MigrationManager manager = MigrationManager.load(gameDirectory);

        MigrationManager.Migration migration = manager.prepare(manifest, target);
        CommentedConfig document = TomlFormat.newConfig();
        document.set("recipes.enabled", true);
        document.set("recipes.count", 3);
        document.set("recipes.playerChoice", "kept");

        NightConfigMerge.apply(document, migration.changes());
        assertFalse(document.<Boolean>get("recipes.enabled"));
        assertEquals(7, document.<Number>get("recipes.count").intValue());
        assertEquals("kept", document.get("recipes.playerChoice"));
        assertFalse(Files.exists(versionFile(target)), "prepare must not advance state");

        migration.commit();
        assertEquals("4\n", Files.readString(versionFile(target)));
        assertNull(MigrationManager.load(gameDirectory).prepare(manifest, target));
    }

    @Test
    void storedVersionSelectsOnlyNewerFragments() throws IOException {
        MigrationManifest manifest = manifest("""
                [[changes]]
                version = 2
                op = "merge"
                [changes.fragment]
                old = false

                [[changes]]
                version = 5
                op = "merge"
                [changes.fragment]
                current = true
                """);
        Path target = gameDirectory.resolve("config/example.toml");
        Files.createDirectories(versionFile(target).getParent());
        Files.writeString(versionFile(target), "2\n");

        MigrationManager.Migration migration =
                MigrationManager.load(gameDirectory).prepare(manifest, target);
        CommentedConfig document = TomlFormat.newConfig();
        document.set("old", true);
        NightConfigMerge.apply(document, migration.changes());

        assertTrue(document.<Boolean>get("old"));
        assertTrue(document.<Boolean>get("current"));
        migration.commit();
        assertEquals("5\n", Files.readString(versionFile(target)));
    }

    @Test
    void physicalServerFilesHaveIndependentVersions() throws IOException {
        MigrationManifest manifest = manifest("""
                [[changes]]
                version = 3
                op = "merge"
                [changes.fragment]
                enabled = false
                """);
        Path first = gameDirectory.resolve("saves/First/serverconfig/example.toml");
        Path second = gameDirectory.resolve("saves/Second/serverconfig/example.toml");
        MigrationManager manager = MigrationManager.load(gameDirectory);

        MigrationManager.Migration firstMigration = manager.prepare(manifest, first);
        MigrationManager.Migration secondMigration = manager.prepare(manifest, second);
        firstMigration.commit();

        assertEquals("3\n", Files.readString(versionFile(first)));
        assertFalse(Files.exists(versionFile(second)));
        assertEquals(1, secondMigration.changes().size());
    }

    @Test
    void onePhysicalConfigHasOnlyOneManifestOwner() throws IOException {
        MigrationManifest first = manifest("first.toml", """
                [[changes]]
                version = 1
                op = "merge"
                [changes.fragment]
                first = true
                """);
        MigrationManifest second = manifest("second.toml", """
                [[changes]]
                version = 2
                op = "merge"
                [changes.fragment]
                second = true
                """);
        Path target = gameDirectory.resolve("config/example.toml");
        MigrationManager manager = MigrationManager.load(gameDirectory);

        manager.prepare(first, target);
        assertThrows(ConfigMigrationException.class, () -> manager.prepare(second, target));
    }

    private MigrationManifest manifest(String contents) throws IOException {
        return manifest("manifest.toml", contents);
    }

    private MigrationManifest manifest(String file, String contents) throws IOException {
        Path path = gameDirectory.resolve(file);
        Files.writeString(path, contents);
        return MigrationManifest.load(path);
    }

    private Path versionFile(Path target) {
        Path relative = gameDirectory.relativize(target);
        Path statePath = gameDirectory.resolve("config/config-migrations/state").resolve(relative);
        return statePath.resolveSibling(statePath.getFileName() + ".version");
    }
}
