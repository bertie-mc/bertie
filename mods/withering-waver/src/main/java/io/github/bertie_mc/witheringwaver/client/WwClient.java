package io.github.bertie_mc.witheringwaver.client;

import io.github.bertie_mc.witheringwaver.WitheringWaver;
import io.github.bertie_mc.witheringwaver.WwEntities;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

@EventBusSubscriber(modid = WitheringWaver.MOD_ID, value = Dist.CLIENT)
public final class WwClient {
    private WwClient() {}

    @SubscribeEvent
    public static void registerLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(WitheringWaverRenderer.LAYER, WitheringWaverModel::createLayer);
        event.registerLayerDefinition(ShrapnelRenderer.LAYER, ShrapnelModel::createLayer);
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(WwEntities.WITHERING_WAVER.get(), WitheringWaverRenderer::new);
        event.registerEntityRenderer(WwEntities.ORBITING_SKULL.get(), OrbitingSkullRenderer::new);
        event.registerEntityRenderer(WwEntities.SKULL_SHRAPNEL.get(), ShrapnelRenderer::new);
    }
}
