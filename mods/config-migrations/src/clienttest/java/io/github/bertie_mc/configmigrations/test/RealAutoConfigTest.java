package io.github.bertie_mc.configmigrations.test;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.ConfigHolder;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;

/** A real AutoConfig owner used by the client integration test. */
public final class RealAutoConfigTest {
    public static final String CONFIG_NAME = "configmigrationstest-autoconfig";
    public static final String FILE_NAME = CONFIG_NAME + ".json";
    public static final int MIGRATED_VALUE = 73;

    private static ConfigHolder<TestConfig> holder;

    private RealAutoConfigTest() {
    }

    public static void register() {
        holder = AutoConfig.register(TestConfig.class, GsonConfigSerializer::new);
    }

    public static void assertMigrated() {
        int actual = holder.getConfig().migratedValue;
        if (actual != MIGRATED_VALUE) {
            throw new IllegalStateException(
                    "AutoConfig migration did not reach the live config: " + actual);
        }
    }

    @Config(name = CONFIG_NAME)
    public static final class TestConfig implements ConfigData {
        public int migratedValue = 1;
    }
}
