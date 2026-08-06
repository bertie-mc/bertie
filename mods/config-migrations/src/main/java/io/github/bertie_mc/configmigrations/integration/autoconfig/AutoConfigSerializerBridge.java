package io.github.bertie_mc.configmigrations.integration.autoconfig;

import io.github.bertie_mc.configmigrations.ConfigMigrationException;
import io.github.bertie_mc.configmigrations.integration.autoconfig.AutoConfigIntegration.Target;
import io.github.bertie_mc.configmigrations.migration.MigrationManager;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.serializer.ConfigSerializer;

/** Isolates optional AutoConfig API linkage from the always-loaded migration catalog. */
final class AutoConfigSerializerBridge {
    private AutoConfigSerializerBridge() {}

    static Object wrap(MigrationManager migrations, List<Target> targets, Object serializer) {
        return new MigratingSerializer<>(migrations, cast(serializer), targets);
    }

    @SuppressWarnings("unchecked")
    private static <T extends ConfigData> ConfigSerializer<T> cast(Object serializer) {
        return (ConfigSerializer<T>) serializer;
    }

    private static ConfigData partition(ConfigData root, String name) {
        if (name == null) {
            return root;
        }
        for (Field field : root.getClass().getDeclaredFields()) {
            if (!ConfigData.class.isAssignableFrom(field.getType())) {
                continue;
            }
            Config definition = field.getType().getAnnotation(Config.class);
            if (definition == null || !definition.name().equals(name)) {
                continue;
            }
            try {
                field.setAccessible(true);
                return (ConfigData) field.get(root);
            } catch (IllegalAccessException exception) {
                throw new ConfigMigrationException("Failed to read AutoConfig partition " + name, exception);
            }
        }
        return null;
    }

    private static final class MigratingSerializer<T extends ConfigData> implements ConfigSerializer<T> {
        private final MigrationManager migrations;
        private final ConfigSerializer<T> serializer;
        private final List<Target> targets;
        private T pendingRoot;
        private List<MigrationManager.Migration> pending = List.of();

        private MigratingSerializer(MigrationManager migrations, ConfigSerializer<T> serializer, List<Target> targets) {
            this.migrations = migrations;
            this.serializer = serializer;
            this.targets = targets;
        }

        @Override
        public void serialize(T config) throws SerializationException {
            serializer.serialize(config);
            if (config != pendingRoot) {
                pendingRoot = null;
                pending = List.of();
                return;
            }
            for (MigrationManager.Migration migration : pending) {
                migration.commit();
            }
            pendingRoot = null;
            pending = List.of();
        }

        @Override
        public T deserialize() throws SerializationException {
            pendingRoot = null;
            pending = List.of();
            T root = serializer.deserialize();
            List<MigrationManager.Migration> prepared = new ArrayList<>();
            for (Target target : targets) {
                ConfigData data = partition(root, target.partition());
                if (data == null) {
                    continue;
                }
                MigrationManager.Migration migration = migrations.prepare(target.manifest(), target.file());
                if (migration != null) {
                    AutoConfigTreeMerge.apply(data, migration.changes());
                    prepared.add(migration);
                }
            }
            if (!prepared.isEmpty()) {
                pendingRoot = root;
                pending = List.copyOf(prepared);
            }
            return root;
        }

        @Override
        public T createDefault() {
            return serializer.createDefault();
        }
    }
}
