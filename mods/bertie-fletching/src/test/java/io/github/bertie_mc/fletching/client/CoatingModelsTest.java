package io.github.bertie_mc.fletching.client;

import static org.junit.jupiter.api.Assertions.*;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.List;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.junit.jupiter.api.Test;

class CoatingModelsTest {
    @Test
    void potionPixelsFollowCompletedEffectGeometryAndKeepTheirOwnTint() {
        BakedQuad effectQuad = quad(.53125f);
        BakedQuad coatingFront = quad(.53125f);
        BakedQuad coatingBack = quad(.46875f);
        BakedModel base = new TestModel(List.of(effectQuad));
        BakedModel mask = new TestModel(List.of(coatingFront, coatingBack));
        BakedModel result = CoatingModels.compose(base, mask, 0x123456);
        List<BakedQuad> quads = result.getQuads(null, null, RandomSource.create(), ModelData.EMPTY, null);
        assertEquals(3, quads.size());
        assertSame(effectQuad, quads.getFirst());
        assertEquals(0xff563412, quads.get(1).getVertices()[3]);
        assertTrue(Float.intBitsToFloat(quads.get(1).getVertices()[2]) > .53125f);
        assertTrue(Float.intBitsToFloat(quads.get(2).getVertices()[2]) < .46875f);
        assertEquals(-1, coatingFront.getVertices()[3], "source mask remains unmodified for the next potion color");
        assertSame(result, result.getRenderPasses(ItemStack.EMPTY, false).getFirst());
        assertSame(result, result.applyTransform(ItemDisplayContext.GUI, new PoseStack(), false));
    }

    @Test
    void coatingResourcesExistForEveryRenderSurface() {
        for (String type : CoatingModels.TYPES) {
            assertNotNull(getClass().getResource("/assets/bertiefletching/models/item/coating/" + type + ".json"));
            assertNotNull(getClass().getResource("/assets/bertiefletching/textures/coating/" + type + ".png"));
        }
    }

    private static BakedQuad quad(float z) {
        int[] vertices = new int[32];
        for (int i = 0; i < 4; i++) {
            vertices[i * 8 + 2] = Float.floatToRawIntBits(z);
            vertices[i * 8 + 3] = -1;
        }
        return new BakedQuad(vertices, -1, Direction.SOUTH, null, false);
    }

    private record TestModel(List<BakedQuad> quads) implements BakedModel {
        @Override
        public List<BakedQuad> getQuads(BlockState state, Direction side, RandomSource random) {
            return quads;
        }

        @Override
        public boolean useAmbientOcclusion() {
            return false;
        }

        @Override
        public boolean isGui3d() {
            return false;
        }

        @Override
        public boolean usesBlockLight() {
            return false;
        }

        @Override
        public boolean isCustomRenderer() {
            return false;
        }

        @Override
        public TextureAtlasSprite getParticleIcon() {
            return null;
        }

        @Override
        public ItemTransforms getTransforms() {
            return ItemTransforms.NO_TRANSFORMS;
        }

        @Override
        public ItemOverrides getOverrides() {
            return ItemOverrides.EMPTY;
        }
    }
}
