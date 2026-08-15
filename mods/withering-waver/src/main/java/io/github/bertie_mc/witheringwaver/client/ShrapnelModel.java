package io.github.bertie_mc.witheringwaver.client;

import io.github.bertie_mc.witheringwaver.entity.SkullShrapnelEntity;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/** An elongated sharp shard, long axis on +X so the renderer can aim it along flight. */
@OnlyIn(Dist.CLIENT)
public class ShrapnelModel extends HierarchicalModel<SkullShrapnelEntity> {
    private final ModelPart root;

    public ShrapnelModel(ModelPart root) {
        this.root = root;
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        mesh.getRoot()
                .addOrReplaceChild(
                        "shard",
                        CubeListBuilder.create()
                                .texOffs(0, 0)
                                .addBox(-3.0F, -1.0F, -1.0F, 6.0F, 2.0F, 2.0F)
                                .texOffs(0, 8)
                                .addBox(3.0F, -0.5F, -0.5F, 3.0F, 1.0F, 1.0F)
                                .texOffs(0, 12)
                                .addBox(-4.5F, -0.5F, -0.5F, 1.5F, 1.0F, 1.0F),
                        PartPose.ZERO);
        return LayerDefinition.create(mesh, 32, 32);
    }

    @Override
    public ModelPart root() {
        return this.root;
    }

    @Override
    public void setupAnim(
            SkullShrapnelEntity entity,
            float limbSwing,
            float limbSwingAmount,
            float ageInTicks,
            float netHeadYaw,
            float headPitch) {}
}
