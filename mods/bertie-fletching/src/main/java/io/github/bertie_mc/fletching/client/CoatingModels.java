package io.github.bertie_mc.fletching.client;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.vertex.PoseStack;
import io.github.bertie_mc.fletching.BertieFletching;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.BakedModelWrapper;
import net.neoforged.neoforge.client.model.data.ModelData;

public final class CoatingModels {
    public static final List<String> TYPES = List.of("arrow", "bow_0", "bow_1", "bow_2", "crossbow");
    private static final Map<String, BakedModel> MASKS = new HashMap<>();
    private static final Map<Key, BakedModel> MODELS = new HashMap<>();
    private static final Map<TextureKey, ResourceLocation> TEXTURES = new HashMap<>();

    private CoatingModels() {}

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(BertieFletching.ID, path);
    }

    public static ModelResourceLocation modelId(String type) {
        return ModelResourceLocation.standalone(id("item/coating/" + type));
    }

    public static void reload(Map<ModelResourceLocation, BakedModel> models) {
        MODELS.clear();
        MASKS.clear();
        TYPES.forEach(type -> MASKS.put(type, models.get(modelId(type))));
        Minecraft.getInstance().execute(() -> {
            TEXTURES.values()
                    .forEach(texture ->
                            Minecraft.getInstance().getTextureManager().release(texture));
            TEXTURES.clear();
        });
    }

    public static BakedModel coat(BakedModel base, String type, int rgb) {
        BakedModel mask = MASKS.get(type);
        if (mask == null) return base;
        return MODELS.computeIfAbsent(new Key(base, type, rgb), key -> compose(base, mask, rgb));
    }

    static BakedModel compose(BakedModel base, BakedModel mask, int color) {
        return new CoatedModel(base, mask, color);
    }

    public static ResourceLocation coatTexture(ResourceLocation base, int color) {
        return TEXTURES.computeIfAbsent(new TextureKey(base, color), key -> {
            var minecraft = Minecraft.getInstance();
            var texture = minecraft.getTextureManager().getTexture(base);
            if (!(texture instanceof DynamicTexture dynamic) || dynamic.getPixels() == null) return base;
            NativeImage result = new NativeImage(
                    dynamic.getPixels().getWidth(), dynamic.getPixels().getHeight(), true);
            result.copyFrom(dynamic.getPixels());
            try (var stream = minecraft.getResourceManager().open(id("textures/coating/entity.png"));
                    NativeImage mask = NativeImage.read(stream)) {
                int abgr = 0xff000000 | (color & 255) << 16 | color & 0xff00 | color >> 16 & 255;
                for (int y = 0; y < Math.min(mask.getHeight(), result.getHeight()); y++)
                    for (int x = 0; x < Math.min(mask.getWidth(), result.getWidth()); x++) {
                        if ((mask.getPixelRGBA(x, y) >>> 24) != 0) result.setPixelRGBA(x, y, abgr);
                    }
                return minecraft.getTextureManager().register("bertie_coating", new DynamicTexture(result));
            } catch (java.io.IOException error) {
                result.close();
                return base;
            }
        });
    }

    private record Key(BakedModel base, String type, int color) {}

    private record TextureKey(ResourceLocation base, int color) {}

    private static final class CoatedModel extends BakedModelWrapper<BakedModel> {
        private final BakedModel mask;
        private final int color;

        private CoatedModel(BakedModel base, BakedModel mask, int color) {
            super(base);
            this.mask = mask;
            this.color = color;
        }

        @Override
        public List<BakedQuad> getQuads(BlockState state, Direction side, RandomSource random) {
            return coatedQuads(originalModel.getQuads(state, side, random), state, side, random);
        }

        @Override
        public List<BakedQuad> getQuads(
                BlockState state, Direction side, RandomSource random, ModelData data, RenderType renderType) {
            return coatedQuads(originalModel.getQuads(state, side, random, data, renderType), state, side, random);
        }

        @Override
        public List<BakedModel> getRenderPasses(ItemStack stack, boolean fabulous) {
            return List.of(this);
        }

        @Override
        public BakedModel applyTransform(ItemDisplayContext context, PoseStack pose, boolean left) {
            originalModel.applyTransform(context, pose, left);
            return this;
        }

        private List<BakedQuad> coatedQuads(
                List<BakedQuad> base, BlockState state, Direction side, RandomSource random) {
            List<BakedQuad> quads = new ArrayList<>(base);
            for (BakedQuad quad : mask.getQuads(state, side, random)) {
                int[] data = quad.getVertices().clone();
                int stride = data.length / 4;
                for (int vertex = 0; vertex < 4; vertex++) {
                    int offset = vertex * stride;
                    // Lift the coating just above both faces of the completed effect arrow.
                    float z = Float.intBitsToFloat(data[offset + 2]);
                    data[offset + 2] = Float.floatToRawIntBits(z + (z >= .5f ? .001f : -.001f));
                    data[offset + 3] = 0xff000000 | (color & 255) << 16 | color & 0xff00 | color >> 16 & 255;
                }
                quads.add(new BakedQuad(
                        data, -1, quad.getDirection(), quad.getSprite(), quad.isShade(), quad.hasAmbientOcclusion()));
            }
            return quads;
        }
    }
}
