package io.github.bertie_mc.configmigrations.integration.owoconfig;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import blue.endless.jankson.Jankson;
import blue.endless.jankson.JsonGrammar;
import blue.endless.jankson.JsonObject;
import io.github.bertie_mc.configmigrations.migration.MigrationManager;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class OwoConfigIntegrationTest {
    @TempDir
    Path gameDirectory;

    private final Jankson jankson = Jankson.builder().build();

    @Test
    void migratesAConfigThatDidNotExistBeforeLoad() throws Exception {
        Path configPath = gameDirectory.resolve("config/demo.json5");
        OwoConfigIntegration integration = integration(7);

        assertTrue(integration.prepareLoad("demo", configPath));
        ConfigModel defaults = new ConfigModel();
        save(configPath, defaults);

        JsonObject document = read(configPath);
        JsonObject merged = (JsonObject) integration.mergeDocument("demo", document);
        ConfigModel live = jankson.fromJson(merged, ConfigModel.class);
        integration.finishLoad("demo", () -> save(configPath, live));

        assertEquals(7, read(configPath).getInt("value", -1));
        assertFalse(integration.prepareLoad("demo", configPath));
    }

    @Test
    void trustsACompletedNativeSaveLifecycle() throws Exception {
        Path configPath = gameDirectory.resolve("config/demo.json5");
        ConfigModel existing = new ConfigModel();
        existing.value = 1;
        save(configPath, existing);
        OwoConfigIntegration integration = integration(8);

        assertTrue(integration.prepareLoad("demo", configPath));
        JsonObject merged = (JsonObject) integration.mergeDocument("demo", read(configPath));
        ConfigModel live = jankson.fromJson(merged, ConfigModel.class);

        integration.finishLoad("demo", () -> {
            // Native save methods define their own success contract.
        });
        assertFalse(integration.prepareLoad("demo", configPath));
    }

    @Test
    void commitsTheCanonicalValueAcceptedByTheOwner() throws Exception {
        Path configPath = gameDirectory.resolve("config/demo.json5");
        ConfigModel existing = new ConfigModel();
        existing.value = 2;
        save(configPath, existing);
        OwoConfigIntegration integration = integration(99);

        assertTrue(integration.prepareLoad("demo", configPath));
        integration.mergeDocument("demo", read(configPath));

        ConfigModel constrainedLiveValue = new ConfigModel();
        integration.finishLoad(
                "demo",
                () -> save(configPath, constrainedLiveValue));

        assertEquals(4, read(configPath).getInt("value", -1));
        assertFalse(integration.prepareLoad("demo", configPath));
    }

    private OwoConfigIntegration integration(int value) throws IOException {
        Path manifests = gameDirectory.resolve("manifests");
        Files.createDirectories(manifests);
        Files.writeString(
                manifests.resolve("demo.toml"),
                """
                config = "demo"

                [[changes]]
                version = 1
                op = "merge"

                [changes.fragment]
                value = %d
                """.formatted(value));
        return OwoConfigIntegration.load(MigrationManager.load(gameDirectory), manifests);
    }

    private JsonObject read(Path path) throws Exception {
        return jankson.load(Files.readString(path, StandardCharsets.UTF_8));
    }

    private void save(Path path, ConfigModel model) {
        try {
            Files.createDirectories(path.getParent());
            Files.writeString(
                    path,
                    jankson.toJson(model).toJson(JsonGrammar.JANKSON),
                    StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    public static final class ConfigModel {
        public int value = 4;
    }
}
