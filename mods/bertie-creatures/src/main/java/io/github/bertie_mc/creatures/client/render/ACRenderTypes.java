package io.github.bertie_mc.creatures.client.render;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

public class ACRenderTypes extends RenderType {
    public static net.minecraft.client.renderer.ShaderInstance bubbledShader;
    private static final ShaderStateShard BUBBLED_SHADER = new ShaderStateShard(() -> bubbledShader);

    public ACRenderTypes(
            String name,
            com.mojang.blaze3d.vertex.VertexFormat format,
            com.mojang.blaze3d.vertex.VertexFormat.Mode mode,
            int size,
            boolean crumbling,
            boolean sort,
            Runnable setup,
            Runnable clear) {
        super(name, format, mode, size, crumbling, sort, setup, clear);
    }

    protected static final RenderStateShard.TransparencyStateShard EYES_ALPHA_TRANSPARENCY =
            new RenderStateShard.TransparencyStateShard(
                    "eyes_alpha_transparency",
                    () -> {
                        RenderSystem.enableBlend();
                        RenderSystem.blendFuncSeparate(
                                GlStateManager.SourceFactor.SRC_ALPHA,
                                GlStateManager.DestFactor.ONE,
                                GlStateManager.SourceFactor.ONE,
                                GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
                    },
                    () -> {
                        RenderSystem.disableBlend();
                        RenderSystem.defaultBlendFunc();
                    });

    public static RenderType getEyesAlphaEnabled(ResourceLocation locationIn) {
        RenderType.CompositeState rendertype$compositestate = RenderType.CompositeState.builder()
                .setShaderState(RENDERTYPE_EYES_SHADER)
                .setTextureState(new RenderStateShard.TextureStateShard(locationIn, false, false))
                .setTransparencyState(EYES_ALPHA_TRANSPARENCY)
                .setCullState(NO_CULL)
                .setLightmapState(LIGHTMAP)
                .setOverlayState(OVERLAY)
                .setDepthTestState(EQUAL_DEPTH_TEST)
                .createCompositeState(true);
        return create(
                "eye_alpha",
                DefaultVertexFormat.NEW_ENTITY,
                VertexFormat.Mode.QUADS,
                256,
                true,
                false,
                rendertype$compositestate);
    }

    public static RenderType getNucleeperLights() {
        return create(
                "nucleeper_lights",
                DefaultVertexFormat.POSITION_COLOR,
                VertexFormat.Mode.QUADS,
                256,
                true,
                true,
                RenderType.CompositeState.builder()
                        .setShaderState(RenderType.RENDERTYPE_LIGHTNING_SHADER)
                        .setTransparencyState(EYES_ALPHA_TRANSPARENCY)
                        .setCullState(CULL)
                        .setLightmapState(NO_LIGHTMAP)
                        .setOutputState(RenderStateShard.ITEM_ENTITY_TARGET)
                        .createCompositeState(true));
    }

    public static RenderType getGhostly(ResourceLocation texture) {
        CompositeState renderState = CompositeState.builder()
                .setShaderState(RENDERTYPE_ENTITY_TRANSLUCENT_EMISSIVE_SHADER)
                .setCullState(NO_CULL)
                .setTextureState(new TextureStateShard(texture, false, false))
                .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                .setLightmapState(LIGHTMAP)
                .setOverlayState(OVERLAY)
                .setWriteMaskState(COLOR_DEPTH_WRITE)
                .setDepthTestState(LEQUAL_DEPTH_TEST)
                .createCompositeState(true);
        return create("ghostly", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 256, true, true, renderState);
    }

    public static RenderType getTeslaBulb(ResourceLocation resourceLocation) {
        return create(
                "tesla_bulb",
                DefaultVertexFormat.NEW_ENTITY,
                VertexFormat.Mode.QUADS,
                256,
                false,
                true,
                RenderType.CompositeState.builder()
                        .setShaderState(RenderStateShard.RENDERTYPE_ENERGY_SWIRL_SHADER)
                        .setTextureState(new RenderStateShard.TextureStateShard(resourceLocation, false, true))
                        .setLightmapState(LIGHTMAP)
                        .setCullState(RenderStateShard.NO_CULL)
                        .setTransparencyState(RenderStateShard.TRANSLUCENT_TRANSPARENCY)
                        .setDepthTestState(LEQUAL_DEPTH_TEST)
                        .createCompositeState(true));
    }

    public static RenderType getBubbledNoCull(ResourceLocation locationIn) {
        RenderType.CompositeState rendertype$compositestate = RenderType.CompositeState.builder()
                .setShaderState(BUBBLED_SHADER)
                .setTextureState(new RenderStateShard.TextureStateShard(locationIn, false, false))
                .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                .setCullState(NO_CULL)
                .setLightmapState(LIGHTMAP)
                .setOutputState(RenderStateShard.ITEM_ENTITY_TARGET)
                .setOverlayState(OVERLAY)
                .setWriteMaskState(RenderStateShard.COLOR_DEPTH_WRITE)
                .createCompositeState(true);
        return create(
                "bubbled_no_cull",
                DefaultVertexFormat.NEW_ENTITY,
                VertexFormat.Mode.QUADS,
                256,
                false,
                false,
                rendertype$compositestate);
    }

    public static RenderType getBubbledCull(ResourceLocation texture) {
        return create(
                "bubbled_cull",
                DefaultVertexFormat.NEW_ENTITY,
                VertexFormat.Mode.QUADS,
                256,
                true,
                true,
                CompositeState.builder()
                        .setShaderState(BUBBLED_SHADER)
                        .setTextureState(new TextureStateShard(texture, false, false))
                        .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                        .setLightmapState(LIGHTMAP)
                        .setOverlayState(OVERLAY)
                        .setOutputState(ITEM_ENTITY_TARGET)
                        .createCompositeState(true));
    }

    public static RenderType getBookWidget(ResourceLocation texture, boolean sepia) {
        return entityCutoutNoCull(texture);
    }
}
