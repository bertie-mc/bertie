package io.github.bertie_mc.configmigrations.test;

import java.nio.file.Path;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.config.ModConfigs;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

/** Registers the real config owners used by {@link ConfigMigrationsClientTests}. */
@Mod(value = ConfigMigrationsClientTestMod.SUITE_MOD_ID, dist = Dist.CLIENT)
public final class ConfigMigrationsClientTestMod {
    static final String SUITE_MOD_ID = "configmigrations_clienttests";
    static final String MOD_ID = SUITE_MOD_ID;
    private static volatile Path serverConfigPath;

    public ConfigMigrationsClientTestMod(ModContainer container) {
        RealNeoForgeConfigTest.register(container);
        RealIcebergConfigTest.register();
        RealAutoConfigTest.register();
        RealFzzyConfigTest.load();
        RealOwoConfigTest.load();
        RealResourcefulConfigTest.register();
        RealSuperMartijn642ConfigTest.register();
        RealWunderLibConfigTest.load();

        NeoForge.EVENT_BUS.addListener(this::onServerStarted);
    }

    private void onServerStarted(ServerStartedEvent event) {
        serverConfigPath = ModConfigs.getConfigSet(ModConfig.Type.SERVER).stream()
                .filter(candidate -> candidate.getModId().equals(MOD_ID))
                .filter(candidate -> candidate.getFileName().equals(RealNeoForgeConfigTest.SERVER_FILE))
                .findFirst()
                .map(ModConfig::getFullPath)
                .orElse(null);
    }

    static Path serverConfigPath() {
        return serverConfigPath;
    }
}
