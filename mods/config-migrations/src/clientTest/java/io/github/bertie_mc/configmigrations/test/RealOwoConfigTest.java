package io.github.bertie_mc.configmigrations.test;

import io.wispforest.owo.config.ConfigWrapper;
import io.wispforest.owo.config.Option;
import io.wispforest.owo.config.annotation.Config;

/** A real owo-config owner used by the client integration test. */
public final class RealOwoConfigTest {
    public static final String CONFIG_NAME = "configmigrationstest-owo";
    public static final String FILE_NAME = CONFIG_NAME + ".json5";
    public static final int MIGRATED_VALUE = 75;

    private static TestWrapper wrapper;

    private RealOwoConfigTest() {
    }

    public static void load() {
        wrapper = new TestWrapper();
        wrapper.load();
    }

    public static void assertMigrated() {
        int actual = wrapper.migratedValue();
        if (actual != MIGRATED_VALUE) {
            throw new IllegalStateException(
                    "owo-config migration did not reach the live config: " + actual);
        }
    }

    @Config(wrapperName = "ConfigMigrationsTestOwoConfig", name = CONFIG_NAME)
    public static final class TestConfig {
        public int migratedValue = 1;
    }

    private static final class TestWrapper extends ConfigWrapper<TestConfig> {
        private TestWrapper() {
            super(TestConfig.class);
        }

        private int migratedValue() {
            Option<Integer> option = optionForKey(new Option.Key("migratedValue"));
            return option.value();
        }
    }
}
