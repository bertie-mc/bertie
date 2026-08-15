package io.github.bertie_mc.witheringwaver.client;

import io.github.bertie_mc.witheringwaver.entity.WitheringWaverEntity;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * Half-decomposed wither skeleton / piglin hybrid under a hooded cloak.
 *
 * The cloak is five hinged panels — two front, one double-width back, two sides —
 * closing into a rectangle around the body at rest. Casting swings the front pair out
 * toward 10 and 2 o'clock while the back panel throws rearward and sweeps between the
 * 4.5 and 7.5 o'clock diagonals.
 *
 * Assimilation shows real bone armor parts (cuirass, bracers, greaves) instead of a
 * texture overlay, and that plating physically dampens the cloak's movement.
 */
@OnlyIn(Dist.CLIENT)
public class WitheringWaverModel extends HumanoidModel<WitheringWaverEntity> {
    private static final float FRONT_SPLAY_YAW = 1.05F; // ~60 degrees: 10 and 2 o'clock
    private static final float BACK_SWEEP_YAW = 0.79F; // ~45 degrees: 4.5 and 7.5 o'clock
    private static final float ARMORED_DAMPING = 0.35F;

    private final ModelPart hood;
    private final ModelPart mantle;
    private final ModelPart cloakBack;
    private final ModelPart cloakFrontLeft;
    private final ModelPart cloakFrontRight;
    private final ModelPart cloakSideLeft;
    private final ModelPart cloakSideRight;
    private final ModelPart cuirass;
    private final ModelPart bracerRight;
    private final ModelPart bracerLeft;
    private final ModelPart greaveRight;
    private final ModelPart greaveLeft;

    public WitheringWaverModel(ModelPart root) {
        super(root);
        this.hood = this.head.getChild("hood");
        this.mantle = this.body.getChild("mantle");
        this.cloakBack = this.body.getChild("cloak_back");
        this.cloakFrontLeft = this.body.getChild("cloak_front_left");
        this.cloakFrontRight = this.body.getChild("cloak_front_right");
        this.cloakSideLeft = this.body.getChild("cloak_side_left");
        this.cloakSideRight = this.body.getChild("cloak_side_right");
        this.cuirass = this.body.getChild("cuirass");
        this.bracerRight = this.rightArm.getChild("bracer_right");
        this.bracerLeft = this.leftArm.getChild("bracer_left");
        this.greaveRight = this.rightLeg.getChild("greave_right");
        this.greaveLeft = this.leftLeg.getChild("greave_left");
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = HumanoidModel.createMesh(CubeDeformation.NONE, 0.0F);
        PartDefinition root = mesh.getRoot();

        // Two boxes only. The skin half-cube fills the head's right half at full
        // size; the skull is recessed 1px on every exposed side, so the border is
        // one continuous 1px drop with solid walls (the skin cube's cut face).
        PartDefinition head = root.addOrReplaceChild(
                "head",
                CubeListBuilder.create().texOffs(0, 0).addBox(-3.0F, -7.0F, -3.0F, 3.0F, 7.0F, 6.0F),
                PartPose.ZERO);
        head.addOrReplaceChild(
                "skin_half",
                CubeListBuilder.create().texOffs(0, 48).addBox(0.0F, -8.0F, -4.0F, 4.0F, 8.0F, 8.0F),
                PartPose.ZERO);
        // Piglin-exact features. Full snout across both halves, 2 deep so it
        // reaches the recessed skull without floating. Each tusk is vanilla's
        // three pixels: the khaki base painted on the snout's bottom corner plus
        // the 1x2 box beside it, fronts flush with the snout on BOTH sides.
        head.addOrReplaceChild(
                "snout",
                CubeListBuilder.create().texOffs(72, 0).addBox(-2.0F, -4.0F, -5.0F, 4.0F, 4.0F, 2.0F),
                PartPose.ZERO);
        head.addOrReplaceChild(
                "left_tusk",
                CubeListBuilder.create().texOffs(84, 0).addBox(0.0F, -2.0F, -1.0F, 1.0F, 2.0F, 1.0F),
                PartPose.offset(2.0F, 0.0F, -4.0F));
        head.addOrReplaceChild(
                "right_tusk",
                CubeListBuilder.create().texOffs(84, 0).addBox(0.0F, -2.0F, -1.0F, 1.0F, 2.0F, 1.0F),
                PartPose.offset(-3.0F, 0.0F, -4.0F));
        head.addOrReplaceChild(
                "ear",
                CubeListBuilder.create().texOffs(92, 0).addBox(-0.5F, 0.0F, -2.0F, 1.0F, 5.0F, 4.0F),
                PartPose.offsetAndRotation(4.5F, -6.0F, 0.0F, 0.0F, 0.0F, -0.5F));
        head.addOrReplaceChild(
                "hood",
                CubeListBuilder.create()
                        .texOffs(32, 0)
                        .addBox(-5.0F, -9.0F, -5.0F, 10.0F, 10.0F, 10.0F, new CubeDeformation(0.25F)),
                PartPose.ZERO);

        root.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.ZERO);

        PartDefinition body = root.addOrReplaceChild(
                "body",
                CubeListBuilder.create().texOffs(0, 16).addBox(-4.0F, 0.0F, -2.0F, 8.0F, 12.0F, 4.0F),
                PartPose.ZERO);
        body.addOrReplaceChild(
                "mantle",
                CubeListBuilder.create()
                        .texOffs(82, 13)
                        .addBox(-6.0F, -1.0F, -3.5F, 12.0F, 4.0F, 7.0F, new CubeDeformation(0.2F)),
                PartPose.ZERO);
        body.addOrReplaceChild(
                "cloak_back",
                CubeListBuilder.create().texOffs(48, 32).addBox(-4.5F, 0.0F, -0.5F, 9.0F, 15.0F, 1.0F),
                PartPose.offset(0.0F, 0.0F, 3.2F));
        body.addOrReplaceChild(
                "cloak_front_left",
                CubeListBuilder.create().texOffs(68, 32).addBox(-0.25F, 0.0F, -0.5F, 4.75F, 15.0F, 1.0F),
                PartPose.offset(0.0F, 0.0F, -3.2F));
        body.addOrReplaceChild(
                "cloak_front_right",
                CubeListBuilder.create().texOffs(80, 32).addBox(-4.5F, 0.0F, -0.5F, 4.75F, 15.0F, 1.0F),
                PartPose.offset(0.0F, 0.0F, -3.2F));
        body.addOrReplaceChild(
                "cloak_side_left",
                CubeListBuilder.create().texOffs(92, 32).addBox(-0.5F, 0.0F, -3.0F, 1.0F, 15.0F, 6.0F),
                PartPose.offset(5.0F, 0.0F, 0.0F));
        body.addOrReplaceChild(
                "cloak_side_right",
                CubeListBuilder.create().texOffs(92, 32).addBox(-0.5F, 0.0F, -3.0F, 1.0F, 15.0F, 6.0F),
                PartPose.offset(-5.0F, 0.0F, 0.0F));

        body.addOrReplaceChild(
                "cuirass",
                CubeListBuilder.create()
                        .texOffs(32, 48)
                        .addBox(-4.0F, 0.0F, -2.0F, 8.0F, 7.0F, 4.0F, new CubeDeformation(0.75F)),
                PartPose.ZERO);

        PartDefinition rightArm = root.addOrReplaceChild(
                "right_arm",
                CubeListBuilder.create().texOffs(0, 32).addBox(-1.0F, -2.0F, -1.0F, 2.0F, 12.0F, 2.0F),
                PartPose.offset(-5.0F, 2.0F, 0.0F));
        rightArm.addOrReplaceChild(
                "bracer_right",
                CubeListBuilder.create()
                        .texOffs(56, 48)
                        .addBox(-1.0F, 4.0F, -1.0F, 2.0F, 5.0F, 2.0F, new CubeDeformation(0.55F)),
                PartPose.ZERO);
        PartDefinition leftArm = root.addOrReplaceChild(
                "left_arm",
                CubeListBuilder.create().texOffs(8, 32).addBox(-1.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F),
                PartPose.offset(5.0F, 2.0F, 0.0F));
        leftArm.addOrReplaceChild(
                "bracer_left",
                CubeListBuilder.create()
                        .texOffs(64, 48)
                        .addBox(-1.0F, 4.0F, -2.0F, 4.0F, 5.0F, 4.0F, new CubeDeformation(0.55F)),
                PartPose.ZERO);
        PartDefinition rightLeg = root.addOrReplaceChild(
                "right_leg",
                CubeListBuilder.create().texOffs(24, 32).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 12.0F, 2.0F),
                PartPose.offset(-2.0F, 12.0F, 0.0F));
        rightLeg.addOrReplaceChild(
                "greave_right",
                CubeListBuilder.create()
                        .texOffs(80, 48)
                        .addBox(-1.0F, 0.0F, -1.0F, 2.0F, 6.0F, 2.0F, new CubeDeformation(0.5F)),
                PartPose.ZERO);
        PartDefinition leftLeg = root.addOrReplaceChild(
                "left_leg",
                CubeListBuilder.create().texOffs(32, 32).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F),
                PartPose.offset(1.9F, 12.0F, 0.0F));
        leftLeg.addOrReplaceChild(
                "greave_left",
                CubeListBuilder.create()
                        .texOffs(106, 48)
                        .addBox(-2.0F, 0.0F, -2.0F, 4.0F, 6.0F, 4.0F, new CubeDeformation(0.5F)),
                PartPose.ZERO);

        return LayerDefinition.create(mesh, 128, 64);
    }

    @Override
    public void setupAnim(
            WitheringWaverEntity entity,
            float limbSwing,
            float limbSwingAmount,
            float ageInTicks,
            float netHeadYaw,
            float headPitch) {
        super.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
        this.hat.visible = false;

        boolean armored = entity.isAssimilated();
        this.cuirass.visible = armored;
        this.bracerRight.visible = armored;
        this.bracerLeft.visible = armored;
        this.greaveRight.visible = armored;
        this.greaveLeft.visible = armored;

        this.hood.resetPose();
        this.mantle.resetPose();
        this.cloakBack.resetPose();
        this.cloakFrontLeft.resetPose();
        this.cloakFrontRight.resetPose();
        this.cloakSideLeft.resetPose();
        this.cloakSideRight.resetPose();

        float stateTime = Math.max(0.0F, ageInTicks - entity.getClientStateStartTick());
        float flare = 0.0F;
        float hoodBack = 0.0F;
        float cast = 0.0F;

        switch (entity.getAbilityState()) {
            case WitheringWaverEntity.STATE_REAP_WINDUP -> {
                float p = Mth.clamp(stateTime / 14.0F, 0.0F, 1.0F);
                cast = Mth.sin(p * Mth.PI);
                flare = cast;
                this.rightArm.zRot += 0.45F * cast;
                this.leftArm.zRot -= 0.45F * cast;
            }
            case WitheringWaverEntity.STATE_ASSIM_WINDUP -> {
                float p = Mth.clamp(stateTime / 14.0F, 0.0F, 1.0F);
                cast = Math.min(1.0F, p * 2.0F);
                flare = 0.55F * cast;
                this.leftArm.xRot = -1.5F * p;
                this.leftArm.zRot = -0.2F + Mth.sin(stateTime * 0.9F) * 0.15F;
            }
            case WitheringWaverEntity.STATE_VOLLEY_ACCEL -> {
                hoodBack = Mth.clamp(stateTime / 8.0F, 0.0F, 1.0F);
                cast = 0.35F;
                flare = 0.15F;
                pointRightArm();
            }
            case WitheringWaverEntity.STATE_VOLLEY_FIRE -> {
                hoodBack = 1.0F;
                cast = 0.35F;
                flare = 0.15F;
                pointRightArm();
                this.rightArm.xRot += Mth.sin(stateTime * Mth.TWO_PI / 16.0F) * 0.08F;
            }
            case WitheringWaverEntity.STATE_SUMMON_CAST -> {
                float p = Mth.clamp(stateTime / 20.0F, 0.0F, 1.0F);
                cast = Mth.sin(p * Mth.PI);
                flare = 0.35F * cast;
                this.leftArm.xRot = -1.9F * Math.min(1.0F, p * 2.0F);
                this.leftArm.zRot = -0.3F + Mth.sin(stateTime * 1.1F) * 0.2F;
            }
            default -> {}
        }

        // Bone plating physically restricts the cloth.
        if (armored) {
            flare *= ARMORED_DAMPING;
            cast *= ARMORED_DAMPING;
        }

        float idleRipple = 0.04F * Mth.sin(ageInTicks * 0.11F);
        float walk = Mth.cos(limbSwing * 0.6662F) * limbSwingAmount;

        // Front pair: forward and sideways, out to 10 and 2 o'clock while casting.
        float frontFlare = -(0.35F + 0.55F * flare) * cast - Math.max(0.0F, -this.leftLeg.xRot) * 0.2F;
        this.cloakFrontLeft.xRot = frontFlare + idleRipple;
        this.cloakFrontRight.xRot = frontFlare + idleRipple;
        this.cloakFrontLeft.yRot = -FRONT_SPLAY_YAW * cast;
        this.cloakFrontRight.yRot = FRONT_SPLAY_YAW * cast;

        // Back panel: thrown rearward, sweeping between the 4.5 and 7.5 o'clock diagonals.
        this.cloakBack.xRot = (0.4F + 0.6F * flare) * cast + Math.abs(walk) * 0.06F + idleRipple;
        this.cloakBack.yRot = BACK_SWEEP_YAW * cast * Mth.sin(stateTime * 0.45F);

        // Sides: a mild outward flare, mostly along for the ride.
        this.cloakSideLeft.zRot = -(0.25F + 0.35F * flare) * cast - idleRipple;
        this.cloakSideRight.zRot = (0.25F + 0.35F * flare) * cast + idleRipple;

        // The bone plating props the coat open at rest — the cuirass sits in the
        // front gap instead of vanishing behind closed panels.
        if (armored) {
            this.cloakFrontLeft.yRot -= 0.55F;
            this.cloakFrontRight.yRot += 0.55F;
            this.cloakFrontLeft.xRot -= 0.12F;
            this.cloakFrontRight.xRot -= 0.12F;
            this.cloakSideLeft.zRot -= 0.15F;
            this.cloakSideRight.zRot += 0.15F;
            this.cloakBack.xRot += 0.10F;
        }

        this.mantle.xRot = flare * 0.15F * cast;
        // Thrown back over the mantle when the volley whips up, never over the face.
        this.hood.xRot = -1.7F * hoodBack;
    }

    private void pointRightArm() {
        this.rightArm.xRot = -Mth.HALF_PI + this.head.xRot;
        this.rightArm.yRot = this.head.yRot;
        this.rightArm.zRot = 0.0F;
    }
}
