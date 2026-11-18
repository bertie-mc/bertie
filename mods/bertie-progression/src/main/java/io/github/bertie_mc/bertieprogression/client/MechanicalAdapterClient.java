package io.github.bertie_mc.bertieprogression.client;

import io.github.bertie_mc.bertieprogression.BertieProgression;
import io.github.bertie_mc.bertieprogression.cybernetics.MechanicalAdapterRegistry;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

@EventBusSubscriber(modid = BertieProgression.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class MechanicalAdapterClient {
    @SubscribeEvent
    public static void setup(net.neoforged.fml.event.lifecycle.FMLClientSetupEvent event) {
        if (ModList.get().isLoaded("create") && ModList.get().isLoaded("oritech")) {
            event.enqueueWork(MechanicalAdapterRenderer::init);
        }
    }

    @SubscribeEvent
    public static void renderers(EntityRenderersEvent.RegisterRenderers event) {
        if (ModList.get().isLoaded("create") && ModList.get().isLoaded("oritech")) {
            event.registerBlockEntityRenderer(MechanicalAdapterRegistry.ENTITY.get(), MechanicalAdapterRenderer::new);
        }
    }

    private MechanicalAdapterClient() {}
}
