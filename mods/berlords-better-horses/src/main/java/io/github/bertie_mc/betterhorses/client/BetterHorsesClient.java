package io.github.bertie_mc.betterhorses.client;

import io.github.bertie_mc.betterhorses.BetterHorses;
import net.minecraft.client.renderer.entity.HorseRenderer;
import net.minecraft.world.entity.EntityType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

@EventBusSubscriber(modid = BetterHorses.ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class BetterHorsesClient {
    private BetterHorsesClient() {}

    @SubscribeEvent
    public static void screens(RegisterMenuScreensEvent event) {
        event.register(BetterHorses.HORSE_MENU.get(), BetterHorseScreen::new);
    }

    @SubscribeEvent
    public static void layers(EntityRenderersEvent.AddLayers event) {
        if (event.getRenderer(EntityType.HORSE) instanceof HorseRenderer renderer)
            renderer.addLayer(new HorseEquipmentLayer(renderer));
    }
}
