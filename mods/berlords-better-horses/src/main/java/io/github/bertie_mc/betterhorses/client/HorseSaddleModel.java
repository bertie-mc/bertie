package io.github.bertie_mc.betterhorses.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.HorseModel;
import net.minecraft.client.model.geom.*;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.animal.horse.Horse;

public final class HorseSaddleModel extends HorseModel<Horse> {
    private static final String ROOT = "betterhorses_saddle";

    public HorseSaddleModel(String kind) {
        super(mesh(kind));
    }

    private static ModelPart mesh(String kind) {
        MeshDefinition mesh = HorseModel.createBodyMesh(CubeDeformation.NONE);
        PartDefinition saddle =
                mesh.getRoot().getChild("body").addOrReplaceChild(ROOT, CubeListBuilder.create(), PartPose.ZERO);
        boolean passenger = kind.equals("passenger"), warrior = kind.equals("warrior");
        int clothU = warrior ? 64 : 0, clothV = kind.equals("wanderer") ? 64 : 32;
        float front = passenger ? -15.0F : -13, length = passenger ? 20.5F : 16;

        // One continuous blanket has no overlapping front/rear top faces.
        box(saddle, "blanket", clothU, clothV, -5.6F, -8.8F, front, 11.2F, 0.8F, length);
        for (int side : new int[] {-1, 1}) {
            float x = side < 0 ? -5.8F : 5.15F;
            // Mirror the right-hand (-X) face onto the left so the cloth motif stays toward the rear.
            boolean mirror = warrior && side > 0;
            box(saddle, "cloth_flap_" + side, clothU + 8, clothV + 1, x, -8.0F, front, 0.65F, 5.5F, length, mirror);
            box(
                    saddle,
                    "binding_" + side,
                    64,
                    96,
                    side < 0 ? -5.98F : 5.8F,
                    -3.15F,
                    front,
                    0.18F,
                    0.65F,
                    length,
                    mirror);
        }

        if (passenger) {
            seat(saddle, "front", -14.6F, 9, false);
            seat(saddle, "rear", -3.6F, 9, false);
            stirrups(saddle, "front", -10, false);
            stirrups(saddle, "rear", 1, false);
        } else if (warrior) {
            seat(saddle, "warrior", -11, 11, true);
            stirrups(saddle, "warrior", -6, true);
            // Steel-edged pommel and high cantle frame the dark leather seat.
            box(saddle, "pommel_steel", 0, 96, -3.5F, -12.0F, -11.2F, 7, 0.7F, 1.4F);
            box(saddle, "cantle_steel", 0, 96, -4.5F, -12.3F, -0.1F, 9, 0.7F, 1.4F);
        } else {
            seat(saddle, "traveller", -11.6F, 9, false);
            stirrups(saddle, "traveller", -7, false);
            for (int side : new int[] {-1, 1}) {
                float x = side < 0 ? -9.4F : 5.9F;
                box(saddle, "bag_" + side, 64, 1, x, -5.8F, -1, 3.5F, 6, 6);
                box(saddle, "bag_lid_" + side, 2, 1, x - 0.15F, -6.5F, -1.15F, 3.8F, 1.5F, 6.3F);
                box(saddle, "bag_strap_" + side, 4, 2, side < 0 ? -9.62F : 9.4F, -5.8F, 1.3F, 0.22F, 5, 1.2F);
                box(saddle, "bag_clasp_" + side, 70, 98, side < 0 ? -9.8F : 9.62F, -3.6F, 1.0F, 0.18F, 1.4F, 1.8F);
            }
            box(saddle, "bedroll", 64, 64, -5, -11.6F, 1.2F, 10, 3.3F, 4);
            box(saddle, "bedroll_top", 66, 65, -4.2F, -12.3F, 1.7F, 8.4F, 0.7F, 3);
            for (int side : new int[] {-1, 1})
                box(saddle, "bedroll_tie_" + side, 2, 2, side < 0 ? -3.4F : 2.4F, -12.4F, 1.0F, 1, 4.2F, 4.4F);
        }
        return LayerDefinition.create(mesh, 128, 128).bakeRoot();
    }

    private static void seat(PartDefinition root, String name, float z, float depth, boolean warrior) {
        box(root, name + "_seat_base", 64, 0, -4.9F, -9.6F, z, 9.8F, 0.8F, depth);
        box(root, name + "_seat", 1, 1, -4, -10.25F, z + 0.7F, 8, 0.65F, depth - 1.4F);
        box(root, name + "_pommel", 65, 0, -3.5F, warrior ? -11.4F : -11.1F, z - 0.15F, 7, warrior ? 1.8F : 1.5F, 1.2F);
        box(
                root,
                name + "_cantle",
                65,
                0,
                -4.5F,
                warrior ? -11.7F : -11.3F,
                z + depth - 0.1F,
                9,
                warrior ? 2.1F : 1.7F,
                1.2F);
    }

    private static void stirrups(PartDefinition root, String name, float z, boolean steel) {
        for (int side : new int[] {-1, 1}) {
            String id = name + "_" + side;
            boolean mirror = steel && side > 0;
            box(root, "stirrup_strap_" + id, 3, 1, side < 0 ? -6.15F : 5.8F, -8.2F, z - 0.5F, 0.35F, 8.8F, 1, mirror);
            float x = side < 0 ? -6.45F : 5.8F;
            int u = steel ? 0 : 64;
            box(root, "stirrup_front_" + id, u + 2, 97, x, 0.5F, z - 1.8F, 0.65F, 3, 0.6F, mirror);
            box(root, "stirrup_back_" + id, u + 2, 97, x, 0.5F, z + 1.2F, 0.65F, 3, 0.6F, mirror);
            box(root, "stirrup_top_" + id, u + 2, 97, x, 0.1F, z - 1.8F, 0.65F, 0.4F, 3.6F, mirror);
            box(root, "stirrup_tread_" + id, u + 2, 97, x, 3.5F, z - 1.8F, 0.65F, 0.6F, 3.6F, mirror);
            box(
                    root,
                    "strap_buckle_" + id,
                    66,
                    97,
                    side < 0 ? -6.3F : 6.15F,
                    -5.8F,
                    z - 0.7F,
                    0.15F,
                    1.4F,
                    1.4F,
                    mirror);
        }
    }

    private static void box(
            PartDefinition root,
            String name,
            int u,
            int v,
            float x,
            float y,
            float z,
            float width,
            float height,
            float depth) {
        box(root, name, u, v, x, y, z, width, height, depth, false);
    }

    private static void box(
            PartDefinition root,
            String name,
            int u,
            int v,
            float x,
            float y,
            float z,
            float width,
            float height,
            float depth,
            boolean mirror) {
        root.addOrReplaceChild(
                name,
                CubeListBuilder.create().texOffs(u, v).mirror(mirror).addBox(x, y, z, width, height, depth),
                PartPose.ZERO);
    }

    void renderSaddle(PoseStack pose, VertexConsumer vertices, int light) {
        pose.pushPose();
        body.translateAndRotate(pose);
        body.getChild(ROOT).render(pose, vertices, light, OverlayTexture.NO_OVERLAY);
        pose.popPose();
    }
}
