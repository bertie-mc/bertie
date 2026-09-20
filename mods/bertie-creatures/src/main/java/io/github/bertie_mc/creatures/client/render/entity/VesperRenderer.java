package io.github.bertie_mc.creatures.client.render.entity;

import io.github.bertie_mc.creatures.BertieCreatures;
import io.github.bertie_mc.creatures.client.model.VesperModel;
import io.github.bertie_mc.creatures.server.entity.living.VesperEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

public class VesperRenderer extends MobRenderer<VesperEntity, VesperModel> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(BertieCreatures.MODID, "textures/entity/vesper.png");

    public VesperRenderer(EntityRendererProvider.Context renderManagerIn) {
        super(renderManagerIn, new VesperModel(), 0.35F);
    }

    public ResourceLocation getTextureLocation(VesperEntity entity) {
        return TEXTURE;
    }
}
