package io.github.bertie_mc.configmigrations.integration.fzzyconfig;

import io.github.bertie_mc.configmigrations.ConfigMigrationException;
import io.github.bertie_mc.configmigrations.migration.MigrationManager;
import io.github.bertie_mc.configmigrations.migration.MigrationManifest;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

/** Applies migrations around Fzzy Config's native load and correction lifecycle. */
public final class FzzyConfigIntegration {
    private final MigrationManager migrations;
    private final Path configDirectory;
    private final List<Target> targets;
    private final ThreadLocal<Integer> nativeLoadDepth = new ThreadLocal<>();

    private FzzyConfigIntegration(
            MigrationManager migrations, Path configDirectory, List<Target> targets) {
        this.migrations = migrations;
        this.configDirectory = configDirectory.toAbsolutePath().normalize();
        this.targets = targets;
    }

    public static FzzyConfigIntegration load(
            MigrationManager migrations, Path configDirectory, Path manifestDirectory) {
        Path root = configDirectory.toAbsolutePath().normalize();
        List<Target> targets = new ArrayList<>();
        Set<Path> files = new HashSet<>();
        for (MigrationManifest manifest : MigrationManifest.loadDirectory(manifestDirectory)) {
            Path file = root.resolve(manifest.text("file")).normalize();
            if (!files.add(file)) {
                throw new ConfigMigrationException("Duplicate Fzzy Config migration target " + file);
            }
            targets.add(new Target(file, manifest));
        }
        return new FzzyConfigIntegration(migrations, root, List.copyOf(targets));
    }

    public Object runLoad(
            Object api,
            Object classInstance,
            String name,
            String folder,
            String subfolder,
            Supplier<Object> nativeLoad) {
        if (targets.isEmpty()) {
            return nativeLoad.get();
        }
        FzzyConfigFile file = FzzyConfigFile.open(
                configDirectory, api, classInstance, name, folder, subfolder);
        Target target = targets.stream()
                .filter(candidate -> candidate.file().equals(file.canonicalPath()))
                .findFirst()
                .orElse(null);
        if (target == null) {
            return nativeLoad.get();
        }

        MigrationManager.Migration migration = migrations.prepare(target.manifest(), target.file());
        if (migration == null) {
            return nativeLoad.get();
        }

        file.mergeAndWrite(migration.changes());
        Integer previousDepth = nativeLoadDepth.get();
        nativeLoadDepth.set(previousDepth == null ? 1 : previousDepth + 1);
        Object loaded;
        try {
            loaded = nativeLoad.get();
        } finally {
            if (previousDepth == null) {
                nativeLoadDepth.remove();
            } else {
                nativeLoadDepth.set(previousDepth);
            }
        }
        migration.commit();
        return loaded;
    }

    public void joinNativeWrite(CompletableFuture<Void> write) {
        if (nativeLoadDepth.get() != null) {
            write.join();
        }
    }

    private record Target(Path file, MigrationManifest manifest) {
    }
}
