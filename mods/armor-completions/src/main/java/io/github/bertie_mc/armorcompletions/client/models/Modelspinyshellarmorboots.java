package io.github.bertie_mc.armorcompletions.client.models;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import java.util.List;
import java.util.Map;

/** New companion asset for Born in Chaos 1.7.5 / Minecraft 1.21.1. */
public class Modelspinyshellarmorboots<T extends Entity> extends EntityModel<T> {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath("born_in_chaos_v1", "modelspinyshellarmorboots"), "main");
    public final ModelPart RightLeg;
    public final ModelPart LeftLeg;

    public Modelspinyshellarmorboots(ModelPart root) {
        RightLeg = root.getChild("RightLeg");
        LeftLeg = root.getChild("LeftLeg");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition part0 = root.addOrReplaceChild("RightLeg",
                CubeListBuilder.create().texOffs(0, 0).addBox(-2.0F, 8.0F, -2.0F, 4.0F, 4.0F, 4.0F, new CubeDeformation(0.35F))
                .texOffs(18, 0).addBox(-2.45F, 7.9F, -2.45F, 5.0F, 1.0F, 5.0F, new CubeDeformation(0.0F))
                .texOffs(40, 0).addBox(-2.35F, 10.2F, -3.2F, 5.0F, 2.0F, 5.0F, new CubeDeformation(0.0F))
                .texOffs(62, 0).addBox(-2.35F, 11.8F, -3.2F, 5.0F, 1.0F, 5.0F, new CubeDeformation(0.0F))
                .texOffs(84, 0).addBox(-2.1F, 8.9F, -2.65F, 4.0F, 2.0F, 1.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(-1.9F, 12.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        PartDefinition part1 = part0.addOrReplaceChild("ankle_spike",
                CubeListBuilder.create().texOffs(96, 0).addBox(-1.5F, -3.0F, 0.0F, 3.0F, 4.0F, 0.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(-2.6F, 9.8F, 0.3F, 0.0F, 0.0F, -0.65F));
        PartDefinition part2 = root.addOrReplaceChild("LeftLeg",
                CubeListBuilder.create().texOffs(0, 0).addBox(-2.0F, 8.0F, -2.0F, 4.0F, 4.0F, 4.0F, new CubeDeformation(0.35F))
                .texOffs(18, 0).addBox(-2.45F, 7.9F, -2.45F, 5.0F, 1.0F, 5.0F, new CubeDeformation(0.0F))
                .texOffs(40, 0).addBox(-2.35F, 10.2F, -3.2F, 5.0F, 2.0F, 5.0F, new CubeDeformation(0.0F))
                .texOffs(62, 0).addBox(-2.35F, 11.8F, -3.2F, 5.0F, 1.0F, 5.0F, new CubeDeformation(0.0F))
                .texOffs(84, 0).addBox(-2.1F, 8.9F, -2.65F, 4.0F, 2.0F, 1.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(1.9F, 12.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        PartDefinition part3 = part2.addOrReplaceChild("ankle_spike",
                CubeListBuilder.create().texOffs(96, 0).addBox(-1.5F, -3.0F, 0.0F, 3.0F, 4.0F, 0.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(2.6F, 9.8F, 0.3F, 0.0F, 0.0F, 0.65F));
        return LayerDefinition.create(mesh, 128, 128);
    }

    /** Wraps the custom parts exactly as the original armor's item extension does. */
    public static HumanoidModel<LivingEntity> asHumanoidModel(ModelPart bakedRoot) {
        return new HumanoidModel<>(new ModelPart(List.of(), Map.of(
                "head", emptyPart(), "hat", emptyPart(),
                "body", emptyPart(),
                "right_arm", emptyPart(), "left_arm", emptyPart(),
                "right_leg", bakedRoot.getChild("RightLeg"),
                "left_leg", bakedRoot.getChild("LeftLeg"))));
    }

    private static ModelPart emptyPart() {
        return new ModelPart(List.of(), Map.of());
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer vertices, int light, int overlay, int color) {
        RightLeg.render(poseStack, vertices, light, overlay, color);
        LeftLeg.render(poseStack, vertices, light, overlay, color);
    }

    @Override
    public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {}
}
