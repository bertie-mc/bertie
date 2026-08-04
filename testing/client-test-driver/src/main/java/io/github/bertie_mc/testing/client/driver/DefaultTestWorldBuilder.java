package io.github.bertie_mc.testing.client.driver;

import io.github.bertie_mc.testing.client.DedicatedServerContext;
import io.github.bertie_mc.testing.client.IntegratedWorldContext;
import io.github.bertie_mc.testing.client.TestWorldBuilder;
import java.util.Objects;
import java.util.Properties;
import java.util.function.Consumer;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.dedicated.DedicatedServer;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.levelgen.presets.WorldPreset;
import net.minecraft.world.level.levelgen.presets.WorldPresets;

final class DefaultTestWorldBuilder implements TestWorldBuilder {
    private final DefaultClientTestContext context;
    private boolean useConsistentSettings = true;
    private Consumer<WorldCreationUiState> settingsAdjuster = settings -> {};

    DefaultTestWorldBuilder(DefaultClientTestContext context) {
        this.context = Objects.requireNonNull(context);
    }

    @Override
    public TestWorldBuilder setUseConsistentSettings(boolean useConsistentSettings) {
        this.useConsistentSettings = useConsistentSettings;
        return this;
    }

    @Override
    public TestWorldBuilder adjustSettings(Consumer<WorldCreationUiState> settingsAdjuster) {
        this.settingsAdjuster = Objects.requireNonNull(settingsAdjuster);
        return this;
    }

    @Override
    public IntegratedWorldContext create() {
        TestScheduler.requireNoServerRunning();
        openCreateWorldScreen("bertie-client-test-world");
        context.clickScreenButton("selectWorld.create");
        ClientWorldLoading.waitForWorld(
                context,
                "the integrated world to load",
                client -> {
                    var server = client.getSingleplayerServer();
                    return client.level != null
                            && client.player != null
                            && server != null
                            && TestScheduler.canAcceptServerTasks(server);
                });

        var server = context.computeOnClient(client -> client.getSingleplayerServer());
        if (server == null) {
            throw new AssertionError("The integrated server disappeared after its world loaded");
        }
        return context.own(new DefaultIntegratedWorldContext(context, server));
    }

    @Override
    public DedicatedServerContext createServer(Properties serverProperties) {
        TestScheduler.requireNoServerRunning();
        Objects.requireNonNull(serverProperties);
        Properties properties = new Properties();
        properties.putAll(serverProperties);

        PreparedDedicatedWorld world;
        try (DedicatedWorldPreparation.Request request = DedicatedWorldPreparation.begin()) {
            openCreateWorldScreen(properties.getProperty("level-name", "world"));
            context.clickScreenButton("selectWorld.create");
            world = ClientWorldLoading.awaitCreation(
                    context,
                    "the dedicated world data to be written",
                    request.result());
        }
        context.setScreen(TitleScreen::new);

        InProcessDedicatedServer.Launch launch = InProcessDedicatedServer.begin(world, properties);
        try {
            DedicatedServer server = context.awaitInfrastructure(
                    "the dedicated server to start",
                    launch.started());
            context.waitForInfrastructure(
                    "the dedicated server test phase",
                    () -> TestScheduler.canAcceptServerTasks(server));
            return context.own(new DefaultDedicatedServerContext(context, launch));
        } catch (Throwable failure) {
            launch.abort();
            try {
                context.awaitInfrastructure(
                        "the failed dedicated server to stop", launch.stopped());
            } catch (Throwable cleanupFailure) {
                failure.addSuppressed(cleanupFailure);
            }
            throw failure;
        }
    }

    private void openCreateWorldScreen(String worldName) {
        context.runOnClient(client -> CreateWorldScreen.openFresh(client, client.screen));
        context.waitForScreen(CreateWorldScreen.class);
        context.runOnClient(client -> {
            CreateWorldScreen screen = CreateWorldScreen.class.cast(client.screen);
            WorldCreationUiState settings = screen.getUiState();
            settings.setName(worldName);
            if (useConsistentSettings) {
                applyConsistentSettings(settings);
            }
            settingsAdjuster.accept(settings);
        });
    }

    private static void applyConsistentSettings(WorldCreationUiState settings) {
        Holder<WorldPreset> flat = settings.getSettings()
                .worldgenLoadContext()
                .registryOrThrow(Registries.WORLD_PRESET)
                .getHolderOrThrow(WorldPresets.FLAT);
        settings.setWorldType(new WorldCreationUiState.WorldTypeEntry(flat));
        settings.setSeed("1");
        settings.setGenerateStructures(false);
        settings.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, null);
        settings.getGameRules().getRule(GameRules.RULE_WEATHER_CYCLE).set(false, null);
        settings.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false, null);
        settings.getGameRules().getRule(GameRules.RULE_SPAWN_RADIUS).set(0, null);
    }
}
