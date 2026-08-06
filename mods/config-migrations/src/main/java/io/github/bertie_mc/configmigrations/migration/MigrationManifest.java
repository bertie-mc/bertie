package io.github.bertie_mc.configmigrations.migration;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.core.UnmodifiableConfig;
import com.electronwill.nightconfig.core.file.FileNotFoundAction;
import com.electronwill.nightconfig.toml.TomlFormat;
import io.github.bertie_mc.configmigrations.ConfigMigrationException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** One integration-specific target and its ordered, versioned changes. */
public record MigrationManifest(Path source, UnmodifiableConfig settings, List<Change> changes) {
    public MigrationManifest {
        changes = List.copyOf(changes);
    }

    public static List<MigrationManifest> loadDirectory(Path directory) {
        if (!Files.isDirectory(directory)) {
            return List.of();
        }

        try (var paths = Files.walk(directory)) {
            return paths.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(".toml"))
                    .sorted()
                    .map(MigrationManifest::load)
                    .toList();
        } catch (IOException exception) {
            throw new ConfigMigrationException("Failed to discover migration manifests in " + directory, exception);
        }
    }

    public static MigrationManifest load(Path path) {
        try {
            CommentedConfig root = TomlFormat.instance().createParser().parse(path, FileNotFoundAction.THROW_ERROR);
            List<Change> changes = new ArrayList<>();
            int previousVersion = 0;
            for (UnmodifiableConfig entry : tables(root, "changes")) {
                String operation = text(entry, "op");
                if (!operation.equals("merge")) {
                    throw new ConfigMigrationException("Unknown config migration operation '" + operation + "'");
                }
                Object fragment = entry.getRaw("fragment");
                if (!(fragment instanceof UnmodifiableConfig config)) {
                    throw new ConfigMigrationException("A merge change requires a fragment table");
                }
                int version = number(entry, "version");
                if (version <= previousVersion) {
                    throw new ConfigMigrationException("Change versions must be positive and increasing");
                }
                changes.add(new Change(version, config));
                previousVersion = version;
            }
            return new MigrationManifest(path, root, changes);
        } catch (RuntimeException exception) {
            if (exception instanceof ConfigMigrationException migrationException) {
                throw migrationException;
            }
            throw new ConfigMigrationException("Failed to read migration manifest " + path, exception);
        }
    }

    public String text(String key) {
        return text(settings, key);
    }

    private static List<? extends UnmodifiableConfig> tables(UnmodifiableConfig config, String key) {
        Object value = config.getRaw(key);
        if (value == null) {
            return List.of();
        }
        if (!(value instanceof List<?> list)) {
            throw new ConfigMigrationException(key + " must be an array of tables");
        }
        return list.stream().map(UnmodifiableConfig.class::cast).toList();
    }

    private static String text(UnmodifiableConfig config, String key) {
        Object value = config.getRaw(key);
        if (!(value instanceof String text)) {
            throw new ConfigMigrationException(key + " must be a string");
        }
        return text;
    }

    private static int number(UnmodifiableConfig config, String key) {
        return ((Number) config.getRaw(key)).intValue();
    }

    public record Change(int version, UnmodifiableConfig fragment) {}
}
