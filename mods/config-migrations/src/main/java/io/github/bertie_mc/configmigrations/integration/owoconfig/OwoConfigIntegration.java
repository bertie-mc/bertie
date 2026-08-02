package io.github.bertie_mc.configmigrations.integration.owoconfig;

import blue.endless.jankson.JsonArray;
import blue.endless.jankson.JsonElement;
import blue.endless.jankson.JsonObject;
import blue.endless.jankson.JsonPrimitive;
import com.electronwill.nightconfig.core.UnmodifiableConfig;
import io.github.bertie_mc.configmigrations.ConfigMigrationException;
import io.github.bertie_mc.configmigrations.migration.MigrationManager;
import io.github.bertie_mc.configmigrations.migration.MigrationManifest;
import io.github.bertie_mc.configmigrations.migration.MigrationManifest.Change;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Target selection and lifecycle for owo-config's Jankson-backed wrappers. */
public final class OwoConfigIntegration {
    private final MigrationManager migrations;
    private final List<Target> targets;
    private final Map<String, Session> sessions = new HashMap<>();

    private OwoConfigIntegration(MigrationManager migrations, List<Target> targets) {
        this.migrations = migrations;
        this.targets = targets;
    }

    public static OwoConfigIntegration load(MigrationManager migrations, Path manifestDirectory) {
        List<Target> targets = new ArrayList<>();
        Set<String> configs = new HashSet<>();
        for (MigrationManifest manifest : MigrationManifest.loadDirectory(manifestDirectory)) {
            String config = manifest.text("config");
            if (!configs.add(config)) {
                throw new ConfigMigrationException("Duplicate owo-config migration target " + config);
            }
            targets.add(new Target(config, manifest));
        }
        return new OwoConfigIntegration(migrations, targets);
    }

    /** Selects pending changes before owo checks whether the config file exists. */
    public synchronized boolean prepareLoad(String configName, Path path) {
        Target target = findTarget(configName);
        if (target == null) {
            sessions.remove(configName);
            return false;
        }

        MigrationManager.Migration migration = migrations.prepare(target.manifest(), path);
        if (migration == null) {
            sessions.remove(configName);
            return false;
        }

        sessions.put(configName, new Session(migration));
        return true;
    }

    /** Merges pending fragments into owo's parsed document before options consume it. */
    public synchronized Object mergeDocument(String configName, Object nativeDocument) {
        Session session = sessions.get(configName);
        if (session == null) {
            return nativeDocument;
        }

        JsonObject document = (JsonObject) nativeDocument;
        for (Change change : session.migration().changes()) {
            merge(document, change.fragment());
        }
        session.markMerged();
        return document;
    }

    /** Drops a pending session when owo's native load did not finish. */
    public synchronized void cancelLoad(String configName) {
        sessions.remove(configName);
    }

    /** Advances state after owo's native save lifecycle returns successfully. */
    public synchronized void finishLoad(String configName, Runnable nativeSave) {
        Session session = sessions.remove(configName);
        if (session == null || !session.merged()) {
            return;
        }

        nativeSave.run();
        session.migration().commit();
    }

    private Target findTarget(String configName) {
        return targets.stream()
                .filter(candidate -> candidate.config().equals(configName))
                .findFirst()
                .orElse(null);
    }

    private static void merge(JsonObject target, UnmodifiableConfig fragment) {
        for (var entry : fragment.entrySet()) {
            Object value = entry.getValue();
            if (value instanceof UnmodifiableConfig table) {
                JsonElement existing = target.get(entry.getKey());
                JsonObject child;
                if (existing instanceof JsonObject object) {
                    child = object;
                } else {
                    child = new JsonObject();
                    target.put(entry.getKey(), child);
                }
                merge(child, table);
            } else {
                target.put(entry.getKey(), toJson(value));
            }
        }
    }

    private static JsonElement toJson(Object value) {
        if (value instanceof UnmodifiableConfig table) {
            JsonObject object = new JsonObject();
            merge(object, table);
            return object;
        }
        if (value instanceof List<?> list) {
            JsonArray array = new JsonArray();
            for (Object element : list) {
                array.add(toJson(element));
            }
            return array;
        }
        if (value instanceof Boolean || value instanceof Number || value instanceof String) {
            return new JsonPrimitive(value);
        }
        throw new ConfigMigrationException("Unsupported value in an owo-config fragment");
    }

    private record Target(String config, MigrationManifest manifest) {
    }

    private static final class Session {
        private final MigrationManager.Migration migration;
        private boolean merged;

        private Session(MigrationManager.Migration migration) {
            this.migration = migration;
        }

        private MigrationManager.Migration migration() {
            return migration;
        }

        private boolean merged() {
            return merged;
        }

        private void markMerged() {
            merged = true;
        }
    }
}
