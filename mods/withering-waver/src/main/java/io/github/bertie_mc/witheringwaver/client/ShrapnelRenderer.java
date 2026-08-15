package io.github.bertie_mc.witheringwaver.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import io.github.bertie_mc.witheringwaver.WitheringWaver;
import io.github.bertie_mc.witheringwaver.entity.SkullShrapnelEntity;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ShrapnelRenderer extends EntityRenderer<SkullShrapnelEntity> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(WitheringWaver.id("skull_shrapnel"), "main");
    private static final ResourceLocation TEXTURE = WitheringWaver.id("textures/entity/withering_waver_shrapnel.png");

    private final ShrapnelModel model;

    public ShrapnelRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.model = new ShrapnelModel(context.bakeLayer(LAYER));
    }

    @Override
    public void render(
            SkullShrapnelEntity entity,
            float entityYaw,
            float partialTicks,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight) {
        poseStack.pushPose();
        // Arrow-style orientation: yaw, pitch, then a roll spin around the flight axis.
        poseStack.mulPose(Axis.YP.rotationDegrees(Mth.rotLerp(partialTicks, entity.yRotO, entity.getYRot()) - 90.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(Mth.lerp(partialTicks, entity.xRotO, entity.getXRot())));
        poseStack.mulPose(Axis.XP.rotation(entity.getSpin(partialTicks)));
        VertexConsumer consumer = buffer.getBuffer(this.model.renderType(TEXTURE));
        this.model.renderToBuffer(poseStack, consumer, packedLight, OverlayTexture.NO_OVERLAY);
        poseStack.popPose();
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(SkullShrapnelEntity entity) {
        return TEXTURE;
    }
}
