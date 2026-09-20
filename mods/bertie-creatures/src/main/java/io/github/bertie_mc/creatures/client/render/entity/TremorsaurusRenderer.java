package io.github.bertie_mc.creatures.client.render.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.bertie_mc.creatures.BertieCreatures;
import io.github.bertie_mc.creatures.client.model.TremorsaurusModel;
import io.github.bertie_mc.creatures.client.render.entity.layer.TremorsaurusHeldMobLayer;
import io.github.bertie_mc.creatures.client.render.entity.layer.TremorsaurusRiderLayer;
import io.github.bertie_mc.creatures.server.entity.living.TremorsaurusEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

public class TremorsaurusRenderer extends MobRenderer<TremorsaurusEntity, TremorsaurusModel> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(BertieCreatures.MODID, "textures/entity/tremorsaurus.png");
    private static final ResourceLocation TEXTURE_PRINCESS =
            ResourceLocation.fromNamespaceAndPath(BertieCreatures.MODID, "textures/entity/tremorsaurus_princess.png");
    private static final ResourceLocation TEXTURE_RETRO =
            ResourceLocation.fromNamespaceAndPath(BertieCreatures.MODID, "textures/entity/tremorsaurus_retro.png");
    private static final ResourceLocation TEXTURE_TECTONIC =
            ResourceLocation.fromNamespaceAndPath(BertieCreatures.MODID, "textures/entity/tremorsaurus_tectonic.png");

    public TremorsaurusRenderer(EntityRendererProvider.Context renderManagerIn) {
        super(renderManagerIn, new TremorsaurusModel(), 1.1F);
        this.addLayer(new TremorsaurusRiderLayer(this));
        this.addLayer(new TremorsaurusHeldMobLayer(this));
    }

    protected void scale(TremorsaurusEntity mob, PoseStack matrixStackIn, float partialTicks) {}

    public ResourceLocation getTextureLocation(TremorsaurusEntity entity) {
        return entity.hasCustomName()
                        && "princess".equalsIgnoreCase(entity.getName().getString())
                ? TEXTURE_PRINCESS
                : entity.getAltSkin() == 1 ? TEXTURE_RETRO : entity.getAltSkin() == 2 ? TEXTURE_TECTONIC : TEXTURE;
    }
}
