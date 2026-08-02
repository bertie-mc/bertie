package io.github.bertie_mc.configmigrations.migration;

import io.github.bertie_mc.configmigrations.ConfigMigrationException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** One tiny version file per physical config file. */
final class MigrationStateStore {
    private final Path gameDirectory;
    private final Path stateDirectory;

    MigrationStateStore(Path gameDirectory, Path stateDirectory) {
        this.gameDirectory = gameDirectory.toAbsolutePath().normalize();
        this.stateDirectory = stateDirectory.toAbsolutePath().normalize();
    }

    int read(Path configPath) {
        Path path = versionPath(configPath);
        if (!Files.exists(path)) {
            return 0;
        }
        try {
            return Integer.parseInt(Files.readString(path, StandardCharsets.UTF_8).strip());
        } catch (IOException | NumberFormatException exception) {
            throw new ConfigMigrationException("Failed to read migration state " + path, exception);
        }
    }

    void write(Path configPath, int version) {
        Path path = versionPath(configPath);
        try {
            Files.createDirectories(path.getParent());
            Files.writeString(path, version + "\n", StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new ConfigMigrationException("Failed to write migration state " + path, exception);
        }
    }

    Path versionPath(Path configPath) {
        Path target = configPath.toAbsolutePath().normalize();
        if (!target.startsWith(gameDirectory)) {
            throw new ConfigMigrationException("Config path is outside the game directory: " + target);
        }
        Path relative = gameDirectory.relativize(target);
        Path statePath = stateDirectory.resolve(relative);
        return statePath.resolveSibling(statePath.getFileName() + ".version");
    }
}
