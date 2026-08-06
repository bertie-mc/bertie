package io.github.bertie_mc.shortcircuitfix.test;

import io.github.bertie_mc.testing.client.ClientTest;
import io.github.bertie_mc.testing.client.context.ClientTestContext;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.client.ChunkRenderTypeSet;

public final class ShortCircuitClientTests {
    private ShortCircuitClientTests() {}

    @ClientTest
    public static void registersTranslucentRenderLayers(ClientTestContext context) {
        context.runOnClient(client -> {
            assertTranslucent("circuit");
            assertTranslucent("integrated_circuit");
        });
    }

    @SuppressWarnings("deprecation")
    private static void assertTranslucent(String path) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath("short_circuit", path);
        Block block = BuiltInRegistries.BLOCK.get(id);
        if (block == Blocks.AIR) {
            throw new AssertionError("Short Circuit block is not registered: " + id);
        }
        ChunkRenderTypeSet actual = ItemBlockRenderTypes.getRenderLayers(block.defaultBlockState());
        if (!actual.contains(RenderType.translucent()) || actual.asList().size() != 1) {
            throw new AssertionError(id + " uses " + actual.asList() + " instead of only the translucent render layer");
        }
    }
}
