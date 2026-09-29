package io.github.bertie_mc.fletching.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.serialization.MapCodec;
import io.github.bertie_mc.fletching.PartCatalog;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.renderer.texture.atlas.SpriteSource;
import net.minecraft.client.renderer.texture.atlas.SpriteSourceType;
import net.minecraft.client.resources.metadata.animation.FrameSize;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceMetadata;

/** Recolors layer masks from each ingredient's actual resource-pack palette before atlas baking. */
public final class MaterialLayers implements SpriteSource {
    public static final MapCodec<MaterialLayers> CODEC = MapCodec.unit(MaterialLayers::new);
    public static final SpriteSourceType TYPE = new SpriteSourceType(CODEC);
    public static final List<String> VIEWS = List.of("arrow", "bow_0", "bow_1", "bow_2", "crossbow");

    public static ResourceLocation id(PartCatalog.Part part, String view) {
        return ResourceLocation.parse("bertiefletching:parts/" + part.slot() + "_" + part.key() + "/" + view);
    }

    public static ResourceLocation template(PartCatalog.Part part, String view) {
        String folder = view.startsWith("bow") ? "bow_layers" : view + "_layers";
        String suffix = view.startsWith("bow") ? view.substring(3) : "";
        String prefix =
                switch (part.slot()) {
                    case 0 -> "feather";
                    case 1 -> "shaft";
                    case 2 -> "tip";
                    default -> "effect";
                };
        return ResourceLocation.parse(
                "fletchery_expanded:textures/item/" + folder + "/" + prefix + "_" + part.visual() + suffix + ".png");
    }

    @Override
    public void run(ResourceManager resources, Output output) {
        for (var part : PartCatalog.ALL) {
            if (part.tint() == -1) continue;
            int[] palette = palette(resources, part);
            for (String view : VIEWS) {
                var sprite = id(part, view);
                output.add(sprite, loader -> {
                    try (var stream = resources.open(template(part, view))) {
                        NativeImage image = NativeImage.read(stream);
                        recolor(image, palette);
                        return new SpriteContents(
                                sprite,
                                new FrameSize(image.getWidth(), image.getHeight()),
                                image,
                                ResourceMetadata.EMPTY);
                    } catch (IOException error) {
                        throw new IllegalStateException("Missing fletching layer " + sprite, error);
                    }
                });
            }
        }
    }

    @Override
    public SpriteSourceType type() {
        return TYPE;
    }

    public static int[] palette(ResourceManager resources, PartCatalog.Part part) {
        var textures = new HashMap<String, String>();
        ResourceLocation item = ResourceLocation.parse(part.itemId());
        readModel(resources, item.withPath("item/" + item.getPath()), textures, 0);
        String reference = textures.getOrDefault("layer0", textures.getOrDefault("all", textures.get("side")));
        for (int i = 0; reference != null && reference.startsWith("#") && i < 8; i++)
            reference = textures.get(reference.substring(1));
        ResourceLocation texture =
                reference == null ? item.withPath("item/" + item.getPath()) : ResourceLocation.tryParse(reference);
        if (texture != null) {
            try (var stream = resources.open(texture.withPath("textures/" + texture.getPath() + ".png"));
                    var image = NativeImage.read(stream)) {
                var colors = new ArrayList<Integer>();
                var vivid = new ArrayList<Integer>();
                for (int y = 0; y < image.getHeight(); y++)
                    for (int x = 0; x < image.getWidth(); x++) {
                        int p = image.getPixelRGBA(x, y);
                        if ((p >>> 24) < 200) continue;
                        int rgb = (p & 255) << 16 | p & 0xff00 | p >> 16 & 255;
                        colors.add(rgb);
                        int max = Math.max(rgb >> 16 & 255, Math.max(rgb >> 8 & 255, rgb & 255));
                        int min = Math.min(rgb >> 16 & 255, Math.min(rgb >> 8 & 255, rgb & 255));
                        if (max > 0 && (max - min) / (double) max > .25) vivid.add(rgb);
                    }
                // Ignore a bottle's neutral glass or a shard's white glint when color defines the material.
                if (vivid.size() >= colors.size() / 4 && vivid.size() >= 4) colors = vivid;
                colors.sort(Comparator.comparingInt(MaterialLayers::brightness));
                if (!colors.isEmpty())
                    return new int[] {
                        colors.get(colors.size() / 5), colors.get(colors.size() / 2), colors.get(colors.size() * 9 / 10)
                    };
            } catch (IOException ignored) {
            }
        }
        int c = part.tint();
        return new int[] {shade(c, .55), shade(c, .8), c};
    }

    private static void readModel(
            ResourceManager resources, ResourceLocation model, Map<String, String> textures, int depth) {
        if (depth > 8) return;
        try (var reader = resources.openAsReader(model.withPath("models/" + model.getPath() + ".json"))) {
            JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
            if (json.has("parent"))
                readModel(resources, ResourceLocation.parse(json.get("parent").getAsString()), textures, depth + 1);
            if (json.has("textures"))
                json.getAsJsonObject("textures")
                        .entrySet()
                        .forEach(e -> textures.put(e.getKey(), e.getValue().getAsString()));
        } catch (IOException ignored) {
        }
    }

    public static void recolor(NativeImage image, int[] palette) {
        int low = 255, high = 0;
        for (int y = 0; y < image.getHeight(); y++)
            for (int x = 0; x < image.getWidth(); x++) {
                int p = image.getPixelRGBA(x, y);
                if ((p >>> 24) == 0) continue;
                int b = Math.max(p & 255, Math.max(p >> 8 & 255, p >> 16 & 255));
                low = Math.min(low, b);
                high = Math.max(high, b);
            }
        for (int y = 0; y < image.getHeight(); y++)
            for (int x = 0; x < image.getWidth(); x++) {
                int p = image.getPixelRGBA(x, y);
                if ((p >>> 24) == 0) continue;
                int b = Math.max(p & 255, Math.max(p >> 8 & 255, p >> 16 & 255));
                double t = high == low ? .5 : (b - low) / (double) (high - low);
                int color = mix(palette[t < .5 ? 0 : 1], palette[t < .5 ? 1 : 2], t < .5 ? t * 2 : t * 2 - 1);
                image.setPixelRGBA(x, y, p & 0xff000000 | (color & 255) << 16 | color & 0xff00 | color >> 16 & 255);
            }
    }

    private static int brightness(int rgb) {
        return (rgb >> 16 & 255) * 3 + (rgb >> 8 & 255) * 6 + (rgb & 255);
    }

    private static int shade(int color, double factor) {
        return mix(0, color, factor);
    }

    private static int mix(int a, int b, double t) {
        int result = 0;
        for (int shift : new int[] {0, 8, 16})
            result |= (int) Math.round((a >> shift & 255) * (1 - t) + (b >> shift & 255) * t) << shift;
        return result;
    }
}
