package io.github.bertie_mc.configmigrations.integration.supermartijn642;

import com.supermartijn642.configlib.ConfigFile;
import io.github.bertie_mc.configmigrations.ConfigMigrationException;
import io.github.bertie_mc.configmigrations.migration.MigrationManager;
import io.github.bertie_mc.configmigrations.migration.MigrationManifest;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Target selection and lifecycle for SuperMartijn642's Config Lib. */
public final class SuperMartijn642Integration {
    private final MigrationManager migrations;
    private final Path configDirectory;
    private final List<Target> targets;
    private final Map<Selector, MigrationManager.Migration> pending = new HashMap<>();

    private SuperMartijn642Integration(MigrationManager migrations, Path configDirectory, List<Target> targets) {
        this.migrations = migrations;
        this.configDirectory = configDirectory;
        this.targets = targets;
    }

    public static SuperMartijn642Integration load(
            MigrationManager migrations, Path configDirectory, Path manifestDirectory) {
        List<Target> targets = new ArrayList<>();
        Set<Selector> selectors = new HashSet<>();
        for (MigrationManifest manifest : MigrationManifest.loadDirectory(manifestDirectory)) {
            Target target = new Target(manifest.text("mod"), manifest.text("file"), manifest);
            if (!selectors.add(target.selector())) {
                throw new ConfigMigrationException("Duplicate SuperMartijn642 migration target " + target.selector());
            }
            targets.add(target);
        }
        return new SuperMartijn642Integration(migrations, configDirectory, targets);
    }

    /** Applies pending fragments after the owner parses its file and before it reads live values. */
    public synchronized void apply(String modId, String identifier, Object nativeDocument) {
        ConfigFile<?> document = (ConfigFile<?>) nativeDocument;
        Target target = findTarget(modId, identifier);
        if (target == null) {
            return;
        }

        MigrationManager.Migration migration =
                migrations.prepare(target.manifest(), configDirectory.resolve(identifier));
        if (migration == null) {
            return;
        }

        SuperMartijn642Merge.apply(document, migration.changes());
        pending.put(new Selector(modId, identifier), migration);
    }

    /** Advances state after the owner's native save returns. */
    public synchronized void commit(String modId, String identifier) {
        MigrationManager.Migration migration = pending.remove(new Selector(modId, identifier));
        if (migration != null) {
            migration.commit();
        }
    }

    private Target findTarget(String modId, String identifier) {
        return targets.stream()
                .filter(candidate -> candidate.matches(modId, identifier))
                .findFirst()
                .orElse(null);
    }

    private record Target(String modId, String file, MigrationManifest manifest) {
        private Selector selector() {
            return new Selector(modId, file);
        }

        private boolean matches(String candidateModId, String candidateFile) {
            return modId.equals(candidateModId) && file.equals(candidateFile);
        }
    }

    private record Selector(String modId, String file) {}
}
