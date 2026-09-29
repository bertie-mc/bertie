package io.github.bertie_mc.fletching.client;

import io.github.bertie_mc.fletching.BertieFletching;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

@EventBusSubscriber(modid = BertieFletching.ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class ClientEvents {
    private ClientEvents() {}

    @SubscribeEvent
    public static void screens(RegisterMenuScreensEvent event) {
        event.register(BertieFletching.MENU.get(), FletchingScreen::new);
    }

    @SubscribeEvent
    public static void models(ModelEvent.RegisterAdditional event) {
        CoatingModels.TYPES.forEach(type -> event.register(CoatingModels.modelId(type)));
    }

    @SubscribeEvent
    public static void baked(ModelEvent.ModifyBakingResult event) {
        CoatingModels.reload(event.getModels());
        PartVisuals.reload(event.getModels());
    }
}
