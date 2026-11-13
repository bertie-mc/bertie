package io.github.bertie_mc.bertieprogression.client;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import io.github.bertie_mc.bertieprogression.BertieProgression;
import java.util.ArrayList;
import java.util.List;
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
        private static final int PARITY_SHIFT = 18;
        private static final Direction[][] DIAGONALS = {
            {Direction.DOWN, Direction.NORTH}, {Direction.DOWN, Direction.SOUTH},
            {Direction.DOWN, Direction.WEST}, {Direction.DOWN, Direction.EAST},
            {Direction.UP, Direction.NORTH}, {Direction.UP, Direction.SOUTH},
            {Direction.UP, Direction.WEST}, {Direction.UP, Direction.EAST},
            {Direction.NORTH, Direction.WEST}, {Direction.NORTH, Direction.EAST},
            {Direction.SOUTH, Direction.WEST}, {Direction.SOUTH, Direction.EAST}
        };
        private final TextureAtlasSprite texture;
        private final TextureAtlasSprite outline;
        private final LoadingCache<Integer, List<BakedQuad>> cache = CacheBuilder.newBuilder()
                .maximumSize(512)
                .build(new CacheLoader<>() {
                    @Override
                    public List<BakedQuad> load(Integer key) {
                        return build(key);
                    }
                });

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
            for (int i = 0; i < DIAGONALS.length; i++) {
                if (level.getBlockState(pos.relative(DIAGONALS[i][0]).relative(DIAGONALS[i][1]))
                        .is(state.getBlock())) {
                    key |= 1 << (6 + i);
                }
            }
            key |= (pos.getX() & 1) << PARITY_SHIFT;
            key |= (pos.getY() & 1) << (PARITY_SHIFT + 1);
            key |= (pos.getZ() & 1) << (PARITY_SHIFT + 2);
            return data.derive().with(CONNECTIONS, key).build();
        }

        @Override
        public List<BakedQuad> getQuads(
                BlockState state, Direction side, RandomSource random, ModelData data, RenderType renderType) {
            if (state == null) return originalModel.getQuads(state, side, random, data, renderType);
            if (side != null) return List.of();
            Integer key = data.get(CONNECTIONS);
            return cache.getUnchecked(key == null ? 0 : key);
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
            for (Direction outward : Direction.values()) {
                if (connected(key, outward)) continue;
                float[][] inner = bounds(key, outward, 0.5f);
                float[][] outer = bounds(key, outward, 0.025f);
                result.addAll(surface(inner[0], inner[1], outward, key));
                result.add(quad(outer[0], outer[1], outward, outline));
            }
            return List.copyOf(result);
        }

        private static boolean connected(int key, Direction direction) {
            return (key & (1 << direction.ordinal())) != 0;
        }

        private static boolean diagonal(int key, Direction a, Direction b) {
            for (int i = 0; i < DIAGONALS.length; i++) {
                if ((DIAGONALS[i][0] == a && DIAGONALS[i][1] == b) || (DIAGONALS[i][0] == b && DIAGONALS[i][1] == a)) {
                    return (key & (1 << (6 + i))) != 0;
                }
            }
            return false;
        }

        private static float[][] bounds(int key, Direction outward, float inset) {
            float[] low = {inset, inset, inset};
            float[] high = {16 - inset, 16 - inset, 16 - inset};
            for (Direction tangent : Direction.values()) {
                if (tangent.getAxis() == outward.getAxis()) continue;
                float margin = !connected(key, tangent) ? inset : diagonal(key, outward, tangent) ? -inset : 0;
                int axis = tangent.getAxis().ordinal();
                // Concave faces extend past the cell boundary to meet the neighboring inset plane.
                if (tangent.getAxisDirection() == Direction.AxisDirection.NEGATIVE) low[axis] = margin;
                else high[axis] = 16 - margin;
            }
            return new float[][] {low, high};
        }

        private static BakedQuad quad(float[] low, float[] high, Direction outward, TextureAtlasSprite sprite) {
            return new FaceBakery()
                    .bakeQuad(
                            new Vector3f(high[0], high[1], high[2]),
                            new Vector3f(low[0], low[1], low[2]),
                            new BlockElementFace(null, -1, "", new BlockFaceUV(new float[] {0, 0, 16, 16}, 0)),
                            sprite,
                            outward.getOpposite(),
                            BlockModelRotation.X0_Y0,
                            null,
                            false);
        }

        private List<BakedQuad> surface(float[] low, float[] high, Direction outward, int key) {
            BakedQuad base = quad(low, high, outward, texture);
            int[] vertices = base.getVertices();
            int stride = vertices.length / 4;
            List<float[]> polygon = new ArrayList<>();
            for (int i = 0; i < 4; i++) {
                polygon.add(new float[] {
                    Float.intBitsToFloat(vertices[i * stride]),
                    Float.intBitsToFloat(vertices[i * stride + 1]),
                    Float.intBitsToFloat(vertices[i * stride + 2])
                });
            }
            List<List<float[]>> patches = new ArrayList<>();
            patches.add(polygon);
            // Split at reflection folds: interpolating across a fold would skip its peak.
            for (int component = 0; component < 2; component++) {
                for (int cut = 0; cut <= 4; cut++) {
                    List<List<float[]>> next = new ArrayList<>();
                    for (List<float[]> patch : patches) {
                        float min = Float.POSITIVE_INFINITY, max = Float.NEGATIVE_INFINITY;
                        for (float[] p : patch) {
                            float value = project(p, key, component);
                            min = Math.min(min, value);
                            max = Math.max(max, value);
                        }
                        if (min < cut && max > cut) {
                            next.add(clip(patch, key, component, cut, false));
                            next.add(clip(patch, key, component, cut, true));
                        } else {
                            next.add(patch);
                        }
                    }
                    patches = next;
                }
            }
            List<BakedQuad> result = new ArrayList<>();
            for (List<float[]> patch : patches) {
                if (patch.size() == 4) {
                    result.add(texturedQuad(base, patch, key));
                } else {
                    for (int i = 1; i + 1 < patch.size(); i++) {
                        result.add(texturedQuad(
                                base, List.of(patch.get(0), patch.get(i), patch.get(i + 1), patch.get(i + 1)), key));
                    }
                }
            }
            return result;
        }

        private static float project(float[] p, int key, int component) {
            // One continuous projection for all six faces, including floor and ceiling.
            int axis = component == 0 ? 0 : 2;
            return p[axis] + p[1] + ((key >> (PARITY_SHIFT + axis)) & 1) + ((key >> (PARITY_SHIFT + 1)) & 1);
        }

        private static List<float[]> clip(List<float[]> polygon, int key, int component, float cut, boolean positive) {
            List<float[]> result = new ArrayList<>();
            float[] previous = polygon.get(polygon.size() - 1);
            float previousDistance = project(previous, key, component) - cut;
            for (float[] current : polygon) {
                float distance = project(current, key, component) - cut;
                boolean previousInside = positive ? previousDistance >= 0 : previousDistance <= 0;
                boolean inside = positive ? distance >= 0 : distance <= 0;
                if (previousInside != inside) {
                    float t = previousDistance / (previousDistance - distance);
                    result.add(new float[] {
                        previous[0] + t * (current[0] - previous[0]),
                        previous[1] + t * (current[1] - previous[1]),
                        previous[2] + t * (current[2] - previous[2])
                    });
                }
                if (inside) result.add(current);
                previous = current;
                previousDistance = distance;
            }
            return result;
        }

        private BakedQuad texturedQuad(BakedQuad base, List<float[]> points, int key) {
            int[] template = base.getVertices();
            int[] vertices = new int[template.length];
            int stride = vertices.length / 4;
            for (int i = 0; i < 4; i++) {
                System.arraycopy(template, 0, vertices, i * stride, stride);
                float[] p = points.get(i);
                for (int axis = 0; axis < 3; axis++) {
                    vertices[i * stride + axis] = Float.floatToRawIntBits(p[axis]);
                }
                float u = reflect(project(p, key, 0));
                float v = reflect(project(p, key, 1));
                // Sample interior texel centers without the native bevel or dark frame.
                vertices[i * stride + 4] = Float.floatToRawIntBits(texture.getU((1.5f + 12 * u) / 16));
                vertices[i * stride + 5] = Float.floatToRawIntBits(texture.getV((1.5f + 12 * v) / 16));
            }
            return new BakedQuad(vertices, -1, base.getDirection(), texture, false, false);
        }

        private static float reflect(float value) {
            int cell = (int) Math.floor(value);
            float fraction = value - cell;
            return (cell & 1) == 0 ? fraction : 1 - fraction;
        }
    }
}
