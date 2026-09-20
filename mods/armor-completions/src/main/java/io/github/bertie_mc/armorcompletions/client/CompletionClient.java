package io.github.bertie_mc.armorcompletions.client;

import io.github.bertie_mc.armorcompletions.ArmorCompletions;
import io.github.bertie_mc.armorcompletions.ArmorFamily;
import io.github.bertie_mc.armorcompletions.client.models.*;
import io.redspace.ironsspellbooks.entity.armor.GenericCustomArmorRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import software.bernie.geckolib.renderer.GeoArmorRenderer;

@EventBusSubscriber(modid = ArmorCompletions.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class CompletionClient {
    private CompletionClient() {}

    public static GeoArmorRenderer<?> geoRenderer(ArmorFamily family) {
        return switch (family) {
            case BISHOP -> new GenericCustomArmorRenderer<>(new BishopCompletionArmorModel<>());
            case PYROMANCER_BRUTE -> new GenericCustomArmorRenderer<>(new PyromancerBruteCompletionArmorModel<>());
            case NAMELESS_ONE -> new GenericCustomArmorRenderer<>(new NamelessOneCompletionArmorModel<>());
            case NECROMANCER -> new GenericCustomArmorRenderer<>(new NecromancerCompletionArmorModel<>());
            default -> throw new IllegalArgumentException("No GeckoLib renderer for " + family);
        };
    }

    @SubscribeEvent
    public static void layers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(Modelspinyshellarmorleggings.LAYER_LOCATION, Modelspinyshellarmorleggings::createBodyLayer);
        event.registerLayerDefinition(Modelspinyshellarmorboots.LAYER_LOCATION, Modelspinyshellarmorboots::createBodyLayer);
        event.registerLayerDefinition(BoneReptileLeggingsModel.LAYER_LOCATION, BoneReptileLeggingsModel::createArmorLayer);
        event.registerLayerDefinition(BoneReptileBootsModel.LAYER_LOCATION, BoneReptileBootsModel::createArmorLayer);
    }

    @SubscribeEvent
    public static void itemExtensions(RegisterClientExtensionsEvent event) {
        ArmorCompletions.PIECES.forEach((piece, item) -> {
            if (piece.family() == ArmorFamily.SPINY_SHELL || piece.family() == ArmorFamily.BONE_REPTILE) {
                event.registerItem(new NativeArmorExtension(piece.family(), piece.type()), item.get());
            }
        });
    }

    private static final class NativeArmorExtension implements IClientItemExtensions {
        private final ArmorFamily family;
        private final ArmorItem.Type type;
        private HumanoidModel<LivingEntity> model;
        NativeArmorExtension(ArmorFamily family, ArmorItem.Type type) { this.family = family; this.type = type; }

        @Override
        public HumanoidModel<?> getHumanoidArmorModel(LivingEntity entity, ItemStack stack, EquipmentSlot slot, HumanoidModel<?> original) {
            if (model == null) {
                boolean legs = type == ArmorItem.Type.LEGGINGS;
                if (family == ArmorFamily.SPINY_SHELL) {
                    var root = Minecraft.getInstance().getEntityModels().bakeLayer(legs ? Modelspinyshellarmorleggings.LAYER_LOCATION : Modelspinyshellarmorboots.LAYER_LOCATION);
                    model = legs ? Modelspinyshellarmorleggings.asHumanoidModel(root) : Modelspinyshellarmorboots.asHumanoidModel(root);
                } else {
                    var root = Minecraft.getInstance().getEntityModels().bakeLayer(legs ? BoneReptileLeggingsModel.LAYER_LOCATION : BoneReptileBootsModel.LAYER_LOCATION);
                    model = legs ? new BoneReptileLeggingsModel<>(root) : new BoneReptileBootsModel<>(root);
                }
            }
            model.crouching = original.crouching;
            model.riding = original.riding;
            model.young = original.young;
            model.attackTime = original.attackTime;
            model.head.copyFrom(original.head);
            model.hat.copyFrom(original.hat);
            model.body.copyFrom(original.body);
            model.rightArm.copyFrom(original.rightArm);
            model.leftArm.copyFrom(original.leftArm);
            model.rightLeg.copyFrom(original.rightLeg);
            model.leftLeg.copyFrom(original.leftLeg);
            return model;
        }
    }
}
