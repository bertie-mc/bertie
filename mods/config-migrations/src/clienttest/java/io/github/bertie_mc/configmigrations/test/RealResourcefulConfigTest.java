package io.github.bertie_mc.configmigrations.test;

import com.teamresourceful.resourcefulconfig.api.annotations.Config;
import com.teamresourceful.resourcefulconfig.api.annotations.ConfigEntry;
import com.teamresourceful.resourcefulconfig.api.loader.Configurator;

/** A real Resourceful Config owner used by the client integration test. */
public final class RealResourcefulConfigTest {
    public static final String CONFIG_NAME = "configmigrationstest-resourceful";
    public static final String FILE_NAME = CONFIG_NAME + ".jsonc";
    public static final int MIGRATED_VALUE = 76;

    private RealResourcefulConfigTest() {
    }

    public static void register() {
        new Configurator(ConfigMigrationsClientTestMod.MOD_ID).register(TestConfig.class);
    }

    public static void assertMigrated() {
        int actual = TestConfig.migratedValue;
        if (actual != MIGRATED_VALUE) {
            throw new IllegalStateException(
                    "Resourceful Config migration did not reach the live config: " + actual);
        }
    }

    @Config(CONFIG_NAME)
    public static final class TestConfig {
        @ConfigEntry(id = "migratedValue")
        public static int migratedValue = 1;

        private TestConfig() {
        }
    }
}
