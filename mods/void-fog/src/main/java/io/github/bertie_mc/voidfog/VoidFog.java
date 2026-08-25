package io.github.bertie_mc.voidfog;

import io.github.bertie_mc.voidfog.client.SkyProximity;
import io.github.bertie_mc.voidfog.client.VoidFogMotes;
import io.github.bertie_mc.voidfog.client.VoidFogRenderer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.common.NeoForge;

/** The void fog the game drew near the world floor before 1.8, back and configurable per dimension. */
@Mod(value = VoidFog.MOD_ID, dist = Dist.CLIENT)
public final class VoidFog {
    public static final String MOD_ID = "voidfog";

    public VoidFog(IEventBus ignored, ModContainer container) {
        container.registerConfig(ModConfig.Type.CLIENT, VoidFogConfig.SPEC);
        // LOWEST, and receiving cancelled events, and neither is optional in a real pack.
        //
        // Eighteen other mods in this one listen to ViewportEvent.RenderFog and cancel it -
        // Create, Twilight Forest, Alex's Caves, Iron's Spellbooks, Magitech and more, each
        // for its own biome or ability. A cancelled event does not reach later listeners
        // unless they ask for it, so at the default priority this fog simply never ran, and
        // no amount of adjusting its distances changed anything on screen. Depth fog has to
        // have the last word: it is the floor of the world, not a local effect.
        NeoForge.EVENT_BUS.addListener(
                EventPriority.LOWEST, true, ViewportEvent.RenderFog.class, VoidFogRenderer::onRenderFog);
        NeoForge.EVENT_BUS.addListener(
                EventPriority.LOWEST, ViewportEvent.ComputeFogColor.class, VoidFogRenderer::onFogColor);
        NeoForge.EVENT_BUS.addListener(VoidFogMotes::onClientTick);
        // The eased sky factor is world state; carrying it into the next world would open a player
        // underground in fog that belongs to where they logged out.
        NeoForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingIn event) -> SkyProximity.reset());
    }
}
