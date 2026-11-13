package io.github.bertie_mc.bertieprogression.client;

import io.github.bertie_mc.bertieprogression.BertieProgression;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockElementFace;
import net.minecraft.client.renderer.block.model.BlockFaceUV;
import net.minecraft.client.renderer.block.model.FaceBakery;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.BlockModelRotation;
import net.minecraft.client.resources.model.Material;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.ChunkRenderTypeSet;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.model.BakedModelWrapper;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.client.model.data.ModelProperty;
import net.neoforged.neoforge.common.util.TriState;
import org.joml.Vector3f;

@EventBusSubscriber(modid = BertieProgression.MODID, value = Dist.CLIENT)
public final class NegativeMatterConnected {
    private NegativeMatterConnected() {}

    @SubscribeEvent
    public static void bake(ModelEvent.ModifyBakingResult event) {
        if (!ModList.get().isLoaded("anvilcraft")) return;
        var texture = event.getTextureGetter()
                .apply(new Material(
                        TextureAtlas.LOCATION_BLOCKS,
                        ResourceLocation.parse("anvilcraft:block/negative_matter_block")));
        var outline = event.getTextureGetter()
                .apply(new Material(
                        TextureAtlas.LOCATION_BLOCKS,
                        ResourceLocation.parse("anvilcraft:block/negative_matter_block_outline")));
        event.getModels()
                .replaceAll((id, model) -> id.id().equals(ResourceLocation.parse("anvilcraft:negative_matter_block"))
                                && !id.getVariant().equals("inventory")
                        ? new ConnectedModel(model, texture, outline)
                        : model);
    }

    public static final class ConnectedModel extends BakedModelWrapper<BakedModel> {
        private static final ModelProperty<Integer> CONNECTIONS = new ModelProperty<>();
        private final TextureAtlasSprite texture;
        private final TextureAtlasSprite outline;
        private final Map<Integer, List<BakedQuad>> cache = new ConcurrentHashMap<>();

        ConnectedModel(BakedModel original, TextureAtlasSprite texture, TextureAtlasSprite outline) {
            super(original);
            this.texture = texture;
            this.outline = outline;
        }

        @Override
        public ModelData getModelData(BlockAndTintGetter level, BlockPos pos, BlockState state, ModelData data) {
            int key = 0;
            for (Direction direction : Direction.values()) {
                if (level.getBlockState(pos.relative(direction)).is(state.getBlock())) {
                    key |= 1 << direction.ordinal();
                }
            }
            key |= (pos.getX() & 1) << 6;
            key |= (pos.getY() & 1) << 7;
            key |= (pos.getZ() & 1) << 8;
            return data.derive().with(CONNECTIONS, key).build();
        }

        @Override
        public List<BakedQuad> getQuads(
                BlockState state, Direction side, RandomSource random, ModelData data, RenderType renderType) {
            if (state == null) return originalModel.getQuads(state, side, random, data, renderType);
            if (side != null) return List.of();
            Integer key = data.get(CONNECTIONS);
            return cache.computeIfAbsent(key == null ? 0 : key, this::build);
        }

        @Override
        public List<BakedQuad> getQuads(BlockState state, Direction side, RandomSource random) {
            return getQuads(state, side, random, ModelData.EMPTY, null);
        }

        @Override
        public ChunkRenderTypeSet getRenderTypes(BlockState state, RandomSource random, ModelData data) {
            return ChunkRenderTypeSet.of(RenderType.cutout());
        }

        @Override
        public TriState useAmbientOcclusion(BlockState state, ModelData data, RenderType type) {
            return TriState.FALSE;
        }

        private List<BakedQuad> build(int key) {
            List<BakedQuad> result = new ArrayList<>();
            float[] innerLow = new float[3], innerHigh = new float[3];
            float[] outerLow = new float[3], outerHigh = new float[3];
            for (Direction d : Direction.values()) {
                int axis = d.getAxis().ordinal();
                boolean connected = (key & (1 << d.ordinal())) != 0;
                if (d.getAxisDirection() == Direction.AxisDirection.NEGATIVE) {
                    innerLow[axis] = connected ? 0 : 0.5f;
                    outerLow[axis] = connected ? 0 : 0.025f;
                } else {
                    innerHigh[axis] = connected ? 16 : 15.5f;
                    outerHigh[axis] = connected ? 16 : 15.975f;
                }
            }
            for (Direction outward : Direction.values()) {
                if ((key & (1 << outward.ordinal())) != 0) continue;
                result.add(quad(innerLow, innerHigh, outward, texture, key, true));
                result.add(quad(outerLow, outerHigh, outward, outline, key, false));
            }
            return List.copyOf(result);
        }

        private static BakedQuad quad(
                float[] low, float[] high, Direction outward, TextureAtlasSprite sprite, int key, boolean patterned) {
            BakedQuad quad = new FaceBakery()
                    .bakeQuad(
                            new Vector3f(high[0], high[1], high[2]),
                            new Vector3f(low[0], low[1], low[2]),
                            new BlockElementFace(null, -1, "", new BlockFaceUV(new float[] {0, 0, 16, 16}, 0)),
                            sprite,
                            outward.getOpposite(),
                            BlockModelRotation.X0_Y0,
                            null,
                            false);
            if (patterned) {
                int uAxis = outward.getAxis() == Direction.Axis.X ? 2 : 0;
                int vAxis = outward.getAxis() == Direction.Axis.Y ? 2 : 1;
                int[] vertices = quad.getVertices();
                int stride = vertices.length / 4;
                for (int i = 0; i < 4; i++) {
                    float u = Float.intBitsToFloat(vertices[i * stride + uAxis]);
                    float v = Float.intBitsToFloat(vertices[i * stride + vAxis]);
                    if ((key & (1 << (6 + uAxis))) != 0) u = 1 - u;
                    if ((key & (1 << (6 + vAxis))) != 0) v = 1 - v;
                    // Texel centers exclude the native bright bevel at row/column 14 and dark border.
                    // Reflection makes both sides of each join sample the same interior pixels.
                    vertices[i * stride + 4] = Float.floatToRawIntBits(sprite.getU((1.5f + 12 * u) / 16));
                    vertices[i * stride + 5] = Float.floatToRawIntBits(sprite.getV((1.5f + 12 * v) / 16));
                }
            }
            return quad;
        }
    }
}
