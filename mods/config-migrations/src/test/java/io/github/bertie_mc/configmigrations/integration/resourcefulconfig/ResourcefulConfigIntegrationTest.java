package io.github.bertie_mc.configmigrations.integration.resourcefulconfig;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import com.teamresourceful.resourcefulconfig.api.types.ResourcefulConfig;
import com.teamresourceful.resourcefulconfig.api.types.entries.ResourcefulConfigEntry;
import com.teamresourceful.resourcefulconfig.api.types.entries.ResourcefulConfigObjectEntry;
import com.teamresourceful.resourcefulconfig.api.types.entries.ResourcefulConfigValueEntry;
import com.teamresourceful.resourcefulconfig.api.types.options.EntryType;
import com.teamresourceful.resourcefulconfig.common.loader.ParsedConfig;
import com.teamresourceful.resourcefulconfig.common.loader.Writer;
import io.github.bertie_mc.configmigrations.migration.MigrationManager;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.function.Predicate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ResourcefulConfigIntegrationTest {
    @TempDir
    Path gameDirectory;

    @Test
    void nativeEntriesAreMergedAndStateCommitsAfterNativeSave() throws Exception {
        ResourcefulConfigIntegration integration = integration(manifest());
        Path file = gameDirectory.resolve("config/sample.jsonc");
        ParsedConfig config = new ParsedConfig("sample", file);
        TestValueEntry count = scalar(EntryType.INTEGER, 1);
        TestValueEntry values = array(EntryType.INTEGER, new Integer[] {1});
        TestValueEntry mode = scalar(EntryType.ENUM, Mode.FAST);
        TestValueEntry enabled = scalar(EntryType.BOOLEAN, true);
        TestValueEntry scale = scalar(EntryType.DOUBLE, 1.0d);
        config.entries().put("count", count);
        config.entries().put("values", values);
        config.entries().put("mode", mode);
        config.entries().put("details", object("enabled", enabled));
        config.categories().put("graphics", node("graphics", "scale", scale));

        integration.migrate("samplemod", "sample", config, config::save);

        assertEquals(7, count.get());
        assertArrayEquals(new Integer[] {2, 3}, (Object[]) values.get());
        assertEquals(Mode.SLOW, mode.get());
        assertEquals(false, enabled.get());
        assertEquals(0.75d, scale.get());
        assertEquals(1, config.saves());
        assertEquals(Writer.save(config).toString(), Files.readString(file));
        assertEquals("4", Files.readString(state()).strip());
    }

    @Test
    void nativeSetterRejectionPersistsAndCommitsTheCanonicalValue() throws Exception {
        ResourcefulConfigIntegration integration = integration("""
                mod = "samplemod"
                config = "sample"

                [[changes]]
                version = 2
                op = "merge"
                [changes.fragment]
                count = 9
                """);
        ParsedConfig config = new ParsedConfig("sample", gameDirectory.resolve("config/sample.jsonc"));
        TestValueEntry count = new TestValueEntry(EntryType.INTEGER, false, 1, value -> ((Integer) value) <= 5);
        config.entries().put("count", count);

        integration.migrate("samplemod", "sample", config, config::save);

        assertEquals(1, count.get());
        assertEquals(1, config.saves());
        assertEquals(Writer.save(config).toString(), Files.readString(gameDirectory.resolve("config/sample.jsonc")));
        assertEquals("2", Files.readString(state()).strip());
    }

    @Test
    void customResourcefulConfigImplementationsKeepTheirNativeLifecycle() throws Exception {
        ResourcefulConfigIntegration integration = integration("""
                mod = "samplemod"
                config = "sample"

                [[changes]]
                version = 3
                op = "merge"
                [changes.fragment]
                count = 7
                """);
        NodeConfig custom = node("sample", "count", scalar(EntryType.INTEGER, 1));

        integration.migrate("samplemod", "sample", custom, custom::save);

        assertEquals(1, custom.saves);
        assertEquals(1, ((ResourcefulConfigValueEntry) custom.entries().get("count")).get());
        assertFalse(Files.exists(state()));
    }

    @Test
    void remainingLegacyJsonUsesTheNativeFallbackWithoutEnteringJsoncState() throws Exception {
        ResourcefulConfigIntegration integration = integration("""
                mod = "samplemod"
                config = "nested/sample"

                [[changes]]
                version = 3
                op = "merge"
                [changes.fragment]
                count = 7
                """);
        Path legacy = gameDirectory.resolve("config/nested/sample.json");
        Files.createDirectories(legacy.getParent());
        Files.writeString(legacy, "legacy");
        ParsedConfig config = new ParsedConfig("nested/sample", gameDirectory.resolve("config/nested/sample.jsonc"));
        TestValueEntry count = scalar(EntryType.INTEGER, 1);
        config.entries().put("count", count);

        integration.migrate("samplemod", "nested/sample", config, config::save);

        assertEquals(1, count.get());
        assertEquals(Writer.save(config).toString(), Files.readString(legacy));
        assertFalse(Files.exists(
                gameDirectory.resolve("config/config-migrations/state/config/nested/sample.jsonc.version")));
    }

    private ResourcefulConfigIntegration integration(String manifest) throws IOException {
        Path directory = gameDirectory.resolve("migrations/resourcefulconfig");
        Files.createDirectories(directory);
        Files.writeString(directory.resolve("sample.toml"), manifest);
        return ResourcefulConfigIntegration.load(
                MigrationManager.load(gameDirectory), directory, gameDirectory.resolve("config"));
    }

    private Path state() {
        return gameDirectory.resolve("config/config-migrations/state/config/sample.jsonc.version");
    }

    private static String manifest() {
        return """
                mod = "samplemod"
                config = "sample"

                [[changes]]
                version = 4
                op = "merge"
                [changes.fragment]
                count = 7
                values = [2, 3]
                mode = "slow"
                [changes.fragment.details]
                enabled = false
                [changes.fragment.graphics]
                scale = 0.75
                """;
    }

    private static TestValueEntry scalar(EntryType type, Object value) {
        return new TestValueEntry(type, false, value, ignored -> true);
    }

    private static TestValueEntry array(EntryType type, Object[] value) {
        return new TestValueEntry(type, true, value, ignored -> true);
    }

    private static TestObjectEntry object(String name, ResourcefulConfigEntry entry) {
        LinkedHashMap<String, ResourcefulConfigEntry> entries = new LinkedHashMap<>();
        entries.put(name, entry);
        return new TestObjectEntry(entries);
    }

    private static NodeConfig node(String id, String name, ResourcefulConfigEntry entry) {
        NodeConfig config = new NodeConfig(id);
        config.entries().put(name, entry);
        return config;
    }

    private enum Mode {
        FAST,
        SLOW
    }

    private record TestObjectEntry(LinkedHashMap<String, ResourcefulConfigEntry> entries)
            implements ResourcefulConfigObjectEntry {
        @Override
        public EntryType type() {
            return EntryType.OBJECT;
        }
    }

    private static final class NodeConfig implements ResourcefulConfig {
        private final String id;
        private final LinkedHashMap<String, ResourcefulConfigEntry> entries = new LinkedHashMap<>();
        private final LinkedHashMap<String, ResourcefulConfig> categories = new LinkedHashMap<>();
        private int saves;

        private NodeConfig(String id) {
            this.id = id;
        }

        @Override
        public String id() {
            return id;
        }

        @Override
        public LinkedHashMap<String, ResourcefulConfigEntry> entries() {
            return entries;
        }

        @Override
        public LinkedHashMap<String, ResourcefulConfig> categories() {
            return categories;
        }

        @Override
        public void save() {
            saves++;
        }
    }

    private static final class TestValueEntry implements ResourcefulConfigValueEntry {
        private final EntryType type;
        private final boolean array;
        private final Predicate<Object> accepts;
        private Object value;

        private TestValueEntry(EntryType type, boolean array, Object value, Predicate<Object> accepts) {
            this.type = type;
            this.array = array;
            this.value = value;
            this.accepts = accepts;
        }

        @Override
        public EntryType type() {
            return type;
        }

        @Override
        public Class<?> objectType() {
            if (type == EntryType.ENUM) {
                Object element = array ? ((Object[]) value)[0] : value;
                return element.getClass();
            }
            return value.getClass();
        }

        @Override
        public boolean isArray() {
            return array;
        }

        @Override
        public Object get() {
            return value;
        }

        @Override
        public boolean setArray(Object[] value) {
            return set(value, array);
        }

        @Override
        public boolean setByte(byte value) {
            return set(value, !array && type == EntryType.BYTE);
        }

        @Override
        public boolean setShort(short value) {
            return set(value, !array && type == EntryType.SHORT);
        }

        @Override
        public boolean setInt(int value) {
            return set(value, !array && type == EntryType.INTEGER);
        }

        @Override
        public boolean setLong(long value) {
            return set(value, !array && type == EntryType.LONG);
        }

        @Override
        public boolean setFloat(float value) {
            return set(value, !array && type == EntryType.FLOAT);
        }

        @Override
        public boolean setDouble(double value) {
            return set(value, !array && type == EntryType.DOUBLE);
        }

        @Override
        public boolean setBoolean(boolean value) {
            return set(value, !array && type == EntryType.BOOLEAN);
        }

        @Override
        public boolean setString(String value) {
            return set(value, !array && type == EntryType.STRING);
        }

        @Override
        public boolean setEnum(Enum<?> value) {
            return set(value, !array && type == EntryType.ENUM);
        }

        private boolean set(Object candidate, boolean rightType) {
            if (!rightType || !accepts.test(candidate)) {
                return false;
            }
            value = candidate;
            return true;
        }
    }
}
