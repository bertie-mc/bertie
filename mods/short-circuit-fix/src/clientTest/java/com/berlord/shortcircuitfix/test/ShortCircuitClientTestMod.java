package com.berlord.shortcircuitfix.test;

import com.mojang.logging.LogUtils;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLLoadCompleteEvent;
import net.neoforged.neoforge.client.ChunkRenderTypeSet;
import org.slf4j.Logger;

@Mod(value = ShortCircuitClientTestMod.MOD_ID, dist = Dist.CLIENT)
public final class ShortCircuitClientTestMod {
    static final String MOD_ID = "shortcircuitfixtest";
    private static final String SUCCESS_MARKER = "SHORT_CIRCUIT_RENDER_LAYERS_OK";
    private static final Logger LOGGER = LogUtils.getLogger();

    public ShortCircuitClientTestMod(IEventBus modBus) {
        modBus.addListener(this::onLoadComplete);
    }

    private void onLoadComplete(FMLLoadCompleteEvent event) {
        event.enqueueWork(() -> {
            assertTranslucent("circuit");
            assertTranslucent("integrated_circuit");
            LOGGER.info(SUCCESS_MARKER);
        });
    }

    @SuppressWarnings("deprecation")
    private static void assertTranslucent(String path) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath("short_circuit", path);
        Block block = BuiltInRegistries.BLOCK.get(id);
        if (block == Blocks.AIR) {
            throw new IllegalStateException("Short Circuit block is not registered: " + id);
        }
        ChunkRenderTypeSet actual = ItemBlockRenderTypes.getRenderLayers(block.defaultBlockState());
        if (!actual.contains(RenderType.translucent()) || actual.asList().size() != 1) {
            throw new IllegalStateException(
                    id + " uses " + actual.asList() + " instead of only the translucent render layer");
        }
    }
}
