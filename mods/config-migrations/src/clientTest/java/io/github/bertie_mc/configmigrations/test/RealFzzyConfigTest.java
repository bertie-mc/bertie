package io.github.bertie_mc.configmigrations.test;

import me.fzzyhmstrs.fzzy_config.api.ConfigApiJava;
import me.fzzyhmstrs.fzzy_config.config.Config;
import net.minecraft.resources.ResourceLocation;

/** A real Fzzy Config owner used by the client integration test. */
public final class RealFzzyConfigTest {
    public static final String FILE_NAME = "configmigrationstest-fzzy.toml";
    public static final int MIGRATED_VALUE = 74;

    private static TestConfig config;

    private RealFzzyConfigTest() {
    }

    public static void load() {
        config = ConfigApiJava.readOrCreateAndValidate(TestConfig::new);
    }

    public static void assertMigrated() {
        int actual = config.migratedValue;
        if (actual != MIGRATED_VALUE) {
            throw new IllegalStateException(
                    "Fzzy Config migration did not reach the live config: " + actual);
        }
    }

    public static final class TestConfig extends Config {
        public int migratedValue = 1;

        public TestConfig() {
            super(
                    ResourceLocation.fromNamespaceAndPath(
                            "configmigrationstest", "fzzy"),
                    "",
                    "",
                    "configmigrationstest-fzzy");
        }
    }
}
