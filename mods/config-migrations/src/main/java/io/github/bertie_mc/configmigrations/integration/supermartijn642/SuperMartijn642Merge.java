package io.github.bertie_mc.configmigrations.integration.supermartijn642;

import com.electronwill.nightconfig.core.UnmodifiableConfig;
import com.google.gson.JsonPrimitive;
import com.supermartijn642.configlib.ConfigFile;
import com.supermartijn642.configlib.json.JsonConfigFile;
import com.supermartijn642.configlib.toml.TomlConfigFile;
import com.supermartijn642.configlib.toml.TomlElement;
import com.supermartijn642.configlib.toml.TomlPrimitive;
import io.github.bertie_mc.configmigrations.ConfigMigrationException;
import io.github.bertie_mc.configmigrations.migration.MigrationManifest.Change;
import java.util.ArrayList;
import java.util.List;

/** Converts manifest leaves into Config Lib's two native document representations. */
final class SuperMartijn642Merge {
    private SuperMartijn642Merge() {}

    static void apply(ConfigFile<?> document, List<Change> changes) {
        if (document instanceof TomlConfigFile toml) {
            apply(changes, (path, value) -> toml.setValue(path, tomlValue(value)));
        } else if (document instanceof JsonConfigFile json) {
            apply(changes, (path, value) -> json.setValue(path, jsonValue(value)));
        } else {
            throw new ConfigMigrationException("Unsupported SuperMartijn642 config document");
        }
    }

    private static void apply(List<Change> changes, LeafWriter writer) {
        for (Change change : changes) {
            merge(change.fragment(), new ArrayList<>(), writer);
        }
    }

    private static void merge(UnmodifiableConfig fragment, List<String> path, LeafWriter writer) {
        for (var entry : fragment.entrySet()) {
            path.add(entry.getKey());
            if (entry.getValue() instanceof UnmodifiableConfig child) {
                merge(child, path, writer);
            } else {
                writer.set(path.toArray(String[]::new), entry.getValue());
            }
            path.removeLast();
        }
    }

    private static TomlElement tomlValue(Object value) {
        if (value instanceof Boolean bool) {
            return TomlPrimitive.of(bool);
        }
        if (value instanceof Integer integer) {
            return TomlPrimitive.of(integer);
        }
        if (value instanceof Long longValue) {
            return TomlPrimitive.of(longValue);
        }
        if (value instanceof Double doubleValue) {
            return TomlPrimitive.of(doubleValue);
        }
        if (value instanceof String string) {
            return TomlPrimitive.of(string);
        }
        throw new ConfigMigrationException("Unsupported value in a SuperMartijn642 config fragment");
    }

    private static JsonPrimitive jsonValue(Object value) {
        if (value instanceof Boolean bool) {
            return new JsonPrimitive(bool);
        }
        if (value instanceof Number number) {
            return new JsonPrimitive(number);
        }
        if (value instanceof String string) {
            return new JsonPrimitive(string);
        }
        throw new ConfigMigrationException("Unsupported value in a SuperMartijn642 config fragment");
    }

    @FunctionalInterface
    private interface LeafWriter {
        void set(String[] path, Object value);
    }
}
