package io.github.bertie_mc.configmigrations.test;

import com.anthonyhilyard.iceberg.config.IcebergConfig;
import com.anthonyhilyard.iceberg.services.IIcebergConfigSpecBuilder;
import java.util.function.Supplier;

/** A real Iceberg custom IConfigSpec used by the client integration test. */
public final class RealIcebergConfigTest extends IcebergConfig<RealIcebergConfigTest> {
    public static final String FILE_NAME = ConfigMigrationsClientTestMod.MOD_ID + ".toml";
    public static final int MIGRATED_VALUE = 79;

    private static Supplier<Integer> migratedValue;

    private RealIcebergConfigTest(IIcebergConfigSpecBuilder builder) {
        migratedValue = builder.addInRange("migratedValue", 1, 0, 100);
    }

    public static void register() {
        if (!IcebergConfig.register(
                RealIcebergConfigTest.class, ConfigMigrationsClientTestMod.MOD_ID)) {
            throw new IllegalStateException("Iceberg config registration failed");
        }
    }

    public static void assertMigrated() {
        int actual = migratedValue.get();
        if (actual != MIGRATED_VALUE) {
            throw new IllegalStateException(
                    "Iceberg migration did not reach the live config: " + actual);
        }
    }
}
