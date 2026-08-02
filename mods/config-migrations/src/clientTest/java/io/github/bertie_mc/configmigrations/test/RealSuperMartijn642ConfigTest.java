package io.github.bertie_mc.configmigrations.test;

import com.supermartijn642.configlib.api.ConfigBuilders;
import com.supermartijn642.configlib.api.IConfigBuilder;
import java.util.function.Supplier;

/** A real SuperMartijn642 Config Lib owner used by the client integration test. */
public final class RealSuperMartijn642ConfigTest {
    public static final String CONFIG_NAME = "supermartijn642";
    public static final String FILE_NAME =
            ConfigMigrationsClientTestMod.MOD_ID + "-" + CONFIG_NAME + ".toml";
    public static final int MIGRATED_VALUE = 77;

    private static Supplier<Integer> migratedValue;

    private RealSuperMartijn642ConfigTest() {
    }

    public static void register() {
        IConfigBuilder builder = ConfigBuilders.newTomlConfig(
                ConfigMigrationsClientTestMod.MOD_ID, CONFIG_NAME, false);
        migratedValue = builder.define(
                "migratedValue", 1, Integer.MIN_VALUE, Integer.MAX_VALUE);
        builder.build();
    }

    public static void assertMigrated() {
        int actual = migratedValue.get();
        if (actual != MIGRATED_VALUE) {
            throw new IllegalStateException(
                    "SuperMartijn642 Config Lib migration did not reach the live config: " + actual);
        }
    }
}
