package io.github.bertie_mc.spellrestrictions.client;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.renderer.texture.atlas.SpriteSource;
import net.minecraft.client.renderer.texture.atlas.SpriteSourceType;
import net.minecraft.client.resources.metadata.animation.FrameSize;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceMetadata;
import org.slf4j.Logger;

public record OrbSpriteSource(ResourceLocation sprite, int rarity, boolean orbit) implements SpriteSource {
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final MapCodec<OrbSpriteSource> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                    ResourceLocation.CODEC.fieldOf("sprite").forGetter(OrbSpriteSource::sprite),
                    Codec.intRange(0, 4).fieldOf("rarity").forGetter(OrbSpriteSource::rarity),
                    Codec.BOOL.fieldOf("orbit").forGetter(OrbSpriteSource::orbit))
            .apply(instance, OrbSpriteSource::new));
    public static final SpriteSourceType TYPE = new SpriteSourceType(CODEC);

    @Override
    public SpriteSourceType type() {
        return TYPE;
    }

    @Override
    public void run(ResourceManager manager, Output output) {
        var sourceId = ResourceLocation.fromNamespaceAndPath(
                "irons_spellbooks", "textures/item/upgrade_orb_" + (orbit ? "swirl" : "base") + ".png");
        var resource = manager.getResource(sourceId);
        if (resource.isEmpty()) {
            LOGGER.error("Missing required orb source {}", sourceId);
            return;
        }
        output.add(sprite, loader -> {
            try (var stream = resource.get().open();
                    var source = NativeImage.read(stream)) {
                int size = source.getWidth();
                int frames = orbit ? source.getHeight() / size : 24;
                var image = new NativeImage(size, size * frames, false);
                for (int frame = 0; frame < frames; frame++) {
                    for (int y = 0; y < size; y++)
                        for (int x = 0; x < size; x++) {
                            int pixel = source.getPixelRGBA(x, orbit ? y + frame * size : y);
                            image.setPixelRGBA(
                                    x,
                                    y + frame * size,
                                    orbit
                                            ? OrbPalette.orbit(pixel, rarity)
                                            : OrbPalette.pulse(pixel, rarity, frame / 24.0));
                        }
                }
                var metadata = orbit
                        ? resource.get().metadata()
                        : ResourceMetadata.fromJsonStream(
                                new ByteArrayInputStream("{\"animation\":{\"frametime\":2,\"interpolate\":true}}"
                                        .getBytes(StandardCharsets.UTF_8)));
                return new SpriteContents(sprite, new FrameSize(size, size), image, metadata);
            } catch (Exception error) {
                LOGGER.error("Failed to assemble orb sprite {}", sprite, error);
                return null;
            }
        });
    }
}
