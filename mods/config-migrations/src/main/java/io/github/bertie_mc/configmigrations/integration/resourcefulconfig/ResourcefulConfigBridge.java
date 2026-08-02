package io.github.bertie_mc.configmigrations.integration.resourcefulconfig;

import com.electronwill.nightconfig.core.UnmodifiableConfig;
import com.teamresourceful.resourcefulconfig.api.types.ResourcefulConfig;
import com.teamresourceful.resourcefulconfig.api.types.entries.ResourcefulConfigEntry;
import com.teamresourceful.resourcefulconfig.api.types.entries.ResourcefulConfigObjectEntry;
import com.teamresourceful.resourcefulconfig.api.types.entries.ResourcefulConfigValueEntry;
import com.teamresourceful.resourcefulconfig.common.config.ParsingUtils;
import com.teamresourceful.resourcefulconfig.common.loader.ParsedConfig;
import io.github.bertie_mc.configmigrations.ConfigMigrationException;
import io.github.bertie_mc.configmigrations.migration.MigrationManager;
import io.github.bertie_mc.configmigrations.migration.MigrationManifest;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

/** Isolates optional Resourceful Config API linkage from the migration catalog. */
final class ResourcefulConfigBridge {
    private ResourcefulConfigBridge() {
    }

    static void migrate(
            MigrationManager migrations,
            MigrationManifest manifest,
            Path file,
            Object nativeConfig,
            Runnable nativeSave) {
        if (!(nativeConfig instanceof ParsedConfig config)) {
            nativeSave.run();
            return;
        }
        if (Files.exists(legacyFile(file))) {
            nativeSave.run();
            return;
        }

        MigrationManager.Migration migration = migrations.prepare(manifest, file);
        if (migration == null) {
            nativeSave.run();
            return;
        }

        for (MigrationManifest.Change change : migration.changes()) {
            mergeConfig(config, change.fragment(), new ArrayList<>());
        }

        nativeSave.run();
        migration.commit();
    }

    private static void mergeConfig(
            ResourcefulConfig config, UnmodifiableConfig fragment, List<String> path) {
        merge(fragment, path, config.entries(), config.categories());
    }

    private static void mergeObject(
            ResourcefulConfigObjectEntry object,
            UnmodifiableConfig fragment,
            List<String> path) {
        merge(fragment, path, object.entries(), new LinkedHashMap<>());
    }

    private static void merge(
            UnmodifiableConfig fragment,
            List<String> path,
            LinkedHashMap<String, ResourcefulConfigEntry> entries,
            LinkedHashMap<String, ResourcefulConfig> categories) {
        for (var fragmentEntry : fragment.entrySet()) {
            String name = fragmentEntry.getKey();
            Object value = fragmentEntry.getValue();
            ResourcefulConfigEntry entry = entries.get(name);
            path.add(name);
            if (value instanceof UnmodifiableConfig child) {
                if (entry instanceof ResourcefulConfigObjectEntry object) {
                    mergeObject(object, child, path);
                } else {
                    ResourcefulConfig category = categories.get(name);
                    if (category == null) {
                        throw unknown(path);
                    }
                    mergeConfig(category, child, path);
                }
            } else if (entry instanceof ResourcefulConfigValueEntry nativeValue) {
                set(nativeValue, value, path);
            } else {
                throw unknown(path);
            }
            path.removeLast();
        }
    }

    private static void set(
            ResourcefulConfigValueEntry entry, Object value, List<String> path) {
        if (value instanceof List<?> values) {
            Object[] converted = new Object[values.size()];
            for (int index = 0; index < values.size(); index++) {
                converted[index] = scalar(entry, values.get(index), path);
            }
            entry.setArray(converted);
            return;
        }
        switch (entry.type()) {
            case BYTE -> entry.setByte(number(value, path).byteValue());
            case SHORT -> entry.setShort(number(value, path).shortValue());
            case INTEGER -> entry.setInt(number(value, path).intValue());
            case LONG -> entry.setLong(number(value, path).longValue());
            case FLOAT -> entry.setFloat(number(value, path).floatValue());
            case DOUBLE -> entry.setDouble(number(value, path).doubleValue());
            case BOOLEAN -> entry.setBoolean(booleanValue(value, path));
            case STRING -> entry.setString(string(value, path));
            case ENUM -> entry.setEnum(enumValue(entry, value, path));
            default -> throw unsupported(path);
        }
    }

    private static Object scalar(
            ResourcefulConfigValueEntry entry, Object value, List<String> path) {
        return switch (entry.type()) {
            case BYTE -> number(value, path).byteValue();
            case SHORT -> number(value, path).shortValue();
            case INTEGER -> number(value, path).intValue();
            case LONG -> number(value, path).longValue();
            case FLOAT -> number(value, path).floatValue();
            case DOUBLE -> number(value, path).doubleValue();
            case BOOLEAN -> booleanValue(value, path);
            case STRING -> string(value, path);
            case ENUM -> enumValue(entry, value, path);
            default -> throw unsupported(path);
        };
    }

    private static Number number(Object value, List<String> path) {
        if (value instanceof Number number) {
            return number;
        }
        throw unsupported(path);
    }

    private static boolean booleanValue(Object value, List<String> path) {
        if (value instanceof Boolean bool) {
            return bool;
        }
        throw unsupported(path);
    }

    private static String string(Object value, List<String> path) {
        if (value instanceof String string) {
            return string;
        }
        throw unsupported(path);
    }

    private static Enum<?> enumValue(
            ResourcefulConfigValueEntry entry, Object value, List<String> path) {
        Enum<?> parsed = ParsingUtils.parseEnum(entry.objectType(), string(value, path));
        if (parsed != null) {
            return parsed;
        }
        throw unsupported(path);
    }

    private static Path legacyFile(Path jsoncFile) {
        String name = jsoncFile.getFileName().toString();
        return jsoncFile.resolveSibling(name.substring(0, name.length() - 1));
    }

    private static ConfigMigrationException unknown(List<String> path) {
        return new ConfigMigrationException(
                "Unknown Resourceful Config setting " + String.join(".", path));
    }

    private static ConfigMigrationException unsupported(List<String> path) {
        return new ConfigMigrationException(
                "Unsupported value for Resourceful Config setting " + String.join(".", path));
    }
}
