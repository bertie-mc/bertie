package io.github.bertie_mc.witheringwaver.client;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.bertie_mc.witheringwaver.WitheringWaver;
import io.github.bertie_mc.witheringwaver.entity.WitheringWaverEntity;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class WitheringWaverRenderer extends MobRenderer<WitheringWaverEntity, WitheringWaverModel> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(WitheringWaver.id("withering_waver"), "main");
    private static final ResourceLocation TEXTURE = WitheringWaver.id("textures/entity/withering_waver.png");

    public WitheringWaverRenderer(EntityRendererProvider.Context context) {
        super(context, new WitheringWaverModel(context.bakeLayer(LAYER)), 0.6F);
    }

    @Override
    public ResourceLocation getTextureLocation(WitheringWaverEntity entity) {
        return TEXTURE;
    }

    @Override
    protected void scale(WitheringWaverEntity entity, PoseStack poseStack, float partialTicks) {
        poseStack.scale(1.05F, 1.05F, 1.05F);
    }
}
