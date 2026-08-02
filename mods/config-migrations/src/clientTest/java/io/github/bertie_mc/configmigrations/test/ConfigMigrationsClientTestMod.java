package io.github.bertie_mc.configmigrations.test;

import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.config.ModConfigs;
import net.neoforged.fml.event.lifecycle.FMLLoadCompleteEvent;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import org.slf4j.Logger;

/** Runs real migrations through every supported config-system lifecycle. */
@Mod(value = ConfigMigrationsClientTestMod.MOD_ID, dist = Dist.CLIENT)
public final class ConfigMigrationsClientTestMod {
    static final String MOD_ID = "configmigrationstest";
    private static final String CLIENT_SUCCESS = "CONFIG_MIGRATIONS_CLIENT_MIGRATIONS_OK";
    private static final String SERVER_SUCCESS = "CONFIG_MIGRATIONS_SERVER_MIGRATION_OK";
    private static final Logger LOGGER = LogUtils.getLogger();

    public ConfigMigrationsClientTestMod(IEventBus modBus, ModContainer container) {
        RealNeoForgeConfigTest.register(container);
        RealIcebergConfigTest.register();
        RealAutoConfigTest.register();
        RealFzzyConfigTest.load();
        RealOwoConfigTest.load();
        RealResourcefulConfigTest.register();
        RealSuperMartijn642ConfigTest.register();
        RealWunderLibConfigTest.load();

        modBus.addListener(this::onLoadComplete);
        NeoForge.EVENT_BUS.addListener(this::onServerStarted);
    }

    private void onLoadComplete(FMLLoadCompleteEvent event) {
        event.enqueueWork(() -> {
            RealNeoForgeConfigTest.assertStartupMigrated();
            RealNeoForgeConfigTest.assertClientMigrated();
            RealIcebergConfigTest.assertMigrated();
            RealAutoConfigTest.assertMigrated();
            RealFzzyConfigTest.assertMigrated();
            RealOwoConfigTest.assertMigrated();
            RealResourcefulConfigTest.assertMigrated();
            RealSuperMartijn642ConfigTest.assertMigrated();
            RealWunderLibConfigTest.assertMigrated();
            assertMinecraftOptionsMigrated();
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
            LOGGER.info(CLIENT_SUCCESS);
        });
    }

    private void onServerStarted(ServerStartedEvent event) {
        RealNeoForgeConfigTest.assertServerMigrated();
        ModConfig config = ModConfigs.getConfigSet(ModConfig.Type.SERVER).stream()
                .filter(candidate -> candidate.getModId().equals(MOD_ID))
                .filter(candidate -> candidate.getFileName().equals(RealNeoForgeConfigTest.SERVER_FILE))
                .findFirst()
                .orElseThrow();
        assertPersisted(
                config.getFullPath(),
                "migratedValue",
                Integer.toString(RealNeoForgeConfigTest.SERVER_MIGRATED_VALUE));
        LOGGER.info(SERVER_SUCCESS);
    }

    private static void assertMinecraftOptionsMigrated() {
        if (Minecraft.getInstance().options.autoJump().get()) {
            throw new IllegalStateException("Minecraft options migration did not reach the live options");
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
                throw new IllegalStateException(
                        "Artifacts migration did not reach the live config: " + actual);
            }
        } catch (ClassNotFoundException
                | NoSuchFieldException
                | NoSuchMethodException
                | IllegalAccessException
                | InvocationTargetException exception) {
            throw new IllegalStateException("Cannot inspect the Artifacts live config", exception);
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
                throw new IllegalStateException(
                        "Migrated value " + key + "=" + value + " is absent from " + target);
            }

            Path gameDirectory = FMLPaths.GAMEDIR.get();
            Path relative = gameDirectory.relativize(target);
            Path state = gameDirectory.resolve("config/config-migrations/state").resolve(relative);
            state = state.resolveSibling(state.getFileName() + ".version");
            if (!Files.readString(state, StandardCharsets.UTF_8).strip().equals("1")) {
                throw new IllegalStateException("Migration state was not committed for " + target);
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot inspect migrated config " + target, exception);
        }
    }
}
