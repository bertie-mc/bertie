package io.github.bertie_mc.witheringwaver;

import io.github.bertie_mc.witheringwaver.entity.WitheringWaverEntity;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;

@EventBusSubscriber(modid = WitheringWaver.MOD_ID)
public final class WwModEvents {
    private WwModEvents() {}

    @SubscribeEvent
    public static void createAttributes(EntityAttributeCreationEvent event) {
        event.put(
                WwEntities.WITHERING_WAVER.get(),
                WitheringWaverEntity.createAttributes().build());
    }

    @SubscribeEvent
    public static void addCreativeTabItems(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.SPAWN_EGGS) {
            event.accept(WwItems.WITHERING_WAVER_SPAWN_EGG);
        }
    }
}
