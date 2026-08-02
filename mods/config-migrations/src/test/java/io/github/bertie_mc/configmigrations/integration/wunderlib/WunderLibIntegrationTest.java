package io.github.bertie_mc.configmigrations.integration.wunderlib;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.github.bertie_mc.configmigrations.ConfigMigrationException;
import io.github.bertie_mc.configmigrations.migration.MigrationManager;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class WunderLibIntegrationTest {
    private static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath("example", "client");

    @TempDir
    Path gameDirectory;

    @Test
    void recursivelyMergesAndSavesBeforeCommitting() throws Exception {
        Path directory = gameDirectory.resolve("migrations/wunderlib");
        Path target = gameDirectory.resolve("config/example/client.json");
        Path state = gameDirectory.resolve(
                "config/config-migrations/state/config/example/client.json.version");
        Files.createDirectories(directory);
        Files.writeString(directory.resolve("client.toml"), manifest());

        JsonObject settings = new JsonObject();
        settings.addProperty("enabled", true);
        settings.addProperty("kept", "player value");
        JsonObject document = new JsonObject();
        document.add("settings", settings);
        document.addProperty("replace", 1);
        document.add("values", array(1, 2));
        AtomicInteger saves = new AtomicInteger();
        WunderLibIntegration integration =
                WunderLibIntegration.load(MigrationManager.load(gameDirectory), directory);

        integration.migrate(ID, target, document, () -> {
            saves.incrementAndGet();
            assertFalse(Files.exists(state));
            assertFalse(document.getAsJsonObject("settings").get("enabled").getAsBoolean());
            assertEquals(7, document.getAsJsonObject("settings").get("count").getAsInt());
            write(target, document);
        });

        assertEquals(1, saves.get());
        assertEquals("player value", document.getAsJsonObject("settings").get("kept").getAsString());
        assertEquals(2, document.getAsJsonObject("replace").get("child").getAsInt());
        assertEquals(array(3, 4), document.getAsJsonArray("values"));
        assertEquals(5, document.get("literal.dot").getAsInt());
        assertEquals("3\n", Files.readString(state));

        document.getAsJsonObject("settings").addProperty("enabled", true);
        integration.migrate(ID, target, document, saves::incrementAndGet);
        assertTrue(document.getAsJsonObject("settings").get("enabled").getAsBoolean());
        assertEquals(1, saves.get());
    }

    @Test
    void throwingNativeSaveDoesNotCommit() throws Exception {
        Path directory = gameDirectory.resolve("migrations/wunderlib");
        Path target = gameDirectory.resolve("config/example/client.json");
        Path state = gameDirectory.resolve(
                "config/config-migrations/state/config/example/client.json.version");
        Files.createDirectories(directory);
        Files.createDirectories(target.getParent());
        Files.writeString(directory.resolve("client.toml"), manifest());

        JsonObject document = new JsonObject();
        JsonObject settings = new JsonObject();
        settings.addProperty("enabled", true);
        document.add("settings", settings);
        Files.writeString(target, document.toString());
        WunderLibIntegration integration =
                WunderLibIntegration.load(MigrationManager.load(gameDirectory), directory);

        assertThrows(
                UncheckedIOException.class,
                () -> integration.migrate(ID, target, document, () -> {
                    throw new UncheckedIOException(new IOException("write failed"));
                }));
        assertFalse(Files.exists(state));
    }

    @Test
    void duplicateIdsAreRejected() throws Exception {
        Path directory = gameDirectory.resolve("migrations/wunderlib");
        Files.createDirectories(directory.resolve("nested"));
        Files.writeString(directory.resolve("first.toml"), manifest());
        Files.writeString(directory.resolve("nested/second.toml"), manifest());

        assertThrows(
                ConfigMigrationException.class,
                () -> WunderLibIntegration.load(MigrationManager.load(gameDirectory), directory));
    }

    private static JsonArray array(int... values) {
        JsonArray array = new JsonArray();
        for (int value : values) {
            array.add(value);
        }
        return array;
    }

    private static void write(Path path, JsonObject document) {
        try {
            Files.createDirectories(path.getParent());
            Files.writeString(path, document.toString());
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    private static String manifest() {
        return """
                id = "example:client"

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
