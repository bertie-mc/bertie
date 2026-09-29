package io.github.bertie_mc.fletching.client;

import com.fletchery.mod.client.render.LayeredBakedModelFactory;
import com.fletchery.mod.entity.CustomArrowEntity;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.vertex.PoseStack;
import io.github.bertie_mc.fletching.*;
import java.io.IOException;
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

public final class PartVisuals {
    private static volatile Map<ModelResourceLocation, BakedModel> MODELS = Map.of();
    private static final Map<String, BakedModel> CACHE = new HashMap<>();
    private static final Map<String, ResourceLocation> TEXTURES = new HashMap<>();

    private PartVisuals() {}

    public static void reload(Map<ModelResourceLocation, BakedModel> models) {
        MODELS = Map.copyOf(models);
        CACHE.clear();
        Minecraft.getInstance().execute(() -> {
            TEXTURES.values()
                    .forEach(id -> Minecraft.getInstance().getTextureManager().release(id));
            TEXTURES.clear();
        });
    }

    public static BakedModel model(
            String type, int stage, String feather, String shaft, String tip, String extra, String potion) {
        var f = part(0, feather);
        var s = part(1, shaft);
        var t = part(2, tip);
        var e = part(3, extra);
        if (f == null
                || s == null
                || t == null
                || e == null && extra != null && !extra.isEmpty()
                || potion != null && !potion.isEmpty() && !CoatingColor.isEncoded(potion)) return null;
        if (!LayeredBakedModelFactory.isReady()) return null;
        String key = type + stage + "|" + feather + "|" + shaft + "|" + tip + "|" + extra + "|" + potion;
        return CACHE.computeIfAbsent(key, ignored -> {
            String folder =
                    type.equals("BOW") ? "bow_layers" : type.equals("CROSSBOW") ? "crossbow_layers" : "arrow_layers";
            String suffix = type.equals("BOW") ? "_" + Math.clamp(stage, 0, 2) : "";
            BakedModel base =
                    switch (type) {
                        case "BOW" -> LayeredBakedModelFactory.buildBow(stage, List.of());
                        case "CROSSBOW" ->
                            LayeredBakedModelFactory.buildCrossbow(List.of(id("item/crossbow_layers/arrow_base")));
                        default -> LayeredBakedModelFactory.buildArrow(List.of());
                    };
            if (base == null) return null;
            List<Layer> layers = new ArrayList<>();
            add(layers, f, folder, "feather", suffix);
            add(layers, s, folder, "shaft", suffix);
            add(layers, t, folder, "tip", suffix);
            if (e != null) add(layers, e, folder, "effect", suffix);
            BakedModel result = new Composite(base, layers);
            return CoatingColor.isEncoded(potion)
                    ? CoatingModels.coat(
                            result,
                            type.equals("BOW")
                                    ? "bow_" + Math.clamp(stage, 0, 2)
                                    : type.equals("CROSSBOW") ? "crossbow" : "arrow",
                            CoatingColor.decode(potion).orElse(0xffffff))
                    : result;
        });
    }

    private static PartCatalog.Part part(int slot, String id) {
        if (id == null || id.isEmpty()) return null;
        return id.contains(":")
                ? PartCatalog.find(slot, id)
                : PartCatalog.inSlot(slot).stream()
                        .filter(part -> part.itemId()
                                .substring(part.itemId().indexOf(':') + 1)
                                .equals(id))
                        .findFirst()
                        .orElse(null);
    }

    private static void add(List<Layer> layers, PartCatalog.Part part, String folder, String prefix, String suffix) {
        String path = "item/" + folder + "/" + prefix + "_" + part.visual() + suffix;
        String view =
                folder.equals("bow_layers") ? "bow" + suffix : folder.equals("crossbow_layers") ? "crossbow" : "arrow";
        ResourceLocation modelId = part.tint() == -1
                ? id("__layer_stitch/" + path.replace('/', '_'))
                : MaterialLayers.id(part, view).withPrefix("item/");
        BakedModel model = MODELS.get(ModelResourceLocation.standalone(modelId));
        if (model != null) layers.add(new Layer(model, -1));
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("fletchery_expanded", path);
    }

    public static ResourceLocation texture(CustomArrowEntity arrow) {
        var data = arrow.getCustomProperties();
        var p = ArrowProfile.read(data);
        String key = data.getString("feather") + "|" + data.getString("shaft") + "|" + data.getString("tip") + "|"
                + data.getString("effect");
        ResourceLocation base = TEXTURES.computeIfAbsent(key, ignored -> {
            NativeImage image = new NativeImage(32, 32, true);
            try {
                overlay(image, p.shaft(), "shaft/" + p.shaft().visual() + "_shaft");
                overlay(
                        image,
                        p.tip(),
                        "tip/" + p.tip().visual().replace("_shard", "").replace("_ingot", "") + "_tip");
                String feather =
                        switch (p.feather().visual()) {
                            case "phantom_membrane" -> "phantom";
                            case "wheat" -> "wheat";
                            default -> "feather";
                        };
                overlay(image, p.feather(), "feather/" + feather + "_tail");
                if (p.extra() != null) {
                    String effect =
                            switch (p.extra().visual()) {
                                case "slime_ball" -> "slime";
                                case "glowstone_dust" -> "glowstone";
                                case "lapis_lazuli" -> "lapis";
                                default -> p.extra().visual();
                            };
                    overlay(image, p.extra(), "effect/" + effect + "_effect");
                }
                return Minecraft.getInstance().getTextureManager().register("bertie_arrow", new DynamicTexture(image));
            } catch (IOException error) {
                image.close();
                return ResourceLocation.withDefaultNamespace("textures/entity/projectiles/arrow.png");
            }
        });
        return data.getBoolean("bertieCoating")
                ? CoatingModels.coatTexture(base, data.getInt("bertiePotionColor"))
                : base;
    }

    private static void overlay(NativeImage to, PartCatalog.Part part, String path) throws IOException {
        try (var stream = Minecraft.getInstance().getResourceManager().open(id("textures/layers/" + path + ".png"));
                var from = NativeImage.read(stream)) {
            if (part.tint() != -1)
                MaterialLayers.recolor(
                        from, MaterialLayers.palette(Minecraft.getInstance().getResourceManager(), part));
            for (int y = 0; y < Math.min(to.getHeight(), from.getHeight()); y++)
                for (int x = 0; x < Math.min(to.getWidth(), from.getWidth()); x++) {
                    int pixel = from.getPixelRGBA(x, y);
                    if ((pixel >>> 24) == 0) continue;
                    to.blendPixel(x, y, pixel);
                }
        }
    }

    private record Layer(BakedModel model, int tint) {}

    private static final class Composite extends BakedModelWrapper<BakedModel> {
        private final List<Layer> layers;

        Composite(BakedModel base, List<Layer> layers) {
            super(base);
            this.layers = List.copyOf(layers);
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

        @Override
        public List<BakedQuad> getQuads(
                BlockState state, Direction side, RandomSource random, ModelData data, RenderType type) {
            return getQuads(state, side, random);
        }

        @Override
        public List<BakedQuad> getQuads(BlockState state, Direction side, RandomSource random) {
            List<BakedQuad> result = new ArrayList<>(originalModel.getQuads(state, side, random));
            int index = 0;
            for (Layer layer : layers) {
                index++;
                for (BakedQuad quad : layer.model.getQuads(state, side, random)) {
                    int[] data = quad.getVertices().clone();
                    for (int vertex = 0; vertex < 4; vertex++) {
                        int offset = vertex * (data.length / 4);
                        float z = Float.intBitsToFloat(data[offset + 2]);
                        data[offset + 2] = Float.floatToRawIntBits(z + (z >= .5f ? 1 : -1) * index * .0001f);
                        if (layer.tint != -1)
                            data[offset + 3] = 0xff000000
                                    | (layer.tint & 255) << 16
                                    | layer.tint & 0xff00
                                    | layer.tint >> 16 & 255;
                    }
                    result.add(new BakedQuad(
                            data,
                            quad.getTintIndex(),
                            quad.getDirection(),
                            quad.getSprite(),
                            quad.isShade(),
                            quad.hasAmbientOcclusion()));
                }
            }
            return result;
        }
    }
}
