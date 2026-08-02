package io.github.bertie_mc.configmigrations.integration.artifacts;

import com.electronwill.nightconfig.core.ConfigSpec;
import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import io.github.bertie_mc.configmigrations.ConfigMigrationException;
import io.github.bertie_mc.configmigrations.migration.MigrationManager;
import io.github.bertie_mc.configmigrations.migration.MigrationManifest;
import io.github.bertie_mc.configmigrations.migration.NightConfigMerge;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Target selection and lifecycle for Artifacts' custom config managers. */
public final class ArtifactsIntegration {
    private final MigrationManager migrations;
    private final List<Target> targets;

    private ArtifactsIntegration(MigrationManager migrations, List<Target> targets) {
        this.migrations = migrations;
        this.targets = targets;
    }

    public static ArtifactsIntegration load(MigrationManager migrations, Path directory) {
        List<Target> targets = new ArrayList<>();
        Set<String> configs = new HashSet<>();
        for (MigrationManifest manifest : MigrationManifest.loadDirectory(directory)) {
            String config = manifest.text("config");
            if (!configs.add(config)) {
                throw new ConfigMigrationException("Duplicate Artifacts migration target " + config);
            }
            targets.add(new Target(config, manifest));
        }
        return new ArtifactsIntegration(migrations, targets);
    }

    public void migrate(String configName, CommentedFileConfig document, ConfigSpec spec) {
        Target target = targets.stream()
                .filter(candidate -> candidate.config().equals(configName))
                .findFirst()
                .orElse(null);
        if (target == null) {
            return;
        }

        MigrationManager.Migration migration = migrations.prepare(target.manifest(), document.getNioPath());
        if (migration == null) {
            return;
        }

        NightConfigMerge.apply(document, migration.changes());
        spec.correct(document);
        document.save();
        migration.commit();
    }

    private record Target(String config, MigrationManifest manifest) {
    }
}
