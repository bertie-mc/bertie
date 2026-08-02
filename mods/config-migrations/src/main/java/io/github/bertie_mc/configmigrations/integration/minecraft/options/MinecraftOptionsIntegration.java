package io.github.bertie_mc.configmigrations.integration.minecraft.options;

import com.electronwill.nightconfig.core.UnmodifiableConfig;
import io.github.bertie_mc.configmigrations.ConfigMigrationException;
import io.github.bertie_mc.configmigrations.migration.MigrationManager;
import io.github.bertie_mc.configmigrations.migration.MigrationManifest;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import net.minecraft.SharedConstants;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;

/** Flat merge and persistence for Minecraft's root-level options file. */
public final class MinecraftOptionsIntegration {
    private static final String OPTIONS_FILE = "options.txt";

    private final MigrationManager migrations;
    private final MigrationManifest manifest;

    private MinecraftOptionsIntegration(MigrationManager migrations, MigrationManifest manifest) {
        this.migrations = migrations;
        this.manifest = manifest;
    }

    public static MinecraftOptionsIntegration load(Path gameDirectory) {
        Path manifests = gameDirectory.resolve("config/config-migrations/migrations/minecraft");
        MigrationManifest options = null;
        for (MigrationManifest candidate : MigrationManifest.loadDirectory(manifests)) {
            if (!OPTIONS_FILE.equals(candidate.text("file"))) {
                continue;
            }
            if (options != null) {
                throw new ConfigMigrationException("Duplicate Minecraft migration target " + OPTIONS_FILE);
            }
            options = candidate;
        }
        return new MinecraftOptionsIntegration(MigrationManager.load(gameDirectory), options);
    }

    public PendingMigration prepare(Path optionsFile) {
        if (manifest == null) {
            return null;
        }

        MigrationManager.Migration migration = migrations.prepare(manifest, optionsFile);
        if (migration == null) {
            return null;
        }

        if (!Files.exists(optionsFile)) {
            try {
                Files.createFile(optionsFile);
            } catch (IOException exception) {
                throw new ConfigMigrationException("Failed to create " + optionsFile, exception);
            }
        }
        return new PendingMigration(optionsFile, migration);
    }

    public static final class PendingMigration {
        private final Path optionsFile;
        private final MigrationManager.Migration migration;
        private RuntimeException failure;

        private PendingMigration(Path optionsFile, MigrationManager.Migration migration) {
            this.optionsFile = optionsFile;
            this.migration = migration;
        }

        public void apply(CompoundTag options) {
            try {
                for (MigrationManifest.Change change : migration.changes()) {
                    merge(options, change.fragment());
                }
                options.putString(
                        "version",
                        Integer.toString(SharedConstants.getCurrentVersion().getDataVersion().getVersion()));
                write(options);
            } catch (RuntimeException exception) {
                failure = exception;
                throw exception;
            }
        }

        public RuntimeException failure() {
            return failure;
        }

        public void commit() {
            migration.commit();
        }

        private static void merge(CompoundTag options, UnmodifiableConfig fragment) {
            for (var entry : fragment.entrySet()) {
                options.putString(entry.getKey(), rawValue(entry.getValue()));
            }
        }

        private static String rawValue(Object value) {
            if (value instanceof String text) {
                return text;
            }
            if (value instanceof Number || value instanceof Boolean) {
                return value.toString();
            }
            throw new ConfigMigrationException(
                    "Minecraft option fragments must contain only flat string, number, or boolean values");
        }

        private void write(CompoundTag options) {
            StringBuilder contents = new StringBuilder();
            options.getAllKeys().stream().sorted().forEach(key -> {
                Tag value = options.get(key);
                if (value != null) {
                    contents.append(key).append(':').append(value.getAsString()).append('\n');
                }
            });
            try {
                Files.writeString(optionsFile, contents, StandardCharsets.UTF_8);
            } catch (IOException exception) {
                throw new ConfigMigrationException("Failed to write " + optionsFile, exception);
            }
        }
    }
}
