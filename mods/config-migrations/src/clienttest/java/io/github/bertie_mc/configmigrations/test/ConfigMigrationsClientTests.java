package io.github.bertie_mc.configmigrations.test;

import io.github.bertie_mc.testing.client.ClientTest;
import io.github.bertie_mc.testing.client.ClientTestContext;
import io.github.bertie_mc.testing.client.IntegratedWorldContext;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;
import net.minecraft.client.Minecraft;
import net.neoforged.fml.loading.FMLPaths;

/** Exercises every supported migration against its real config-system lifecycle. */
public final class ConfigMigrationsClientTests {
    private ConfigMigrationsClientTests() {
    }

    @ClientTest
    public static void migratesRealConfigSystems(ClientTestContext context) {
        context.runOnClient(ConfigMigrationsClientTests::assertClientMigrations);

        try (IntegratedWorldContext world = context.worldBuilder()
                .adjustSettings(settings -> settings.setName("config-migrations"))
                .create()) {
            world.server().runOnServer(server -> assertServerMigration());
        }
    }

    private static void assertClientMigrations(Minecraft client) {
        RealNeoForgeConfigTest.assertStartupMigrated();
        RealNeoForgeConfigTest.assertClientMigrated();
        RealIcebergConfigTest.assertMigrated();
        RealAutoConfigTest.assertMigrated();
        RealFzzyConfigTest.assertMigrated();
        RealOwoConfigTest.assertMigrated();
        RealResourcefulConfigTest.assertMigrated();
        RealSuperMartijn642ConfigTest.assertMigrated();
        RealWunderLibConfigTest.assertMigrated();
        assertMinecraftOptionsMigrated(client);
        assertArtifactsMigrated();

        assertPersisted("options.txt", "autoJump", "false");
        assertPersisted(
                "config/" + RealNeoForgeConfigTest.STARTUP_FILE,
                "migratedValue",
                Integer.toString(RealNeoForgeConfigTest.STARTUP_MIGRATED_VALUE));
        assertPersisted(
                "config/" + RealNeoForgeConfigTest.CLIENT_FILE,
                "migratedValue",
                Integer.toString(RealNeoForgeConfigTest.CLIENT_MIGRATED_VALUE));
        assertPersisted(
                "config/" + RealIcebergConfigTest.FILE_NAME,
                "migratedValue",
                Integer.toString(RealIcebergConfigTest.MIGRATED_VALUE));
        assertPersisted(
                "config/" + RealAutoConfigTest.FILE_NAME,
                "migratedValue",
                Integer.toString(RealAutoConfigTest.MIGRATED_VALUE));
        assertPersisted(
                "config/" + RealFzzyConfigTest.FILE_NAME,
                "migratedValue",
                Integer.toString(RealFzzyConfigTest.MIGRATED_VALUE));
        assertPersisted(
                "config/" + RealOwoConfigTest.FILE_NAME,
                "migratedValue",
                Integer.toString(RealOwoConfigTest.MIGRATED_VALUE));
        assertPersisted(
                "config/" + RealResourcefulConfigTest.FILE_NAME,
                "migratedValue",
                Integer.toString(RealResourcefulConfigTest.MIGRATED_VALUE));
        assertPersisted(
                "config/" + RealSuperMartijn642ConfigTest.FILE_NAME,
                "migratedValue",
                Integer.toString(RealSuperMartijn642ConfigTest.MIGRATED_VALUE));
        assertPersisted(
                "config/" + RealWunderLibConfigTest.FILE_NAME,
                "migratedValue",
                Integer.toString(RealWunderLibConfigTest.MIGRATED_VALUE));
        assertPersisted("config/artifacts/items.toml", "fartChance", "0.79");
    }

    private static void assertServerMigration() {
        RealNeoForgeConfigTest.assertServerMigrated();
        Path config = ConfigMigrationsClientTestMod.serverConfigPath();
        if (config == null) {
            throw new AssertionError("NeoForge server config was not loaded");
        }
        assertPersisted(
                config,
                "migratedValue",
                Integer.toString(RealNeoForgeConfigTest.SERVER_MIGRATED_VALUE));
    }

    private static void assertMinecraftOptionsMigrated(Minecraft client) {
        if (client.options.autoJump().get()) {
            throw new AssertionError("Minecraft options migration did not reach the live options");
        }
    }

    private static void assertArtifactsMigrated() {
        try {
            Class<?> artifacts = Class.forName("artifacts.Artifacts");
            Object config = artifacts.getField("CONFIG").get(null);
            Object items = config.getClass().getField("items").get(config);
            Object value = items.getClass().getField("whoopeeCushionFartChance").get(items);
            double actual = ((Number) value.getClass().getMethod("get").invoke(value)).doubleValue();
            if (actual != 0.79) {
                throw new AssertionError(
                        "Artifacts migration did not reach the live config: " + actual);
            }
        } catch (ClassNotFoundException
                | NoSuchFieldException
                | NoSuchMethodException
                | IllegalAccessException
                | InvocationTargetException exception) {
            throw new AssertionError("Cannot inspect the Artifacts live config", exception);
        }
    }

    private static void assertPersisted(String relativePath, String key, String value) {
        assertPersisted(FMLPaths.GAMEDIR.get().resolve(relativePath), key, value);
    }

    private static void assertPersisted(Path target, String key, String value) {
        try {
            String contents = Files.readString(target, StandardCharsets.UTF_8);
            Pattern assignment = Pattern.compile(
                    Pattern.quote(key) + "\"?\\s*[:=]\\s*" + Pattern.quote(value));
            if (!assignment.matcher(contents).find()) {
                throw new AssertionError(
                        "Migrated value " + key + "=" + value + " is absent from " + target);
            }

            Path gameDirectory = FMLPaths.GAMEDIR.get();
            Path relative = gameDirectory.relativize(target);
            Path state = gameDirectory.resolve("config/config-migrations/state").resolve(relative);
            state = state.resolveSibling(state.getFileName() + ".version");
            if (!Files.readString(state, StandardCharsets.UTF_8).strip().equals("1")) {
                throw new AssertionError("Migration state was not committed for " + target);
            }
        } catch (IOException exception) {
            throw new AssertionError("Cannot inspect migrated config " + target, exception);
        }
    }
}
