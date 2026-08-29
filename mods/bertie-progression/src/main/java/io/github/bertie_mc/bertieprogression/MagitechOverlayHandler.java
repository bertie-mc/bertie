package io.github.bertie_mc.bertieprogression;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;

/**
 * Magitech's HUD stays off the screen.
 *
 * <p>The mod draws five overlays of its own - a mana gauge, a mana container readout, a spell
 * gauge, a radial spell menu and a tool belt - and offers no setting to turn any of them off. Every
 * one is registered as a named GUI layer, so cancelling the layer by namespace takes the lot
 * without naming them one by one, and without touching the workbench screens the mod is unusable
 * without.
 */
@EventBusSubscriber(modid = BertieProgression.MODID, value = Dist.CLIENT)
public final class MagitechOverlayHandler {

    private static final String MAGITECH = "magitech";

    private MagitechOverlayHandler() {
    }

    @SubscribeEvent
    static void onRenderLayer(RenderGuiLayerEvent.Pre event) {
        if (MAGITECH.equals(event.getName().getNamespace())) {
            event.setCanceled(true);
        }
    }
}
