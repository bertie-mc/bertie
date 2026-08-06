package io.github.bertie_mc.configmigrations.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import me.fzzyhmstrs.fzzy_config.api.FileType;
import me.fzzyhmstrs.fzzy_config.config.Config;
import me.fzzyhmstrs.fzzy_config.impl.ConfigApiImpl;
import net.neoforged.fml.loading.FMLPaths;
import net.peanuuutz.tomlkt.TomlLiteral;
import net.peanuuutz.tomlkt.TomlTable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;

@ResourceLock("neoforge-config-migration-launch")
class FzzyConfigLifecycleTest {
    private static final String NAME = "client";
    private static final String FOLDER = "configmigrations-fzzy-test";
    private static final String SUBFOLDER = "nested";

    @Test
    void wrapsNativeLoadAndCommitsAfterItsPersistence() throws Exception {
        Path gameDirectory = FMLPaths.GAMEDIR.get().toAbsolutePath().normalize();
        Path manifest = gameDirectory.resolve("config/config-migrations/migrations/fzzy/tests/client.toml");
        Path target =
                FMLPaths.CONFIGDIR.get().resolve(FOLDER).resolve(SUBFOLDER).resolve(NAME + ".json");
        Path stateRoot = gameDirectory.resolve("config/config-migrations/state");
        Path state = stateRoot
                .resolve(gameDirectory.relativize(target.toAbsolutePath().normalize()))
                .resolveSibling(NAME + ".json.version");
        Files.createDirectories(manifest.getParent());
        Files.createDirectories(target.getParent());
        Files.writeString(manifest, manifest());
        Files.writeString(target, "{\"settings\":{\"enabled\":true,\"kept\":\"player\"}}");
        Files.deleteIfExists(state);
        ConfigApiImpl api = ConfigApiImpl.INSTANCE;
        api.fixtureReset();
        MigrationRuntime.resetForTests();
        MigrationRuntime.initializeLaunch(false);

        try {
            Config first = new Config(FileType.JSON);
            api.fixtureBeforeReturn(() -> {
                assertFalse(Files.exists(state));
                assertFalse(enabled(first));
                assertFalse(enabled(decode(target)));
            });

            Config firstLoaded = api.readOrCreateAndValidate$fzzy_config(
                    () -> new Config(FileType.JSON), first, NAME, FOLDER, SUBFOLDER);

            assertEquals(first, firstLoaded);
            assertFalse(enabled(firstLoaded));
            assertEquals("player", string(firstLoaded, "kept"));
            assertEquals("2\n", Files.readString(state));
            assertFalse(enabled(decode(target)));

            api.fixtureBeforeReturn(() -> {});
            Files.writeString(target, "{\"settings\":{\"enabled\":true}}");
            Config second = new Config(FileType.JSON);
            Config secondLoaded = api.readOrCreateAndValidate$fzzy_config(
                    () -> new Config(FileType.JSON), second, NAME, FOLDER, SUBFOLDER);

            assertTrue(enabled(secondLoaded));
            assertEquals(2, api.fixtureNativeLoads());
        } finally {
            MigrationRuntime.resetForTests();
            api.fixtureAwaitWrites();
            api.fixtureReset();
            Files.deleteIfExists(state);
            Files.deleteIfExists(target);
            Files.deleteIfExists(manifest);
        }
    }

    @Test
    void leavesTheMissingFileBranchAvailableForCompatibilityImport() throws Exception {
        String name = "compat";
        Path gameDirectory = FMLPaths.GAMEDIR.get().toAbsolutePath().normalize();
        Path manifest = gameDirectory.resolve("config/config-migrations/migrations/fzzy/tests/compat.toml");
        Path target =
                FMLPaths.CONFIGDIR.get().resolve(FOLDER).resolve(SUBFOLDER).resolve(name + ".json");
        Path compatibility =
                FMLPaths.CONFIGDIR.get().resolve("configmigrations-fzzy-legacy").resolve(name + ".toml");
        Path stateRoot = gameDirectory.resolve("config/config-migrations/state");
        Path state = stateRoot
                .resolve(gameDirectory.relativize(target.toAbsolutePath().normalize()))
                .resolveSibling(name + ".json.version");
        Files.createDirectories(manifest.getParent());
        Files.createDirectories(target.getParent());
        Files.createDirectories(compatibility.getParent());
        Files.writeString(manifest, """
                file = "%s/%s/%s.json"

                [[changes]]
                version = 2
                op = "merge"
                [changes.fragment.settings]
                enabled = false
                """.formatted(FOLDER, SUBFOLDER, name));
        Files.writeString(compatibility, "{\"settings\":{\"enabled\":true,\"kept\":\"legacy player value\"}}");
        Files.deleteIfExists(target);
        Files.deleteIfExists(state);
        ConfigApiImpl api = ConfigApiImpl.INSTANCE;
        api.fixtureReset();
        api.fixtureCompatibility(compatibility, FileType.TOML);
        MigrationRuntime.resetForTests();
        MigrationRuntime.initializeLaunch(false);

        try {
            Config config = new Config(FileType.JSON);
            api.fixtureBeforeReturn(() -> {
                assertTrue(Files.exists(target));
                assertFalse(Files.exists(compatibility));
                assertFalse(enabled(config));
            });

            Config loaded = api.readOrCreateAndValidate$fzzy_config(
                    () -> new Config(FileType.JSON), config, name, FOLDER, SUBFOLDER);

            assertFalse(enabled(loaded));
            assertEquals("legacy player value", string(loaded, "kept"));
            assertEquals("2\n", Files.readString(state));
        } finally {
            MigrationRuntime.resetForTests();
            api.fixtureAwaitWrites();
            api.fixtureReset();
            Files.deleteIfExists(state);
            Files.deleteIfExists(target);
            Files.deleteIfExists(compatibility);
            Files.deleteIfExists(manifest);
        }
    }

    private static boolean enabled(Config config) {
        return enabled(config.fixtureDocument());
    }

    private static boolean enabled(TomlTable document) {
        TomlTable settings = (TomlTable) document.get("settings");
        return Boolean.parseBoolean(((TomlLiteral) settings.get("enabled")).getContent());
    }

    private static String string(Config config, String key) {
        TomlTable settings = (TomlTable) config.fixtureDocument().get("settings");
        return ((TomlLiteral) settings.get(key)).getContent();
    }

    private static TomlTable decode(Path target) {
        try {
            return (TomlTable) FileType.JSON.decode(Files.readString(target)).get();
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }

    private static String manifest() {
        return """
                file = "%s/%s/%s.json"

                [[changes]]
                version = 2
                op = "merge"
                [changes.fragment.settings]
                enabled = false
                """.formatted(FOLDER, SUBFOLDER, NAME);
    }
}
