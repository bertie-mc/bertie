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
        for (var part : io.github.bertie_mc.fletching.PartCatalog.ALL)
            if (part.tint() != -1)
                for (String view : MaterialLayers.VIEWS)
                    event.register(net.minecraft.client.resources.model.ModelResourceLocation.standalone(
                            MaterialLayers.id(part, view).withPrefix("item/")));
    }

    @SubscribeEvent
    public static void spriteSources(net.neoforged.neoforge.client.event.RegisterSpriteSourceTypesEvent event) {
        event.register(
                net.minecraft.resources.ResourceLocation.parse("bertiefletching:material_layers"), MaterialLayers.TYPE);
    }

    @SubscribeEvent
    public static void baked(ModelEvent.ModifyBakingResult event) {
        CoatingModels.reload(event.getModels());
        PartVisuals.reload(event.getModels());
    }
}
