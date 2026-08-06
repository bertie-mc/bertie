package io.github.bertie_mc.configmigrations.test;

import de.ambertation.wunderlib.configs.ConfigFile;
import de.ambertation.wunderlib.utils.Version;

/** A real WunderLib config owner used by the client integration test. */
public final class RealWunderLibConfigTest {
    public static final String NAMESPACE = ConfigMigrationsClientTestMod.MOD_ID;
    public static final String CATEGORY = "wunderlib";
    public static final String CONFIG_ID = NAMESPACE + ":" + CATEGORY;
    public static final String FILE_NAME = NAMESPACE + "/" + CATEGORY + ".json";
    public static final int MIGRATED_VALUE = 78;

    private static TestConfig config;

    private RealWunderLibConfigTest() {}

    public static void load() {
        config = new TestConfig();
    }

    public static void assertMigrated() {
        int actual = config.migratedValue.get();
        if (actual != MIGRATED_VALUE) {
            throw new IllegalStateException("WunderLib migration did not reach the live config: " + actual);
        }
    }

    private static final class TestConfig extends ConfigFile {
        private final IntValue migratedValue;

        private TestConfig() {
            super(new Provider(), NAMESPACE, CATEGORY);
            migratedValue = new IntValue("", "migratedValue", 1);
        }
    }

    private static final class Provider implements Version.ModVersionProvider {
        @Override
        public Version getModVersion() {
            return new Version(1, 0, 0);
        }

        @Override
        public String getModID() {
            return NAMESPACE;
        }
    }
}
