package com.teamresourceful.resourcefulconfig.common.loader;

import com.teamresourceful.resourcefulconfig.api.types.ResourcefulConfig;
import com.teamresourceful.resourcefulconfig.api.types.entries.ResourcefulConfigEntry;
import com.teamresourceful.resourcefulconfig.api.types.entries.ResourcefulConfigObjectEntry;
import com.teamresourceful.resourcefulconfig.api.types.entries.ResourcefulConfigValueEntry;
import com.teamresourceful.resourcefulconfig.common.jsonc.JsoncObject;
import java.lang.reflect.Array;
import java.util.Map;

/** Deterministic stand-in for Resourceful Config's native writer. */
public final class Writer {
    private Writer() {
    }

    public static JsoncObject save(ResourcefulConfig config) {
        StringBuilder output = new StringBuilder();
        writeConfig(output, config);
        return new JsoncObject(output.toString());
    }

    private static void writeConfig(StringBuilder output, ResourcefulConfig config) {
        output.append('{');
        boolean first = true;
        for (var entry : config.entries().entrySet()) {
            first = separator(output, first);
            writeEntry(output, entry.getKey(), entry.getValue());
        }
        for (var category : config.categories().entrySet()) {
            first = separator(output, first);
            string(output, category.getKey());
            output.append(':');
            writeConfig(output, category.getValue());
        }
        output.append('}');
    }

    private static void writeEntries(
            StringBuilder output, Map<String, ResourcefulConfigEntry> entries) {
        output.append('{');
        boolean first = true;
        for (var entry : entries.entrySet()) {
            first = separator(output, first);
            writeEntry(output, entry.getKey(), entry.getValue());
        }
        output.append('}');
    }

    private static void writeEntry(
            StringBuilder output, String name, ResourcefulConfigEntry entry) {
        string(output, name);
        output.append(':');
        if (entry instanceof ResourcefulConfigObjectEntry object) {
            writeEntries(output, object.entries());
        } else {
            value(output, ((ResourcefulConfigValueEntry) entry).get());
        }
    }

    private static void value(StringBuilder output, Object value) {
        if (value == null) {
            output.append("null");
        } else if (value instanceof String || value instanceof Enum<?>) {
            string(output, value instanceof Enum<?> enumeration ? enumeration.name() : value.toString());
        } else if (value instanceof Number || value instanceof Boolean) {
            output.append(value);
        } else if (value.getClass().isArray()) {
            output.append('[');
            for (int index = 0; index < Array.getLength(value); index++) {
                if (index != 0) {
                    output.append(',');
                }
                value(output, Array.get(value, index));
            }
            output.append(']');
        } else {
            throw new IllegalArgumentException("Unsupported fixture value " + value);
        }
    }

    private static boolean separator(StringBuilder output, boolean first) {
        if (!first) {
            output.append(',');
        }
        return false;
    }

    private static void string(StringBuilder output, String value) {
        output.append('"').append(value.replace("\"", "\\\"")).append('"');
    }
}
