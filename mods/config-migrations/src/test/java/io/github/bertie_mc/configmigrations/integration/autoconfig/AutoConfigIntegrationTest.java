package io.github.bertie_mc.configmigrations.integration.autoconfig;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.bertie_mc.configmigrations.migration.MigrationManager;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.serializer.ConfigSerializer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class AutoConfigIntegrationTest {
    @TempDir
    Path gameDirectory;

    @Test
    void commitsOnlyAfterTheNativeSerializerPersistsTheMigratedObject() throws Exception {
        AutoConfigIntegration integration = integration("""
                config = "sample"
                file = "sample.json"

                [[changes]]
                version = 3
                op = "merge"
                [changes.fragment]
                count = 7
                """);
        RecordingSerializer<SampleConfig> nativeSerializer =
                new RecordingSerializer<>(new SampleConfig(), new SampleConfig());
        ConfigSerializer<SampleConfig> serializer = wrap(integration, "sample", nativeSerializer);

        SampleConfig loaded = serializer.deserialize();
        assertEquals(7, loaded.count);
        assertFalse(Files.exists(state("sample.json")));

        loaded.validatePostLoad();
        assertEquals(7, loaded.validatedCount);
        serializer.serialize(loaded);

        assertSame(loaded, nativeSerializer.serialized);
        assertEquals("3", Files.readString(state("sample.json")).strip());

        loaded.count = 11;
        serializer.deserialize();
        assertEquals(11, loaded.count, "an applied migration is not reasserted");
    }

    @Test
    void aFailedNativeSaveDoesNotAdvanceState() throws Exception {
        AutoConfigIntegration integration = integration("""
                config = "sample"
                file = "failed.json"

                [[changes]]
                version = 2
                op = "merge"
                [changes.fragment]
                count = 5
                """);
        RecordingSerializer<SampleConfig> nativeSerializer =
                new RecordingSerializer<>(new SampleConfig(), new SampleConfig());
        nativeSerializer.failSave = true;
        ConfigSerializer<SampleConfig> serializer = wrap(integration, "sample", nativeSerializer);

        SampleConfig loaded = serializer.deserialize();
        assertThrows(ConfigSerializer.SerializationException.class, () -> serializer.serialize(loaded));

        assertFalse(Files.exists(state("failed.json")));
    }

    @Test
    void replacingARejectedRootWithDefaultsDoesNotCommitItsMigration() throws Exception {
        AutoConfigIntegration integration = integration("""
                config = "sample"
                file = "rejected.json"

                [[changes]]
                version = 4
                op = "merge"
                [changes.fragment]
                count = 13
                """);
        RecordingSerializer<SampleConfig> nativeSerializer =
                new RecordingSerializer<>(new SampleConfig(), new SampleConfig());
        ConfigSerializer<SampleConfig> serializer = wrap(integration, "sample", nativeSerializer);

        serializer.deserialize();
        serializer.serialize(serializer.createDefault());

        assertFalse(Files.exists(state("rejected.json")));
    }

    @Test
    void selectsAPartitionByItsOwnAutoConfigName() throws Exception {
        AutoConfigIntegration integration = integration("""
                config = "partitioned"
                partition = "server"
                file = "partitioned/server.json5"

                [[changes]]
                version = 6
                op = "merge"
                [changes.fragment]
                enabled = false
                """);
        PartitionedConfig root = new PartitionedConfig();
        RecordingSerializer<PartitionedConfig> nativeSerializer =
                new RecordingSerializer<>(root, new PartitionedConfig());
        ConfigSerializer<PartitionedConfig> serializer =
                wrap(integration, "partitioned", nativeSerializer);

        assertFalse(serializer.deserialize().server.enabled);
        assertTrue(root.client.enabled);
        serializer.serialize(root);

        assertEquals("6", Files.readString(state("partitioned/server.json5")).strip());
    }

    @Test
    void leavesUnconfiguredAutoConfigSerializersAlone() throws Exception {
        AutoConfigIntegration integration = integration("""
                config = "sample"
                file = "sample.json"
                """);
        RecordingSerializer<SampleConfig> serializer =
                new RecordingSerializer<>(new SampleConfig(), new SampleConfig());

        assertSame(serializer, integration.wrap("other", serializer));
    }

    private AutoConfigIntegration integration(String manifest) throws IOException {
        Path directory = gameDirectory.resolve("migrations/autoconfig");
        Files.createDirectories(directory);
        Files.writeString(directory.resolve("target.toml"), manifest);
        return AutoConfigIntegration.load(
                MigrationManager.load(gameDirectory), directory, gameDirectory.resolve("config"));
    }

    private Path state(String file) {
        return gameDirectory.resolve("config/config-migrations/state/config").resolve(file + ".version");
    }

    @SuppressWarnings("unchecked")
    private static <T extends ConfigData> ConfigSerializer<T> wrap(
            AutoConfigIntegration integration, String name, ConfigSerializer<T> serializer) {
        return (ConfigSerializer<T>) integration.wrap(name, serializer);
    }

    @Config(name = "sample")
    private static final class SampleConfig implements ConfigData {
        private int count = 1;
        private int validatedCount;

        @Override
        public void validatePostLoad() {
            validatedCount = count;
        }
    }

    @Config(name = "partitioned")
    private static final class PartitionedConfig implements ConfigData {
        private ServerConfig server = new ServerConfig();
        private ClientConfig client = new ClientConfig();
    }

    @Config(name = "server")
    private static final class ServerConfig implements ConfigData {
        private boolean enabled = true;
    }

    @Config(name = "client")
    private static final class ClientConfig implements ConfigData {
        private boolean enabled = true;
    }

    private static final class RecordingSerializer<T extends ConfigData>
            implements ConfigSerializer<T> {
        private final T loaded;
        private final T defaults;
        private T serialized;
        private boolean failSave;

        private RecordingSerializer(T loaded, T defaults) {
            this.loaded = loaded;
            this.defaults = defaults;
        }

        @Override
        public void serialize(T config) throws SerializationException {
            if (failSave) {
                throw new SerializationException(new IOException("save failed"));
            }
            serialized = config;
        }

        @Override
        public T deserialize() {
            return loaded;
        }

        @Override
        public T createDefault() {
            return defaults;
        }
    }
}
