package io.github.bertie_mc.emi.integration.unbound;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

@EventBusSubscriber(modid = "bertieemi", bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class UnboundPonderClient {
    @SubscribeEvent
    public static void setup(FMLClientSetupEvent event) {
        if (ModList.get().isLoaded("unbound") && ModList.get().isLoaded("create"))
            event.enqueueWork(UnboundPonders::register);
    }

    private UnboundPonderClient() {}
}
