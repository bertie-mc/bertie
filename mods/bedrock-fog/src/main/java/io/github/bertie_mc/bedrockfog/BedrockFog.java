package io.github.bertie_mc.bedrockfog;

import io.github.bertie_mc.bedrockfog.client.BedrockFogRenderer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;

/** The void fog the game drew near the world floor before 1.8, back and configurable per dimension. */
@Mod(value = BedrockFog.MOD_ID, dist = Dist.CLIENT)
public final class BedrockFog {
    public static final String MOD_ID = "bedrockfog";

    public BedrockFog(IEventBus ignored, ModContainer container) {
        container.registerConfig(ModConfig.Type.CLIENT, BedrockFogConfig.SPEC);
        NeoForge.EVENT_BUS.addListener(BedrockFogRenderer::onRenderFog);
        NeoForge.EVENT_BUS.addListener(BedrockFogRenderer::onFogColor);
    }
}
