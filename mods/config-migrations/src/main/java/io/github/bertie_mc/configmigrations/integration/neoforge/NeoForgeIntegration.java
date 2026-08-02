package io.github.bertie_mc.configmigrations.integration.neoforge;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.core.io.WritingMode;
import com.electronwill.nightconfig.toml.TomlWriter;
import io.github.bertie_mc.configmigrations.ConfigMigrationException;
import io.github.bertie_mc.configmigrations.migration.MigrationManager;
import io.github.bertie_mc.configmigrations.migration.MigrationManifest;
import io.github.bertie_mc.configmigrations.migration.NightConfigMerge;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.neoforged.fml.config.IConfigSpec;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.config.ModConfigs;

/** Target selection and lifecycle for bridged configs registered through NeoForge. */
public final class NeoForgeIntegration {
    private final MigrationManager migrations;
    private final List<Target> targets;
    private volatile Phase activePhase;

    private NeoForgeIntegration(MigrationManager migrations, List<Target> targets) {
        this.migrations = migrations;
        this.targets = targets;
    }

    public static NeoForgeIntegration load(MigrationManager migrations, Path directory) {
        List<Target> targets = new ArrayList<>();
        Set<Selector> selectors = new HashSet<>();
        for (MigrationManifest manifest : MigrationManifest.loadDirectory(directory)) {
            Target target = new Target(
                    manifest.text("mod"),
                    ModConfig.Type.valueOf(manifest.text("type")),
                    manifest.text("file"),
                    manifest);
            if (!selectors.add(target.selector())) {
                throw new ConfigMigrationException("Duplicate NeoForge migration target " + target.selector());
            }
            targets.add(target);
        }
        return new NeoForgeIntegration(migrations, targets);
    }

    public void runRegistrationPhase(Runnable nativeGather) {
        run(new Phase(ModConfig.Type.STARTUP), nativeGather);
    }

    public void runLoadPhase(ModConfig.Type type, Runnable nativeLoad) {
        Phase phase = new Phase(type);
        phase.selectRegisteredConfigs();
        run(phase, nativeLoad);
    }

    public void registerSpec(IConfigSpec spec, ModConfig config) {
        Phase phase = activePhase;
        if (phase != null && config.getType() == ModConfig.Type.STARTUP) {
            phase.select(spec, config);
        }
    }

    public void accept(
            IConfigSpec spec,
            IConfigSpec.ILoadedConfig loadedConfig,
            Runnable nativeAcceptance) {
        Selection selection = currentSelection(spec);
        MigrationManager.Migration migration = null;
        if (selection != null) {
            Path path = selection.config().getFullPath();
            migration = migrations.prepare(selection.target().manifest(), path);
            if (migration != null) {
                CommentedConfig document = loadedConfig.config();
                NightConfigMerge.apply(document, migration.changes());
                spec.correct(document);
                new TomlWriter().write(document, path, WritingMode.REPLACE_ATOMIC);
            }
        }
        nativeAcceptance.run();
        if (migration != null) {
            migration.commit();
        }
    }

    private void run(Phase phase, Runnable nativeAction) {
        activePhase = phase;
        try {
            nativeAction.run();
        } finally {
            activePhase = null;
        }
    }

    private Selection currentSelection(IConfigSpec spec) {
        Phase phase = activePhase;
        return phase == null ? null : phase.selections.get(spec);
    }

    private Target findTarget(ModConfig config) {
        return targets.stream()
                .filter(candidate -> candidate.matches(config.getModId(), config.getType(), config.getFileName()))
                .findFirst()
                .orElse(null);
    }

    private final class Phase {
        private final ModConfig.Type type;
        private final Map<IConfigSpec, Selection> selections = new ConcurrentHashMap<>();

        private Phase(ModConfig.Type type) {
            this.type = type;
        }

        private void selectRegisteredConfigs() {
            for (ModConfig config : List.copyOf(ModConfigs.getConfigSet(type))) {
                select(config.getSpec(), config);
            }
        }

        private void select(IConfigSpec spec, ModConfig config) {
            Target target = findTarget(config);
            if (target != null) {
                selections.put(spec, new Selection(config, target));
            }
        }
    }

    private record Selection(ModConfig config, Target target) {
    }

    private record Target(
            String modId,
            ModConfig.Type type,
            String file,
            MigrationManifest manifest) {
        private Selector selector() {
            return new Selector(modId, type, file);
        }

        private boolean matches(String candidateModId, ModConfig.Type candidateType, String candidateFile) {
            return modId.equals(candidateModId) && type == candidateType && file.equals(candidateFile);
        }
    }

    private record Selector(String modId, ModConfig.Type type, String file) {
    }
}
