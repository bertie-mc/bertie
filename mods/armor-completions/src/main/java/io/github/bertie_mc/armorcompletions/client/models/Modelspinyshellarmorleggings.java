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
public class Modelspinyshellarmorleggings<T extends Entity> extends EntityModel<T> {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath("born_in_chaos_v1", "modelspinyshellarmorleggings"), "main");
    public final ModelPart Body;
    public final ModelPart RightLeg;
    public final ModelPart LeftLeg;

    public Modelspinyshellarmorleggings(ModelPart root) {
        Body = root.getChild("Body");
        RightLeg = root.getChild("RightLeg");
        LeftLeg = root.getChild("LeftLeg");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition part0 = root.addOrReplaceChild("Body",
                CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, 10.0F, -2.0F, 8.0F, 2.0F, 4.0F, new CubeDeformation(0.35F)),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        PartDefinition part1 = root.addOrReplaceChild("RightLeg",
                CubeListBuilder.create().texOffs(0, 12).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(0.12F))
                .texOffs(20, 12).addBox(-2.85F, 0.0F, -2.3F, 1.0F, 5.0F, 5.0F, new CubeDeformation(0.0F))
                .texOffs(34, 12).addBox(-2.1F, 0.5F, -2.65F, 4.0F, 4.0F, 1.0F, new CubeDeformation(0.0F))
                .texOffs(46, 12).addBox(-2.1F, 5.1F, -2.85F, 4.0F, 3.0F, 1.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(-1.9F, 12.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        PartDefinition part2 = part1.addOrReplaceChild("thigh_spike",
                CubeListBuilder.create().texOffs(60, 12).addBox(-1.5F, -3.5F, 0.0F, 3.0F, 4.0F, 0.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(-2.6F, 4.5F, 0.0F, 0.0F, 0.0F, -0.48F));
        PartDefinition part3 = root.addOrReplaceChild("LeftLeg",
                CubeListBuilder.create().texOffs(0, 12).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(0.12F))
                .texOffs(20, 12).addBox(1.65F, 0.0F, -2.3F, 1.0F, 5.0F, 5.0F, new CubeDeformation(0.0F))
                .texOffs(34, 12).addBox(-2.1F, 0.5F, -2.65F, 4.0F, 4.0F, 1.0F, new CubeDeformation(0.0F))
                .texOffs(46, 12).addBox(-2.1F, 5.1F, -2.85F, 4.0F, 3.0F, 1.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(1.9F, 12.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        PartDefinition part4 = part3.addOrReplaceChild("thigh_spike",
                CubeListBuilder.create().texOffs(60, 12).addBox(-1.5F, -3.5F, 0.0F, 3.0F, 4.0F, 0.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(2.6F, 4.5F, 0.0F, 0.0F, 0.0F, 0.48F));
        return LayerDefinition.create(mesh, 128, 128);
    }

    /** Wraps the custom parts exactly as the original armor's item extension does. */
    public static HumanoidModel<LivingEntity> asHumanoidModel(ModelPart bakedRoot) {
        return new HumanoidModel<>(new ModelPart(List.of(), Map.of(
                "head", emptyPart(), "hat", emptyPart(),
                "body", bakedRoot.getChild("Body"),
                "right_arm", emptyPart(), "left_arm", emptyPart(),
                "right_leg", bakedRoot.getChild("RightLeg"),
                "left_leg", bakedRoot.getChild("LeftLeg"))));
    }

    private static ModelPart emptyPart() {
        return new ModelPart(List.of(), Map.of());
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer vertices, int light, int overlay, int color) {
        Body.render(poseStack, vertices, light, overlay, color);
        RightLeg.render(poseStack, vertices, light, overlay, color);
        LeftLeg.render(poseStack, vertices, light, overlay, color);
    }

    @Override
    public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {}
}
