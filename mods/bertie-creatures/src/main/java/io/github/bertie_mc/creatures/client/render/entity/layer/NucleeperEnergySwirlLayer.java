package io.github.bertie_mc.creatures.client.render.entity.layer;

import io.github.bertie_mc.creatures.BertieCreatures;
import io.github.bertie_mc.creatures.client.model.NucleeperModel;
import io.github.bertie_mc.creatures.server.entity.living.NucleeperEntity;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.EnergySwirlLayer;
import net.minecraft.resources.ResourceLocation;

public class NucleeperEnergySwirlLayer extends EnergySwirlLayer<NucleeperEntity, NucleeperModel> {
    private static final ResourceLocation POWER_LOCATION = ResourceLocation.fromNamespaceAndPath(
            BertieCreatures.MODID, "textures/entity/nucleeper/nucleeper_charged.png");
    private final NucleeperModel model = new NucleeperModel(1.0F);

    public NucleeperEnergySwirlLayer(RenderLayerParent<NucleeperEntity, NucleeperModel> renderer) {
        super(renderer);
    }

    protected float xOffset(float f) {
        return f * 0.01F;
    }

    protected ResourceLocation getTextureLocation() {
        return POWER_LOCATION;
    }

    protected EntityModel<NucleeperEntity> model() {
        return this.model;
    }
}
