package io.github.bertie_mc.configmigrations.migration;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.core.UnmodifiableConfig;
import io.github.bertie_mc.configmigrations.migration.MigrationManifest.Change;
import java.util.ArrayList;
import java.util.List;

/** Recursive merge shared by NightConfig-based integrations. */
public final class NightConfigMerge {
    private NightConfigMerge() {}

    public static void apply(CommentedConfig target, List<Change> changes) {
        for (Change change : changes) {
            merge(target, change.fragment());
        }
    }

    private static void merge(CommentedConfig target, UnmodifiableConfig fragment) {
        for (var entry : fragment.entrySet()) {
            List<String> path = List.of(entry.getKey());
            Object value = entry.getValue();
            if (value instanceof UnmodifiableConfig table) {
                Object existing = target.getRaw(path);
                CommentedConfig child;
                if (existing instanceof CommentedConfig config) {
                    child = config;
                } else {
                    child = target.createSubConfig();
                    target.set(path, child);
                }
                merge(child, table);
            } else {
                target.set(path, copy(target, value));
            }
        }
    }

    private static Object copy(CommentedConfig target, Object value) {
        if (value instanceof UnmodifiableConfig table) {
            CommentedConfig copy = target.createSubConfig();
            merge(copy, table);
            return copy;
        }
        if (value instanceof List<?> list) {
            List<Object> copy = new ArrayList<>(list.size());
            for (Object element : list) {
                copy.add(copy(target, element));
            }
            return copy;
        }
        return value;
    }
}
