package io.github.bertie_mc.configmigrations.integration.wunderlib;

import com.electronwill.nightconfig.core.UnmodifiableConfig;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import io.github.bertie_mc.configmigrations.ConfigMigrationException;
import io.github.bertie_mc.configmigrations.migration.MigrationManager;
import io.github.bertie_mc.configmigrations.migration.MigrationManifest;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;

/** Applies JSON fragments as a WunderLib {@code ConfigFile} finishes loading. */
public final class WunderLibIntegration {
    private final MigrationManager migrations;
    private final List<Target> targets;

    private WunderLibIntegration(MigrationManager migrations, List<Target> targets) {
        this.migrations = migrations;
        this.targets = targets;
    }

    public static WunderLibIntegration load(MigrationManager migrations, Path directory) {
        List<Target> targets = new ArrayList<>();
        Set<ResourceLocation> ids = new HashSet<>();
        for (MigrationManifest manifest : MigrationManifest.loadDirectory(directory)) {
            ResourceLocation id = ResourceLocation.parse(manifest.text("id"));
            if (!ids.add(id)) {
                throw new ConfigMigrationException("Duplicate WunderLib migration target " + id);
            }
            targets.add(new Target(id, manifest));
        }
        return new WunderLibIntegration(migrations, targets);
    }

    public void migrate(
            ResourceLocation id,
            Path path,
            JsonObject document,
            Runnable nativeSave) {
        Target target = targets.stream()
                .filter(candidate -> candidate.id().equals(id))
                .findFirst()
                .orElse(null);
        if (target == null) {
            return;
        }

        MigrationManager.Migration migration = migrations.prepare(target.manifest(), path);
        if (migration == null) {
            return;
        }

        for (MigrationManifest.Change change : migration.changes()) {
            merge(document, change.fragment());
        }
        nativeSave.run();
        migration.commit();
    }

    private static void merge(JsonObject target, UnmodifiableConfig fragment) {
        for (var entry : fragment.entrySet()) {
            Object value = entry.getValue();
            JsonElement current = target.get(entry.getKey());
            if (current instanceof JsonObject currentObject
                    && value instanceof UnmodifiableConfig fragmentObject) {
                merge(currentObject, fragmentObject);
            } else {
                target.add(entry.getKey(), json(value));
            }
        }
    }

    private static JsonElement json(Object value) {
        if (value instanceof UnmodifiableConfig config) {
            JsonObject object = new JsonObject();
            merge(object, config);
            return object;
        }
        if (value instanceof List<?> list) {
            JsonArray array = new JsonArray();
            list.forEach(item -> array.add(json(item)));
            return array;
        }
        if (value instanceof String string) {
            return new JsonPrimitive(string);
        }
        if (value instanceof Number number) {
            return new JsonPrimitive(number);
        }
        if (value instanceof Boolean bool) {
            return new JsonPrimitive(bool);
        }
        throw new ConfigMigrationException("Unsupported WunderLib migration value " + value);
    }

    private record Target(ResourceLocation id, MigrationManifest manifest) {
    }
}
