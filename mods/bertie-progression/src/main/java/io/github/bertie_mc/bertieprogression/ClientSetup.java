package io.github.bertie_mc.bertieprogression;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

/**
 * Client-only setup. Registers the ponder scenes: the Deep Waters Shrine, and Magitech's Athanor
 * Altar.
 *
 * <p>Ponder ships JarJar-embedded inside Create, so it is an optional dependency: the plugin class
 * is referenced ONLY from inside a lambda that runs after the ModList check, so its ponder imports
 * are never classloaded when Create is absent. The Athanor scene additionally needs Magitech, whose
 * pillar it attaches to.
 */
@EventBusSubscriber(modid = BertieProgression.MODID, value = Dist.CLIENT)
public final class ClientSetup {

    @SubscribeEvent
    static void onClientSetup(FMLClientSetupEvent event) {
        if (!ModList.get().isLoaded("ponder")) return;
        event.enqueueWork(() -> io.github.bertie_mc.bertieprogression.shrine.ShrinePonderPlugin.register());
        if (ModList.get().isLoaded("magitech")) {
            event.enqueueWork(() -> io.github.bertie_mc.bertieprogression.athanor.AthanorPonderPlugin.register());
        }
    }

    private ClientSetup() {}
}
