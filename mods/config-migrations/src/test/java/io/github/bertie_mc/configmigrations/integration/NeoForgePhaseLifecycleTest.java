package io.github.bertie_mc.configmigrations.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.anthonyhilyard.iceberg.neoforge.config.NeoForgeIcebergConfigSpec;
import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.core.file.FileNotFoundAction;
import com.electronwill.nightconfig.toml.TomlFormat;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ConfigTracker;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.loading.FMLPaths;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;

@ResourceLock("neoforge-config-tracker")
@ResourceLock("neoforge-config-migration-launch")
class NeoForgePhaseLifecycleTest {
    private static final String MOD_ID = "configmigrationstest";
    private static final String SERVER_FILE = "configmigrations-phase-server.toml";

    @Test
    void serverLoadsKeepWorldOverridesIndependentFromTheGlobalFallback() throws Exception {
        Path gameDirectory = FMLPaths.GAMEDIR.get().toAbsolutePath().normalize();
        Path configDirectory = FMLPaths.CONFIGDIR.get().toAbsolutePath().normalize();
        Path manifest = configDirectory.resolve("config-migrations/migrations/neoforge/tests/server.toml");
        Path stateDirectory = configDirectory.resolve("config-migrations/state");
        Path globalBase = gameDirectory.resolve("configmigrations-phase-server-global");
        Path firstWorld = gameDirectory.resolve("configmigrations-phase-server-first-world");
        Path secondWorld = gameDirectory.resolve("configmigrations-phase-server-second-world");
        Path globalTarget = globalBase.resolve(SERVER_FILE);
        Path firstWorldTarget = firstWorld.resolve(SERVER_FILE);
        Path secondWorldTarget = secondWorld.resolve(SERVER_FILE);
        ConfigTracker tracker = ConfigTracker.INSTANCE;

        Files.createDirectories(manifest.getParent());
        deleteTree(stateDirectory);
        deleteTree(globalBase);
        deleteTree(firstWorld);
        deleteTree(secondWorld);
        Files.createDirectories(globalBase);
        Files.createDirectories(firstWorld);
        Files.writeString(globalTarget, "[settings]\nenabled = true\ncount = 4\n");
        Files.writeString(firstWorldTarget, "[settings]\nenabled = true\ncount = 8\n");
        Files.writeString(manifest, manifest());
        MigrationRuntime.resetForTests();
        MigrationRuntime.initializeLaunch(false);

        try {
            NeoForgeIcebergConfigSpec spec = new NeoForgeIcebergConfigSpec();
            ModContainer container = mock(ModContainer.class);
            when(container.getModId()).thenReturn(MOD_ID);
            MigrationRuntime.runNeoForgeRegistrationPhase(
                    () -> tracker.registerConfig(ModConfig.Type.SERVER, spec, container, SERVER_FILE));

            MigrationRuntime.runNeoForgeLoadPhase(
                    ModConfig.Type.SERVER, () -> tracker.loadConfigs(ModConfig.Type.SERVER, globalBase, firstWorld));

            assertFalse(read(firstWorldTarget).<Boolean>get("settings.enabled"));
            assertEquals(3, read(firstWorldTarget).<Number>get("settings.count").intValue());
            assertTrue(read(globalTarget).<Boolean>get("settings.enabled"));

            tracker.unloadConfigs(ModConfig.Type.SERVER);
            MigrationRuntime.runNeoForgeLoadPhase(
                    ModConfig.Type.SERVER, () -> tracker.loadConfigs(ModConfig.Type.SERVER, globalBase, secondWorld));

            assertFalse(Files.exists(secondWorldTarget));
            assertFalse(read(globalTarget).<Boolean>get("settings.enabled"));
            assertEquals(3, read(globalTarget).<Number>get("settings.count").intValue());
            assertEquals(1, state(gameDirectory, firstWorldTarget));
            assertEquals(1, state(gameDirectory, globalTarget));
        } finally {
            try {
                tracker.unloadConfigs(ModConfig.Type.SERVER);
            } finally {
                deleteTree(stateDirectory);
                Files.deleteIfExists(globalTarget);
                Files.deleteIfExists(firstWorldTarget);
                Files.deleteIfExists(secondWorldTarget);
                Files.deleteIfExists(manifest);
                MigrationRuntime.resetForTests();
            }
        }
    }

    private static CommentedConfig read(Path path) {
        return TomlFormat.instance().createParser().parse(path, FileNotFoundAction.THROW_ERROR);
    }

    private static int state(Path gameDirectory, Path target) throws IOException {
        return Integer.parseInt(
                Files.readString(versionFile(gameDirectory, target)).strip());
    }

    private static Path versionFile(Path gameDirectory, Path target) {
        Path relative = gameDirectory.relativize(target.toAbsolutePath().normalize());
        Path statePath = gameDirectory.resolve("config/config-migrations/state").resolve(relative);
        return statePath.resolveSibling(statePath.getFileName() + ".version");
    }

    private static void deleteTree(Path root) throws IOException {
        if (!Files.exists(root)) {
            return;
        }
        try (var paths = Files.walk(root)) {
            for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
                Files.deleteIfExists(path);
            }
        }
    }

    private static String manifest() {
        return """
                mod = "%s"
                type = "SERVER"
                file = "%s"

                [[changes]]
                version = 1
                op = "merge"

                [changes.fragment.settings]
                enabled = false
                count = 99
                """.formatted(MOD_ID, SERVER_FILE);
    }
}
