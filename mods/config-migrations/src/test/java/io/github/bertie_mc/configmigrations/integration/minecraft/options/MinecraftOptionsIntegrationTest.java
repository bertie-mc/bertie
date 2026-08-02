package io.github.bertie_mc.configmigrations.integration.minecraft.options;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.bertie_mc.configmigrations.ConfigMigrationException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.stream.Collectors;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class MinecraftOptionsIntegrationTest {
    @TempDir
    Path gameDirectory;

    @Test
    void flatFragmentsMergeIntoDataFixedOptionsAndPersistOnce() throws Exception {
        Path manifest = gameDirectory.resolve(
                "config/config-migrations/migrations/minecraft/options.toml");
        Path optionsFile = gameDirectory.resolve("options.txt");
        Path state = gameDirectory.resolve("config/config-migrations/state/options.txt.version");
        Files.createDirectories(manifest.getParent());
        Files.writeString(manifest, manifest());
        Files.writeString(optionsFile, "autoJump:true\nunknown:kept\n");

        MinecraftOptionsIntegration integration = MinecraftOptionsIntegration.load(gameDirectory);
        MinecraftOptionsIntegration.PendingMigration pending = integration.prepare(optionsFile);
        CompoundTag options = new CompoundTag();
        options.putString("version", "100");
        options.putString("autoJump", "true");
        options.putString("unknown", "kept");

        pending.apply(options);

        assertEquals("false", options.getString("autoJump"));
        assertEquals("120", options.getString("maxFps"));
        assertEquals("[\"vanilla\"]", options.getString("resourcePacks"));
        assertEquals("kept", options.getString("unknown"));
        Map<String, String> persisted = readOptions(optionsFile);
        assertEquals("false", persisted.get("autoJump"));
        assertEquals("120", persisted.get("maxFps"));
        assertEquals("[\"vanilla\"]", persisted.get("resourcePacks"));
        assertEquals("kept", persisted.get("unknown"));
        assertFalse(Files.exists(state));

        pending.commit();

        assertEquals("3\n", Files.readString(state));
        assertNull(MinecraftOptionsIntegration.load(gameDirectory).prepare(optionsFile));
    }

    @Test
    void aPendingMigrationCreatesTheOtherwiseMissingOptionsFile() throws Exception {
        Path manifest = gameDirectory.resolve(
                "config/config-migrations/migrations/minecraft/options.toml");
        Path optionsFile = gameDirectory.resolve("options.txt");
        Files.createDirectories(manifest.getParent());
        Files.writeString(manifest, manifest());

        MinecraftOptionsIntegration.PendingMigration pending =
                MinecraftOptionsIntegration.load(gameDirectory).prepare(optionsFile);

        assertTrue(Files.exists(optionsFile));
        CompoundTag options = new CompoundTag();
        pending.apply(options);
        assertEquals("false", readOptions(optionsFile).get("autoJump"));
    }

    @Test
    void noManifestLeavesAMissingOptionsFileAlone() {
        Path optionsFile = gameDirectory.resolve("options.txt");

        assertNull(MinecraftOptionsIntegration.load(gameDirectory).prepare(optionsFile));
        assertFalse(Files.exists(optionsFile));
    }

    @Test
    void duplicateOptionsTargetsAreRejected() throws Exception {
        Path directory = gameDirectory.resolve("config/config-migrations/migrations/minecraft");
        Files.createDirectories(directory.resolve("nested"));
        Files.writeString(directory.resolve("first.toml"), manifest());
        Files.writeString(directory.resolve("nested/second.toml"), manifest());

        assertThrows(
                ConfigMigrationException.class,
                () -> MinecraftOptionsIntegration.load(gameDirectory));
    }

    private static Map<String, String> readOptions(Path path) throws Exception {
        try (var lines = Files.lines(path)) {
            return lines.map(line -> line.split(":", 2))
                    .collect(Collectors.toMap(parts -> parts[0], parts -> parts[1]));
        }
    }

    private static String manifest() {
        return """
                file = "options.txt"

                [[changes]]
                version = 1
                op = "merge"
                [changes.fragment]
                autoJump = false
                resourcePacks = '["vanilla"]'

                [[changes]]
                version = 3
                op = "merge"
                [changes.fragment]
                maxFps = 120
                """;
    }
}
