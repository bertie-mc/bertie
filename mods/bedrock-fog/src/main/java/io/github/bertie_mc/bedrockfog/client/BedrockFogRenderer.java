package io.github.bertie_mc.bedrockfog.client;

import com.mojang.blaze3d.shaders.FogShape;
import io.github.bertie_mc.bedrockfog.BedrockFogConfig;
import io.github.bertie_mc.bedrockfog.FogCurve;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.FogType;
import net.neoforged.neoforge.client.event.ViewportEvent;

/**
 * Draws the fog: the terrain fog planes close in and the fog colour is pulled towards black, both
 * scaled by how far below the fade depth the camera is.
 */
public final class BedrockFogRenderer {
    /**
     * The configured dimension IDs, parsed once. Both events fire every frame, so the list is not
     * re-parsed per frame; {@link net.neoforged.neoforge.common.ModConfigSpec.ConfigValue#get()}
     * hands back the same list instance until the config reloads, which is what invalidates this.
     */
    private static List<? extends String> parsedFrom;

    private static Set<ResourceLocation> parsedDimensions = Set.of();

    private BedrockFogRenderer() {}

    /**
     * Only terrain fog is touched. Sky fog is left alone because reshaping it while the fog colour
     * is already near-black bands the horizon on the rare deep spot that can still see sky.
     */
    public static void onRenderFog(ViewportEvent.RenderFog event) {
        if (event.getMode() != FogRenderer.FogMode.FOG_TERRAIN || event.getType() != FogType.NONE) {
            return;
        }
        float strength = strength(event.getCamera());
        if (strength <= 0.0F) {
            return;
        }

        float start = (float) BedrockFogConfig.FOG_START.getAsDouble();
        float end = (float) BedrockFogConfig.FOG_END.getAsDouble();
        event.setNearPlaneDistance(FogCurve.lerp(event.getNearPlaneDistance(), start, strength));
        event.setFarPlaneDistance(FogCurve.lerp(event.getFarPlaneDistance(), end, strength));
        event.setFogShape(FogShape.SPHERE);
        // The event carries the new distances only when it is cancelled.
        event.setCanceled(true);
    }

    /** Head underwater or in lava keeps that fluid's own colour - the fog there is not ours. */
    public static void onFogColor(ViewportEvent.ComputeFogColor event) {
        if (event.getCamera().getFluidInCamera() != FogType.NONE) {
            return;
        }
        float strength = strength(event.getCamera());
        if (strength <= 0.0F) {
            return;
        }

        float keep = 1.0F - strength * (float) BedrockFogConfig.DARKNESS.getAsDouble();
        event.setRed(event.getRed() * keep);
        event.setGreen(event.getGreen() * keep);
        event.setBlue(event.getBlue() * keep);
    }

    private static float strength(Camera camera) {
        if (!BedrockFogConfig.ENABLED.getAsBoolean()) {
            return 0.0F;
        }
        Level level = Minecraft.getInstance().level;
        if (level == null || !appliesTo(level)) {
            return 0.0F;
        }
        return FogCurve.strength(
                camera.getPosition().y,
                level.getMinBuildHeight(),
                BedrockFogConfig.FADE_DEPTH.getAsInt(),
                BedrockFogConfig.FULL_DEPTH.getAsInt());
    }

    private static boolean appliesTo(Level level) {
        List<? extends String> configured = BedrockFogConfig.DIMENSIONS.get();
        if (configured != parsedFrom) {
            Set<ResourceLocation> parsed = new HashSet<>();
            for (String id : configured) {
                ResourceLocation dimension = ResourceLocation.tryParse(id);
                if (dimension != null) {
                    parsed.add(dimension);
                }
            }
            parsedFrom = configured;
            parsedDimensions = parsed;
        }
        return parsedDimensions.contains(level.dimension().location());
    }
}
