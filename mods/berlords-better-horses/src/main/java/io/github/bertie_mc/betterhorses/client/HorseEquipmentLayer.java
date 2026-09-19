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
    private final HorseSaddleModel passenger = new HorseSaddleModel("passenger");
    private final HorseSaddleModel warrior = new HorseSaddleModel("warrior");
    private final HorseSaddleModel wanderer = new HorseSaddleModel("wanderer");

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
        HorseSaddleModel model = saddle.is(BetterHorses.PASSENGER.get())
                ? passenger
                : saddle.is(BetterHorses.WARRIOR.get())
                        ? warrior
                        : saddle.is(BetterHorses.WANDERER.get()) ? wanderer : null;
        if (model != null && !horse.isBaby()) {
            animate(model, horse, swing, amount, partial, age, yaw, pitch);
            model.renderSaddle(
                    pose,
                    buffers.getBuffer(
                            RenderType.entityCutoutNoCull(BetterHorses.id("textures/entity/saddle_materials.png"))),
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
}
