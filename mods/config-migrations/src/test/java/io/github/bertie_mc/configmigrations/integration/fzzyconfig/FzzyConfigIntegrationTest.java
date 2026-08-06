package io.github.bertie_mc.configmigrations.integration.fzzyconfig;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.bertie_mc.configmigrations.ConfigMigrationException;
import io.github.bertie_mc.configmigrations.migration.MigrationManager;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import me.fzzyhmstrs.fzzy_config.api.FileType;
import me.fzzyhmstrs.fzzy_config.config.Config;
import me.fzzyhmstrs.fzzy_config.impl.ConfigApiImpl;
import net.peanuuutz.tomlkt.TomlArray;
import net.peanuuutz.tomlkt.TomlElement;
import net.peanuuutz.tomlkt.TomlLiteral;
import net.peanuuutz.tomlkt.TomlTable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FzzyConfigIntegrationTest {
    private static final String NAME = "client";
    private static final String FOLDER = "example";
    private static final String SUBFOLDER = "nested";

    @TempDir
    Path gameDirectory;

    @Test
    void mergesBeforeNativeLoadAndCommitsAfterReturn() throws Exception {
        Path manifests = gameDirectory.resolve("migrations/fzzy");
        Path target = gameDirectory.resolve("config/example/nested/client.json");
        Path state = state(target);
        Files.createDirectories(manifests);
        Files.createDirectories(target.getParent());
        Files.writeString(manifests.resolve("client.toml"), manifest());
        Files.writeString(target, initialDocument());
        ConfigApiImpl api = ConfigApiImpl.INSTANCE;
        api.fixtureReset();
        Config config = new Config(FileType.JSON);
        AtomicInteger nativeLoads = new AtomicInteger();
        FzzyConfigIntegration integration = FzzyConfigIntegration.load(
                MigrationManager.load(gameDirectory), gameDirectory.resolve("config"), manifests);

        Object loaded = integration.runLoad(api, config, NAME, FOLDER, SUBFOLDER, () -> {
            nativeLoads.incrementAndGet();
            assertFalse(Files.exists(state));
            config.fixtureDocument(decode(target, FileType.JSON));
            assertFalse(bool(config.fixtureDocument(), "settings", "enabled"));
            assertEquals(7, integer(config.fixtureDocument(), "settings", "count"));
            return config;
        });

        assertEquals(config, loaded);
        assertEquals(1, nativeLoads.get());
        TomlTable persisted = decode(target, FileType.JSON);
        assertEquals("player value", string(persisted, "settings", "kept"));
        assertEquals(2, integer(persisted, "replace", "child"));
        assertEquals(2, ((TomlArray) persisted.get("values")).size());
        assertEquals(5, integer(persisted, "literal.dot"));
        assertEquals("3\n", Files.readString(state));
    }

    @Test
    void normalizesAnAlternateInputFormatBeforeNativeLoad() throws Exception {
        Path manifests = gameDirectory.resolve("migrations/fzzy");
        Path canonical = gameDirectory.resolve("config/example/nested/client.json");
        Path alternate = canonical.resolveSibling("client.toml");
        Files.createDirectories(manifests);
        Files.createDirectories(alternate.getParent());
        Files.writeString(manifests.resolve("client.toml"), manifest());
        Files.writeString(alternate, initialDocument());
        Config config = new Config(FileType.JSON);
        FzzyConfigIntegration integration = FzzyConfigIntegration.load(
                MigrationManager.load(gameDirectory), gameDirectory.resolve("config"), manifests);

        integration.runLoad(ConfigApiImpl.INSTANCE, config, NAME, FOLDER, SUBFOLDER, () -> {
            config.fixtureDocument(decode(alternate, FileType.TOML));
            assertFalse(bool(config.fixtureDocument(), "settings", "enabled"));
            write(canonical, FileType.JSON.encode(config.fixtureDocument()).get());
            try {
                Files.delete(alternate);
            } catch (Exception exception) {
                throw new IllegalStateException(exception);
            }
            return config;
        });

        assertTrue(Files.exists(canonical));
        assertFalse(Files.exists(alternate));
        assertFalse(bool(decode(canonical, FileType.JSON), "settings", "enabled"));
        assertEquals("3\n", Files.readString(state(canonical)));
    }

    @Test
    void preservesFzzyCompatibilityImportWhenTheCurrentFileIsMissing() throws Exception {
        Path manifests = gameDirectory.resolve("migrations/fzzy");
        Path canonical = gameDirectory.resolve("config/example/nested/client.json");
        Path compatibility = gameDirectory.resolve("config/legacy/client.toml");
        Files.createDirectories(manifests);
        Files.createDirectories(canonical.getParent());
        Files.createDirectories(compatibility.getParent());
        Files.writeString(manifests.resolve("client.toml"), manifest());
        Files.writeString(compatibility, initialDocument());
        ConfigApiImpl api = ConfigApiImpl.INSTANCE;
        api.fixtureReset();
        api.fixtureCompatibility(compatibility, FileType.TOML);
        Config config = new Config(FileType.JSON);
        FzzyConfigIntegration integration = FzzyConfigIntegration.load(
                MigrationManager.load(gameDirectory), gameDirectory.resolve("config"), manifests);

        try {
            integration.runLoad(api, config, NAME, FOLDER, SUBFOLDER, () -> {
                assertFalse(Files.exists(canonical));
                config.fixtureDocument(decode(compatibility, FileType.TOML));
                assertEquals("player value", string(config.fixtureDocument(), "settings", "kept"));
                assertFalse(bool(config.fixtureDocument(), "settings", "enabled"));
                write(canonical, FileType.JSON.encode(config.fixtureDocument()).get());
                try {
                    Files.delete(compatibility);
                } catch (Exception exception) {
                    throw new IllegalStateException(exception);
                }
                return config;
            });
        } finally {
            api.fixtureReset();
        }

        assertTrue(Files.exists(canonical));
        assertFalse(Files.exists(compatibility));
        assertEquals("player value", string(decode(canonical, FileType.JSON), "settings", "kept"));
        assertEquals("3\n", Files.readString(state(canonical)));
    }

    @Test
    void commitsAfterNativeCorrectionReturns() throws Exception {
        Path manifests = gameDirectory.resolve("migrations/fzzy");
        Path target = gameDirectory.resolve("config/example/nested/client.json");
        Files.createDirectories(manifests);
        Files.createDirectories(target.getParent());
        Files.writeString(manifests.resolve("client.toml"), manifest());
        Files.writeString(target, initialDocument());
        Config config = new Config(FileType.JSON);
        FzzyConfigIntegration integration = FzzyConfigIntegration.load(
                MigrationManager.load(gameDirectory), gameDirectory.resolve("config"), manifests);

        integration.runLoad(ConfigApiImpl.INSTANCE, config, NAME, FOLDER, SUBFOLDER, () -> {
            TomlTable corrected = withInteger(decode(target, FileType.JSON), "settings", "count", 4);
            config.fixtureDocument(corrected);
            write(target, FileType.JSON.encode(corrected).get());
            return config;
        });

        assertEquals(4, integer(decode(target, FileType.JSON), "settings", "count"));
        assertEquals("3\n", Files.readString(state(target)));
    }

    @Test
    void nativeFailureDoesNotCommit() throws Exception {
        Path manifests = gameDirectory.resolve("migrations/fzzy");
        Path target = gameDirectory.resolve("config/example/nested/client.json");
        Files.createDirectories(manifests);
        Files.createDirectories(target.getParent());
        Files.writeString(manifests.resolve("client.toml"), manifest());
        Files.writeString(target, initialDocument());
        FzzyConfigIntegration integration = FzzyConfigIntegration.load(
                MigrationManager.load(gameDirectory), gameDirectory.resolve("config"), manifests);

        assertThrows(
                IllegalStateException.class,
                () -> integration.runLoad(
                        ConfigApiImpl.INSTANCE, new Config(FileType.JSON), NAME, FOLDER, SUBFOLDER, () -> {
                            throw new IllegalStateException("native load failed");
                        }));
        assertFalse(Files.exists(state(target)));
    }

    @Test
    void trustsACompletedNativeLoadAndCommits() throws Exception {
        Path manifests = gameDirectory.resolve("migrations/fzzy");
        Path target = gameDirectory.resolve("config/example/nested/client.json");
        Files.createDirectories(manifests);
        Files.createDirectories(target.getParent());
        Files.writeString(manifests.resolve("client.toml"), manifest());
        Files.writeString(target, initialDocument());
        FzzyConfigIntegration integration = FzzyConfigIntegration.load(
                MigrationManager.load(gameDirectory), gameDirectory.resolve("config"), manifests);
        Config fallback = new Config(FileType.JSON);
        fallback.fixtureDocument(decodeString("{\"settings\":{\"enabled\":true}}", FileType.JSON));

        Object loaded = integration.runLoad(
                ConfigApiImpl.INSTANCE, new Config(FileType.JSON), NAME, FOLDER, SUBFOLDER, () -> fallback);

        assertEquals(fallback, loaded);
        TomlTable persisted = decode(target, FileType.JSON);
        assertFalse(bool(persisted, "settings", "enabled"));
        assertEquals("player value", string(persisted, "settings", "kept"));
        assertEquals("3\n", Files.readString(state(target)));
    }

    @Test
    void duplicateFilesAreRejected() throws Exception {
        Path manifests = gameDirectory.resolve("migrations/fzzy");
        Files.createDirectories(manifests.resolve("nested"));
        Files.writeString(manifests.resolve("first.toml"), manifest());
        Files.writeString(manifests.resolve("nested/second.toml"), manifest());

        assertThrows(
                ConfigMigrationException.class,
                () -> FzzyConfigIntegration.load(
                        MigrationManager.load(gameDirectory), gameDirectory.resolve("config"), manifests));
    }

    private Path state(Path target) {
        Path relative = gameDirectory
                .toAbsolutePath()
                .normalize()
                .relativize(target.toAbsolutePath().normalize());
        Path state = gameDirectory.resolve("config/config-migrations/state").resolve(relative);
        return state.resolveSibling(state.getFileName() + ".version");
    }

    private static TomlTable decode(Path path, FileType type) {
        try {
            return (TomlTable) type.decode(Files.readString(path)).get();
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }

    private static TomlTable decodeString(String contents, FileType type) {
        return (TomlTable) type.decode(contents).get();
    }

    private static TomlTable withInteger(TomlTable root, String table, String key, int value) {
        TomlTable current = (TomlTable) root.get(table);
        Map<String, TomlElement> child = new LinkedHashMap<>(current.getContent());
        child.put(key, new TomlLiteral(Integer.toString(value), TomlLiteral.Type.Integer));
        Map<String, TomlElement> document = new LinkedHashMap<>(root.getContent());
        document.put(table, new TomlTable(child, current.getAnnotations()));
        return new TomlTable(document, root.getAnnotations());
    }

    private static void write(Path path, String contents) {
        try {
            Files.writeString(path, contents);
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }

    private static boolean bool(TomlTable table, String... path) {
        return Boolean.parseBoolean(literal(table, path).getContent());
    }

    private static int integer(TomlTable table, String... path) {
        return Integer.parseInt(literal(table, path).getContent());
    }

    private static String string(TomlTable table, String... path) {
        return literal(table, path).getContent();
    }

    private static TomlLiteral literal(TomlTable table, String... path) {
        TomlTable current = table;
        for (int index = 0; index < path.length - 1; index++) {
            current = (TomlTable) current.get(path[index]);
        }
        return (TomlLiteral) current.get(path[path.length - 1]);
    }

    private static String initialDocument() {
        return """
                {
                  "settings": {"enabled": true, "kept": "player value"},
                  "replace": 1,
                  "values": [1, 2]
                }
                """;
    }

    private static String manifest() {
        return """
                file = "example/nested/client.json"

                [[changes]]
                version = 1
                op = "merge"
                [changes.fragment.settings]
                enabled = false

                [[changes]]
                version = 3
                op = "merge"
                [changes.fragment]
                values = [3, 4]
                "literal.dot" = 5
                [changes.fragment.settings]
                count = 7
                [changes.fragment.replace]
                child = 2
                """;
    }
}
