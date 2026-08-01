package com.berlord.shortcircuitfix;

import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

/**
 * Short Circuit's blockstates apply vanilla stained-glass models as the circuit
 * shell, but only its Fabric build registers the blocks on the translucent render
 * layer (BlockRenderLayerMap in ShortCircuitFabric). The NeoForge build skips that
 * step, so the shell renders on the solid layer and looks like an opaque colored
 * block. This mod performs the missing registration.
 */
@Mod(value = ShortCircuitFix.MOD_ID, dist = Dist.CLIENT)
public class ShortCircuitFix {
    public static final String MOD_ID = "shortcircuitfix";

    public ShortCircuitFix(IEventBus modBus) {
        modBus.addListener(this::onClientSetup);
    }

    private void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            setTranslucent("circuit");
            setTranslucent("integrated_circuit");
        });
    }

    @SuppressWarnings("deprecation")
    private static void setTranslucent(String path) {
        Block block = BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("short_circuit", path));
        if (block != Blocks.AIR) {
            ItemBlockRenderTypes.setRenderLayer(block, RenderType.translucent());
        }
    }
}
