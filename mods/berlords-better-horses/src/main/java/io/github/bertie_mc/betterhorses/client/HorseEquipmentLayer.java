package io.github.bertie_mc.betterhorses.client;

import com.mojang.blaze3d.vertex.*;
import io.github.bertie_mc.betterhorses.*;
import java.util.Locale;
import net.minecraft.client.model.HorseModel;
import net.minecraft.client.model.geom.*;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.animal.horse.Horse;
import net.minecraft.world.item.ItemStack;

public final class HorseEquipmentLayer extends RenderLayer<Horse, HorseModel<Horse>> {
    private final HorseModel<Horse> shoes =
            new HorseModel<>(LayerDefinition.create(HorseModel.createBodyMesh(new CubeDeformation(0.006F)), 64, 64)
                    .bakeRoot());
    private final SaddleModel passenger = new SaddleModel("passenger");
    private final SaddleModel warrior = new SaddleModel("warrior");
    private final SaddleModel wanderer = new SaddleModel("wanderer");

    public HorseEquipmentLayer(RenderLayerParent<Horse, HorseModel<Horse>> parent) {
        super(parent);
    }

    @Override
    public void render(
            PoseStack pose,
            MultiBufferSource buffers,
            int light,
            Horse horse,
            float swing,
            float amount,
            float partial,
            float age,
            float yaw,
            float pitch) {
        HorseEquipment equipment = (HorseEquipment) horse;
        if (equipment.betterhorses$tier() != ShoeTier.NONE) {
            animate(shoes, horse, swing, amount, partial, age, yaw, pitch);
            shoes.renderToBuffer(
                    pose,
                    buffers.getBuffer(RenderType.entityCutoutNoCull(BetterHorses.id("textures/entity/horseshoes_"
                            + equipment.betterhorses$tier().name().toLowerCase(Locale.ROOT) + ".png"))),
                    light,
                    OverlayTexture.NO_OVERLAY);
        }
        ItemStack saddle = equipment.betterhorses$syncedSaddle();
        SaddleModel model = saddle.is(BetterHorses.PASSENGER.get())
                ? passenger
                : saddle.is(BetterHorses.WARRIOR.get())
                        ? warrior
                        : saddle.is(BetterHorses.WANDERER.get()) ? wanderer : null;
        if (model != null && !horse.isBaby()) {
            animate(model, horse, swing, amount, partial, age, yaw, pitch);
            model.renderSaddle(
                    pose,
                    buffers.getBuffer(RenderType.entityCutoutNoCull(
                            BetterHorses.id("textures/entity/" + model.kind + "_saddle.png"))),
                    light);
        }
    }

    private void animate(
            HorseModel<Horse> model,
            Horse horse,
            float swing,
            float amount,
            float partial,
            float age,
            float yaw,
            float pitch) {
        getParentModel().copyPropertiesTo(model);
        model.prepareMobModel(horse, swing, amount, partial);
        model.setupAnim(horse, swing, amount, age, yaw, pitch);
    }

    private static final class SaddleModel extends HorseModel<Horse> {
        private final String kind;

        SaddleModel(String kind) {
            super(mesh(kind));
            this.kind = kind;
        }

        private static ModelPart mesh(String kind) {
            MeshDefinition mesh = HorseModel.createBodyMesh(CubeDeformation.NONE);
            PartDefinition body = mesh.getRoot().getChild("body");
            CubeListBuilder cubes =
                    CubeListBuilder.create().texOffs(0, 0).addBox(-5, -8, -9, 10, 9, 9, new CubeDeformation(0.56F));
            if (kind.equals("passenger")) cubes.texOffs(0, 0).addBox(-5, -8, 0, 10, 3, 7, new CubeDeformation(0.56F));
            if (kind.equals("warrior"))
                cubes.texOffs(0, 32)
                        .addBox(-5, -10, -9, 10, 2, 2)
                        .texOffs(0, 32)
                        .addBox(-5, -10, -1, 10, 2, 2);
            if (kind.equals("wanderer"))
                cubes.texOffs(0, 32)
                        .addBox(-8, -6, -3, 3, 6, 7)
                        .texOffs(0, 32)
                        .addBox(5, -6, -3, 3, 6, 7)
                        .texOffs(32, 32)
                        .addBox(-5, -10, 2, 10, 3, 3);
            body.addOrReplaceChild("saddle", cubes, PartPose.ZERO);
            return LayerDefinition.create(mesh, 64, 64).bakeRoot();
        }

        void renderSaddle(PoseStack pose, VertexConsumer vertices, int light) {
            pose.pushPose();
            body.translateAndRotate(pose);
            body.getChild("saddle").render(pose, vertices, light, OverlayTexture.NO_OVERLAY);
            pose.popPose();
        }
    }
}
