package io.github.bertie_mc.configmigrations.migration;

import io.github.bertie_mc.configmigrations.ConfigMigrationException;
import io.github.bertie_mc.configmigrations.migration.MigrationManifest.Change;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Selects pending versions and commits them after an integration succeeds. */
public final class MigrationManager {
    private static final String CONTROL_DIRECTORY = "config-migrations";

    private final MigrationStateStore state;
    private final Map<Path, Path> owners = new HashMap<>();

    private MigrationManager(MigrationStateStore state) {
        this.state = state;
    }

    public static MigrationManager load(Path gameDirectory) {
        Path normalized = gameDirectory.toAbsolutePath().normalize();
        Path stateDirectory = normalized.resolve("config")
                .resolve(CONTROL_DIRECTORY)
                .resolve("state");
        return new MigrationManager(new MigrationStateStore(normalized, stateDirectory));
    }

    public synchronized Migration prepare(MigrationManifest manifest, Path targetPath) {
        Path target = targetPath.toAbsolutePath().normalize();
        Path source = manifest.source().toAbsolutePath().normalize();
        Path owner = owners.putIfAbsent(target, source);
        if (owner != null && !owner.equals(source)) {
            throw new ConfigMigrationException("Multiple migration manifests target " + target);
        }

        int appliedVersion = state.read(target);
        List<Change> pending = manifest.changes().stream()
                .filter(change -> change.version() > appliedVersion)
                .toList();
        if (pending.isEmpty()) {
            return null;
        }
        return new Migration(target, pending.getLast().version(), pending);
    }

    public final class Migration {
        private final Path targetPath;
        private final int version;
        private final List<Change> changes;

        private Migration(Path targetPath, int version, List<Change> changes) {
            this.targetPath = targetPath;
            this.version = version;
            this.changes = List.copyOf(changes);
        }

        public List<Change> changes() {
            return changes;
        }

        public void commit() {
            synchronized (MigrationManager.this) {
                state.write(targetPath, version);
            }
        }
    }
}
