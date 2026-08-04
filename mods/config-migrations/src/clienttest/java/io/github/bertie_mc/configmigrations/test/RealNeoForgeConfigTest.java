package io.github.bertie_mc.configmigrations.test;

import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.ModConfigSpec;

/** Real standard NeoForge configs used by the client integration test. */
public final class RealNeoForgeConfigTest {
    public static final String STARTUP_FILE = "configmigrationstest-startup.toml";
    public static final String CLIENT_FILE = "configmigrationstest-client.toml";
    public static final String SERVER_FILE = "configmigrationstest-server.toml";
    public static final int STARTUP_MIGRATED_VALUE = 70;
    public static final int CLIENT_MIGRATED_VALUE = 71;
    public static final int SERVER_MIGRATED_VALUE = 72;

    private static final ModConfigSpec STARTUP_SPEC;
    private static final ModConfigSpec CLIENT_SPEC;
    private static final ModConfigSpec SERVER_SPEC;
    private static final ModConfigSpec.IntValue startupValue;
    private static final ModConfigSpec.IntValue clientValue;
    private static final ModConfigSpec.IntValue serverValue;

    static {
        ModConfigSpec.Builder startup = new ModConfigSpec.Builder();
        startupValue = startup.defineInRange("migratedValue", 1, 0, 100);
        STARTUP_SPEC = startup.build();

        ModConfigSpec.Builder client = new ModConfigSpec.Builder();
        clientValue = client.defineInRange("migratedValue", 1, 0, 100);
        CLIENT_SPEC = client.build();

        ModConfigSpec.Builder server = new ModConfigSpec.Builder();
        serverValue = server.defineInRange("migratedValue", 1, 0, 100);
        SERVER_SPEC = server.build();
    }

    private RealNeoForgeConfigTest() {
    }

    public static void register(ModContainer container) {
        container.registerConfig(ModConfig.Type.STARTUP, STARTUP_SPEC, STARTUP_FILE);
        container.registerConfig(ModConfig.Type.CLIENT, CLIENT_SPEC, CLIENT_FILE);
        container.registerConfig(ModConfig.Type.SERVER, SERVER_SPEC, SERVER_FILE);
    }

    public static void assertStartupMigrated() {
        assertValue("NeoForge STARTUP", STARTUP_MIGRATED_VALUE, startupValue.get());
    }

    public static void assertClientMigrated() {
        assertValue("NeoForge CLIENT", CLIENT_MIGRATED_VALUE, clientValue.get());
    }

    public static void assertServerMigrated() {
        assertValue("NeoForge SERVER", SERVER_MIGRATED_VALUE, serverValue.get());
    }

    private static void assertValue(String config, int expected, int actual) {
        if (actual != expected) {
            throw new IllegalStateException(
                    config + " migration did not reach the live config: " + actual);
        }
    }
}
