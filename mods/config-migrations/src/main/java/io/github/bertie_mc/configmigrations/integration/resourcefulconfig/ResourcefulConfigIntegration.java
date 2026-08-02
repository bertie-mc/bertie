package io.github.bertie_mc.configmigrations.integration.resourcefulconfig;

import io.github.bertie_mc.configmigrations.ConfigMigrationException;
import io.github.bertie_mc.configmigrations.migration.MigrationManager;
import io.github.bertie_mc.configmigrations.migration.MigrationManifest;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/** Selects migrations for Resourceful Config's standard JSONC-backed configs. */
public final class ResourcefulConfigIntegration {
    private final MigrationManager migrations;
    private final Map<Selector, Target> targets;

    private ResourcefulConfigIntegration(
            MigrationManager migrations, Map<Selector, Target> targets) {
        this.migrations = migrations;
        this.targets = targets;
    }

    public static ResourcefulConfigIntegration load(
            MigrationManager migrations, Path directory, Path configDirectory) {
        Map<Selector, Target> targets = new HashMap<>();
        for (MigrationManifest manifest : MigrationManifest.loadDirectory(directory)) {
            Selector selector = new Selector(manifest.text("mod"), manifest.text("config"));
            Target target = new Target(
                    configDirectory.resolve(selector.config() + ".jsonc"), manifest);
            if (targets.putIfAbsent(selector, target) != null) {
                throw new ConfigMigrationException(
                        "Duplicate Resourceful Config migration target " + selector);
            }
        }
        return new ResourcefulConfigIntegration(migrations, Map.copyOf(targets));
    }

    /** Runs at Configurator's initial load-to-save boundary. */
    public void migrate(
            String modId,
            String configId,
            Object nativeConfig,
            Runnable nativeSave) {
        Target target = targets.get(new Selector(modId, configId));
        if (target == null) {
            nativeSave.run();
            return;
        }
        ResourcefulConfigBridge.migrate(
                migrations, target.manifest(), target.file(), nativeConfig, nativeSave);
    }

    private record Selector(String mod, String config) {
    }

    private record Target(Path file, MigrationManifest manifest) {
    }
}
