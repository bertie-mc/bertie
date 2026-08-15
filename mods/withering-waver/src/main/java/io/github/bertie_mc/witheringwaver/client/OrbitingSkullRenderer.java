package io.github.bertie_mc.witheringwaver.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.bertie_mc.witheringwaver.entity.OrbitingSkullEntity;
import net.minecraft.client.model.SkullModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/** Renders a reaped skull with the vanilla wither skeleton skull model, full-bright. */
@OnlyIn(Dist.CLIENT)
public class OrbitingSkullRenderer extends EntityRenderer<OrbitingSkullEntity> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.withDefaultNamespace("textures/entity/skeleton/wither_skeleton.png");

    private final SkullModel model;

    public OrbitingSkullRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.model = new SkullModel(context.bakeLayer(ModelLayers.WITHER_SKELETON_SKULL));
    }

    @Override
    protected int getBlockLightLevel(OrbitingSkullEntity entity, BlockPos pos) {
        return 15;
    }

    @Override
    public void render(
            OrbitingSkullEntity entity,
            float entityYaw,
            float partialTicks,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight) {
        poseStack.pushPose();
        poseStack.scale(-1.0F, -1.0F, 1.0F);
        float yRot = Mth.rotLerp(partialTicks, entity.yRotO, entity.getYRot());
        float xRot = Mth.lerp(partialTicks, entity.xRotO, entity.getXRot());
        VertexConsumer consumer = buffer.getBuffer(this.model.renderType(TEXTURE));
        this.model.setupAnim(0.0F, yRot, xRot);
        this.model.renderToBuffer(poseStack, consumer, packedLight, OverlayTexture.NO_OVERLAY);
        poseStack.popPose();
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(OrbitingSkullEntity entity) {
        return TEXTURE;
    }
}
