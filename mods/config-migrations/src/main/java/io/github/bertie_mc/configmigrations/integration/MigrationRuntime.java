package io.github.bertie_mc.configmigrations.integration;

import com.electronwill.nightconfig.core.ConfigSpec;
import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.google.gson.JsonObject;
import io.github.bertie_mc.configmigrations.ConfigMigrationException;
import io.github.bertie_mc.configmigrations.integration.artifacts.ArtifactsIntegration;
import io.github.bertie_mc.configmigrations.integration.autoconfig.AutoConfigIntegration;
import io.github.bertie_mc.configmigrations.integration.fzzyconfig.FzzyConfigIntegration;
import io.github.bertie_mc.configmigrations.integration.neoforge.NeoForgeIntegration;
import io.github.bertie_mc.configmigrations.integration.owoconfig.OwoConfigIntegration;
import io.github.bertie_mc.configmigrations.integration.resourcefulconfig.ResourcefulConfigIntegration;
import io.github.bertie_mc.configmigrations.integration.supermartijn642.SuperMartijn642Integration;
import io.github.bertie_mc.configmigrations.integration.wunderlib.WunderLibIntegration;
import io.github.bertie_mc.configmigrations.migration.MigrationManager;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.config.IConfigSpec;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.fml.loading.LoadingModList;

/** Creates the shared migration engine and delegates hooks to concrete integrations. */
public final class MigrationRuntime {
    private static final Set<String> INTEGRATIONS =
            Set.of(
                    "minecraft",
                    "neoforge",
                    "artifacts",
                    "autoconfig",
                    "fzzy",
                    "owo",
                    "resourceful",
                    "supermartijn642",
                    "wunderlib");

    private static NeoForgeIntegration neoForge;
    private static ArtifactsIntegration artifacts;
    private static AutoConfigIntegration autoConfig;
    private static FzzyConfigIntegration fzzyConfig;
    private static OwoConfigIntegration owoConfig;
    private static ResourcefulConfigIntegration resourcefulConfig;
    private static SuperMartijn642Integration superMartijn642;
    private static WunderLibIntegration wunderLib;

    private MigrationRuntime() {
    }

    public static void initializeLaunch(boolean datagen) {
        if (datagen) {
            neoForge = null;
            artifacts = null;
            autoConfig = null;
            fzzyConfig = null;
            owoConfig = null;
            resourcefulConfig = null;
            superMartijn642 = null;
            wunderLib = null;
            return;
        }

        Path gameDirectory = FMLPaths.GAMEDIR.get();
        Path manifests = gameDirectory.resolve("config/config-migrations/migrations");
        validateIntegrationDirectories(manifests);
        MigrationManager migrations = MigrationManager.load(gameDirectory);
        neoForge = NeoForgeIntegration.load(migrations, manifests.resolve("neoforge"));
        artifacts = ArtifactsIntegration.load(migrations, manifests.resolve("artifacts"));
        autoConfig = AutoConfigIntegration.load(
                migrations,
                manifests.resolve("autoconfig"),
                gameDirectory.resolve("config"));
        fzzyConfig = FzzyConfigIntegration.load(
                migrations,
                gameDirectory.resolve("config"),
                manifests.resolve("fzzy"));
        owoConfig = LoadingModList.get().getModFileById("owo") != null
                ? OwoConfigIntegration.load(migrations, manifests.resolve("owo"))
                : null;
        resourcefulConfig = ResourcefulConfigIntegration.load(
                migrations,
                manifests.resolve("resourceful"),
                gameDirectory.resolve("config"));
        superMartijn642 = LoadingModList.get().getModFileById("supermartijn642configlib") != null
                ? SuperMartijn642Integration.load(
                        migrations,
                        gameDirectory.resolve("config"),
                        manifests.resolve("supermartijn642"))
                : null;
        wunderLib = WunderLibIntegration.load(migrations, manifests.resolve("wunderlib"));
    }

    public static void runNeoForgeRegistrationPhase(Runnable nativeGather) {
        NeoForgeIntegration integration = neoForge;
        if (integration == null) {
            nativeGather.run();
        } else {
            integration.runRegistrationPhase(nativeGather);
        }
    }

    public static void runNeoForgeLoadPhase(ModConfig.Type type, Runnable nativeLoad) {
        NeoForgeIntegration integration = neoForge;
        if (integration == null) {
            nativeLoad.run();
        } else {
            integration.runLoadPhase(type, nativeLoad);
        }
    }

    public static void registerNeoForgeSpec(IConfigSpec spec, ModConfig config) {
        NeoForgeIntegration integration = neoForge;
        if (integration != null) {
            integration.registerSpec(spec, config);
        }
    }

    public static void acceptNeoForge(
            IConfigSpec spec,
            IConfigSpec.ILoadedConfig loadedConfig,
            Runnable nativeAcceptance) {
        NeoForgeIntegration integration = neoForge;
        if (integration == null) {
            nativeAcceptance.run();
        } else {
            integration.accept(spec, loadedConfig, nativeAcceptance);
        }
    }

    public static void migrateArtifacts(String configName, CommentedFileConfig document, ConfigSpec spec) {
        ArtifactsIntegration integration = artifacts;
        if (integration != null) {
            integration.migrate(configName, document, spec);
        }
    }

    public static Object wrapAutoConfigSerializer(String configName, Object serializer) {
        AutoConfigIntegration integration = autoConfig;
        return integration == null ? serializer : integration.wrap(configName, serializer);
    }

    public static Object runFzzyConfigLoad(
            Object api,
            Object classInstance,
            String name,
            String folder,
            String subfolder,
            Supplier<Object> nativeLoad) {
        FzzyConfigIntegration integration = fzzyConfig;
        return integration == null
                ? nativeLoad.get()
                : integration.runLoad(api, classInstance, name, folder, subfolder, nativeLoad);
    }

    public static void joinFzzyConfigWrite(CompletableFuture<Void> write) {
        FzzyConfigIntegration integration = fzzyConfig;
        if (integration != null) {
            integration.joinNativeWrite(write);
        }
    }

    public static boolean prepareOwoConfigLoad(String configName, Path path) {
        OwoConfigIntegration integration = owoConfig;
        return integration != null && integration.prepareLoad(configName, path);
    }

    public static Object mergeOwoConfigDocument(String configName, Object document) {
        OwoConfigIntegration integration = owoConfig;
        return integration == null ? document : integration.mergeDocument(configName, document);
    }

    public static void finishOwoConfigLoad(String configName, Runnable nativeSave) {
        OwoConfigIntegration integration = owoConfig;
        if (integration != null) {
            integration.finishLoad(configName, nativeSave);
        }
    }

    public static void cancelOwoConfigLoad(String configName) {
        OwoConfigIntegration integration = owoConfig;
        if (integration != null) {
            integration.cancelLoad(configName);
        }
    }

    public static void migrateResourcefulConfig(
            String modId,
            String configId,
            Object config,
            Runnable nativeSave) {
        ResourcefulConfigIntegration integration = resourcefulConfig;
        if (integration == null) {
            nativeSave.run();
        } else {
            integration.migrate(modId, configId, config, nativeSave);
        }
    }

    public static void applySuperMartijn642Config(String modId, String identifier, Object document) {
        SuperMartijn642Integration integration = superMartijn642;
        if (integration != null) {
            integration.apply(modId, identifier, document);
        }
    }

    public static void commitSuperMartijn642Config(String modId, String identifier) {
        SuperMartijn642Integration integration = superMartijn642;
        if (integration != null) {
            integration.commit(modId, identifier);
        }
    }

    public static void migrateWunderLib(
            ResourceLocation id, Path path, JsonObject document, Runnable nativeSave) {
        WunderLibIntegration integration = wunderLib;
        if (integration != null) {
            integration.migrate(id, path, document, nativeSave);
        }
    }

    static void resetForTests() {
        neoForge = null;
        artifacts = null;
        autoConfig = null;
        fzzyConfig = null;
        owoConfig = null;
        resourcefulConfig = null;
        superMartijn642 = null;
        wunderLib = null;
    }

    static void validateIntegrationDirectories(Path manifests) {
        if (!Files.isDirectory(manifests)) {
            return;
        }
        try (var entries = Files.list(manifests)) {
            for (Path entry : entries.toList()) {
                if (!Files.isDirectory(entry) || !INTEGRATIONS.contains(entry.getFileName().toString())) {
                    throw new ConfigMigrationException("Unknown config migration integration " + entry.getFileName());
                }
            }
        } catch (IOException exception) {
            throw new ConfigMigrationException("Failed to discover config migration integrations", exception);
        }
    }
}
