package io.github.bertie_mc.voidfog.client;

import com.mojang.blaze3d.shaders.FogShape;
import io.github.bertie_mc.voidfog.FogCurve;
import io.github.bertie_mc.voidfog.VoidFogApi;
import io.github.bertie_mc.voidfog.VoidFogConfig;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.FogType;
import net.neoforged.neoforge.client.event.ViewportEvent;

/**
 * Draws the fog: the terrain fog planes close in and the fog colour is pulled towards black, both
 * scaled by how far below the fade depth the camera is, by how near an opening to the sky is, and by
 * anything suppressing the effect for this player.
 */
public final class VoidFogRenderer {
    /**
     * The configured dimension IDs, parsed once. Both events fire every frame, so the list is not
     * re-parsed per frame; {@link net.neoforged.neoforge.common.ModConfigSpec.ConfigValue#get()}
     * hands back the same list instance until the config reloads, which is what invalidates this.
     */
    private static List<? extends String> parsedFrom;

    private static Set<ResourceLocation> parsedDimensions = Set.of();

    private VoidFogRenderer() {}

    /**
     * Both fog modes, terrain and sky.
     *
     * <p>Sky fog used to be left alone, on the theory that reshaping it under a near-black colour
     * would band the horizon. The opposite happened: the terrain went black while the sky behind it
     * kept its own pale colour, and the join between them - a cave mouth, a gap in a ceiling - drew
     * a hard bright edge through the effect. The sky has to be pulled in with everything else or
     * the fog has a visible boundary wherever the world does not fill the view.
     */
    public static void onRenderFog(ViewportEvent.RenderFog event) {
        if (event.getType() != FogType.NONE) {
            return;
        }
        float strength = strength(event.getCamera());
        if (strength <= 0.0F) {
            return;
        }

        float start = (float) VoidFogConfig.FOG_START.getAsDouble();
        float far = FogCurve.distance(
                (float) VoidFogConfig.FOG_CLEAR.getAsDouble(), (float) VoidFogConfig.FOG_END.getAsDouble(), strength);
        event.setNearPlaneDistance(FogCurve.lerp(event.getNearPlaneDistance(), start, strength));
        // Never draw FURTHER than the game was going to; a low render distance still wins.
        event.setFarPlaneDistance(Math.min(event.getFarPlaneDistance(), far));
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

        float keep = FogCurve.colourKept(strength, (float) VoidFogConfig.DARKNESS.getAsDouble());
        event.setRed(event.getRed() * keep);
        event.setGreen(event.getGreen() * keep);
        event.setBlue(event.getBlue() * keep);
    }

    /** The strength the fog is drawn at right now, 0 to 1. Shared with the particles. */
    public static float strength(Camera camera) {
        return strength(camera.getPosition().y);
    }

    public static float strength(double y) {
        float depth = depthStrength(y);
        if (depth <= 0.0F) {
            return 0.0F;
        }

        depth *= SkyProximity.factor();
        if (depth <= 0.0F) {
            return 0.0F;
        }

        LocalPlayer player = Minecraft.getInstance().player;
        return player == null ? depth : depth * (1.0F - VoidFogApi.suppression(player));
    }

    /** Depth alone, before the sky check and any suppression. */
    public static float depthStrength(double y) {
        if (!VoidFogConfig.ENABLED.getAsBoolean()) {
            return 0.0F;
        }
        Level level = Minecraft.getInstance().level;
        if (level == null || !appliesTo(level)) {
            return 0.0F;
        }
        return FogCurve.strength(
                y, level.getMinBuildHeight(), VoidFogConfig.FADE_DEPTH.getAsInt(), VoidFogConfig.FULL_DEPTH.getAsInt());
    }

    /**
     * How much of the sky's own colour survives, for the mixin that dims the dome.
     *
     * <p>Uses the camera the world is actually drawn from, so the sky dims in step with the fog
     * rather than on its own schedule.
     */
    public static float skyKept() {
        Minecraft client = Minecraft.getInstance();
        if (client.gameRenderer == null || client.gameRenderer.getMainCamera() == null) {
            return 1.0F;
        }
        float strength = strength(client.gameRenderer.getMainCamera().getPosition().y);
        return FogCurve.colourKept(strength, (float) VoidFogConfig.DARKNESS.getAsDouble());
    }

    private static boolean appliesTo(Level level) {
        List<? extends String> configured = VoidFogConfig.DIMENSIONS.get();
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
