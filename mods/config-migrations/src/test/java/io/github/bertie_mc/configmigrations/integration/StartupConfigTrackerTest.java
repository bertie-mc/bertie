package io.github.bertie_mc.configmigrations.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.anthonyhilyard.iceberg.neoforge.config.NeoForgeIcebergConfigSpec;
import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.core.file.FileNotFoundAction;
import com.electronwill.nightconfig.core.io.WritingMode;
import com.electronwill.nightconfig.toml.TomlFormat;
import com.electronwill.nightconfig.toml.TomlWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ConfigTracker;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;

@ResourceLock("neoforge-config-tracker")
@ResourceLock("neoforge-config-migration-launch")
class StartupConfigTrackerTest {
    private static final String MOD_ID = "configmigrationstest";
    private static final String EXISTING_FILE = "configmigrations-startup-existing.toml";
    private static final String COPIED_FILE = "configmigrations-startup-copied.toml";

    @Test
    void startupMigratesStandardAndCustomSpecsBeforeTheLoadingEvent() throws Exception {
        Path gameDirectory = FMLPaths.GAMEDIR.get().toAbsolutePath().normalize();
        Path configDirectory = FMLPaths.CONFIGDIR.get().toAbsolutePath().normalize();
        Path manifestDirectory = configDirectory.resolve("config-migrations/migrations/neoforge/tests/startup");
        Path existingManifest = manifestDirectory.resolve("existing.toml");
        Path copiedManifest = manifestDirectory.resolve("copied.toml");
        Path stateDirectory = configDirectory.resolve("config-migrations/state");
        Path existingTarget = configDirectory.resolve(EXISTING_FILE);
        Path copiedTarget = configDirectory.resolve(COPIED_FILE);
        Path copiedDefault = gameDirectory.resolve("defaultconfigs").resolve(COPIED_FILE);
        ConfigTracker tracker = ConfigTracker.INSTANCE;

        Files.createDirectories(manifestDirectory);
        Files.createDirectories(copiedDefault.getParent());
        clean(configDirectory, stateDirectory, copiedDefault);
        SpecFixture existing = spec();
        Files.writeString(existingManifest, manifest(EXISTING_FILE));
        Files.writeString(copiedManifest, manifest(COPIED_FILE));
        writeCorrect(existingTarget, existing.spec());
        Files.writeString(copiedDefault, "[settings]\nenabled = true\ncount = 8\n");
        MigrationRuntime.resetForTests();
        MigrationRuntime.initializeLaunch(false);

        try {
            MigrationRuntime.runNeoForgeRegistrationPhase(() -> {
                ModContainer existingContainer = mock(ModContainer.class);
                when(existingContainer.getModId()).thenReturn(MOD_ID);
                tracker.registerConfig(
                        ModConfig.Type.STARTUP, existing.spec(), existingContainer, EXISTING_FILE);

                assertFalse(existing.enabled().get());
                assertFalse(read(existingTarget).<Boolean>get("settings.enabled"));
                Path backup = configDirectory.resolve("configmigrations-startup-existing-1.toml.bak");
                assertFalse(Files.exists(backup));

                NeoForgeIcebergConfigSpec copied = new NeoForgeIcebergConfigSpec();
                List<Class<?>> events = new ArrayList<>();
                ModContainer copiedContainer = mock(ModContainer.class);
                when(copiedContainer.getModId()).thenReturn(MOD_ID);
                doAnswer(invocation -> {
                    ModConfigEvent event = invocation.getArgument(0);
                    events.add(event.getClass());
                    if (event instanceof ModConfigEvent.Loading) {
                        assertFalse(copied.enabled());
                        assertFalse(read(copiedTarget).<Boolean>get("settings.enabled"));
                        assertEquals(
                                1,
                                state(gameDirectory, copiedTarget));
                    }
                    return null;
                }).when(copiedContainer).acceptEvent(any(ModConfigEvent.class));

                tracker.registerConfig(
                        ModConfig.Type.STARTUP, copied, copiedContainer, COPIED_FILE);

                assertEquals(8, copied.count());
                assertEquals(8, read(copiedTarget).<Number>get("settings.count").intValue());
                assertEquals(List.of(ModConfigEvent.Loading.class), events);
            });

            assertEquals(1, state(gameDirectory, existingTarget));
            assertEquals(1, state(gameDirectory, copiedTarget));
        } finally {
            try {
                tracker.unloadConfigs(ModConfig.Type.STARTUP);
            } finally {
                clean(configDirectory, stateDirectory, copiedDefault);
                Files.deleteIfExists(existingManifest);
                Files.deleteIfExists(copiedManifest);
                MigrationRuntime.resetForTests();
            }
        }
    }

    private static SpecFixture spec() {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        builder.push("settings");
        ModConfigSpec.BooleanValue enabled = builder.define("enabled", true);
        builder.defineInRange("count", 3, 1, 10);
        builder.pop();
        return new SpecFixture(builder.build(), enabled);
    }

    private static CommentedConfig read(Path path) {
        return TomlFormat.instance().createParser().parse(path, FileNotFoundAction.THROW_ERROR);
    }

    private static void writeCorrect(Path path, ModConfigSpec spec) {
        CommentedConfig document = CommentedConfig.inMemory();
        spec.correct(document);
        document.set("settings.enabled", true);
        document.set("settings.count", 4);
        assertTrue(spec.isCorrect(document));
        new TomlWriter().write(document, path, WritingMode.REPLACE);
    }

    private static int state(Path gameDirectory, Path target) throws IOException {
        Path relative = gameDirectory.relativize(target.toAbsolutePath().normalize());
        Path statePath = gameDirectory.resolve("config/config-migrations/state").resolve(relative);
        Path version = statePath.resolveSibling(statePath.getFileName() + ".version");
        return Integer.parseInt(Files.readString(version).strip());
    }

    private static void clean(Path configDirectory, Path stateDirectory, Path copiedDefault) throws IOException {
        deleteTree(stateDirectory);
        Files.deleteIfExists(copiedDefault);
        if (!Files.isDirectory(configDirectory)) {
            return;
        }
        try (var paths = Files.list(configDirectory)) {
            for (Path path : paths.filter(candidate -> candidate.getFileName()
                    .toString()
                    .startsWith("configmigrations-startup-")).toList()) {
                Files.deleteIfExists(path);
            }
        }
    }

    private static void deleteTree(Path root) throws IOException {
        if (!Files.exists(root)) {
            return;
        }
        try (var paths = Files.walk(root)) {
            for (Path path : paths.sorted(java.util.Comparator.reverseOrder()).toList()) {
                Files.deleteIfExists(path);
            }
        }
    }

    private static String manifest(String file) {
        return """
                mod = "%s"
                type = "STARTUP"
                file = "%s"

                [[changes]]
                version = 1
                op = "merge"

                [changes.fragment.settings]
                enabled = false
                """.formatted(MOD_ID, file);
    }

    private record SpecFixture(ModConfigSpec spec, ModConfigSpec.BooleanValue enabled) {
    }
}
