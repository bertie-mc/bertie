package io.github.bertie_mc.configmigrations.integration.autoconfig;

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

/** Applies migrations to AutoConfig's logical data before its native validation and save. */
public final class AutoConfigIntegration {
    private final MigrationManager migrations;
    private final Map<String, List<Target>> targets;

    private AutoConfigIntegration(MigrationManager migrations, Map<String, List<Target>> targets) {
        this.migrations = migrations;
        this.targets = targets;
    }

    public static AutoConfigIntegration load(
            MigrationManager migrations, Path directory, Path configDirectory) {
        Map<String, List<Target>> targets = new HashMap<>();
        Set<Selector> selectors = new HashSet<>();
        for (MigrationManifest manifest : MigrationManifest.loadDirectory(directory)) {
            String config = manifest.text("config");
            Object partitionValue = manifest.settings().getRaw("partition");
            String partition = partitionValue == null ? null : (String) partitionValue;
            Selector selector = new Selector(config, partition);
            if (!selectors.add(selector)) {
                throw new ConfigMigrationException("Duplicate AutoConfig migration target " + selector);
            }

            Path file = configDirectory.resolve(manifest.text("file"));
            targets.computeIfAbsent(config, ignored -> new ArrayList<>())
                    .add(new Target(partition, file, manifest));
        }
        targets.replaceAll((ignored, value) -> List.copyOf(value));
        return new AutoConfigIntegration(migrations, Map.copyOf(targets));
    }

    public Object wrap(String configName, Object serializer) {
        List<Target> matching = targets.get(configName);
        if (matching == null) {
            return serializer;
        }
        return AutoConfigSerializerBridge.wrap(migrations, matching, serializer);
    }

    private record Selector(String config, String partition) {
        @Override
        public String toString() {
            return partition == null ? config : config + "/" + partition;
        }
    }

    record Target(String partition, Path file, MigrationManifest manifest) {
    }
}
