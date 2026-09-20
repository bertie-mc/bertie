package io.github.bertie_mc.bertieprogression.client;

import io.github.bertie_mc.bertieprogression.BertieProgression;
import io.github.bertie_mc.bertieprogression.HazenUnreleased;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;

/**
 * Wears the Flamebearer Soul armour.
 *
 * <p>The set is registered by {@link HazenUnreleased} under Haze n Stuff's own ids, which gets the
 * items but not the renderer that would draw them on a player. Its geometry is read out of that
 * mod's jar and rebuilt as a humanoid layer here, so the armour renders through vanilla's own
 * armour layer with the mod's texture.
 */
@EventBusSubscriber(modid = BertieProgression.MODID, value = Dist.CLIENT)
public final class HazenArmorClient {

    private HazenArmorClient() {}

    private static final String GEO = "geo/armor/garments_of_the_first_flamebearer_armor.geo.json";

    private static final ModelLayerLocation LAYER = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(BertieProgression.MODID, "flamebearer_soul"),
            "main");

    private static GeoArmorGeometry.Built built;
    private static GeoArmorModel model;

    private static GeoArmorGeometry.Built built() {
        if (built == null) {
            built = GeoArmorGeometry.load(HazenUnreleased.HAZEN, GEO);
        }
        return built;
    }

    @SubscribeEvent
    static void onRegisterLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
        if (!ModList.get().isLoaded(HazenUnreleased.HAZEN)) {
            return;
        }
        event.registerLayerDefinition(LAYER, () -> built().layer());
    }

    @SubscribeEvent
    static void onRegisterClientExtensions(RegisterClientExtensionsEvent event) {
        if (!ModList.get().isLoaded(HazenUnreleased.HAZEN)) {
            return;
        }
        List<Item> pieces = new ArrayList<>();
        for (ResourceLocation id : HazenUnreleased.soulArmourIds()) {
            BuiltInRegistries.ITEM.getOptional(id).ifPresent(pieces::add);
        }
        if (pieces.isEmpty()) {
            return;
        }
        event.registerItem(new IClientItemExtensions() {
            @Override
            public HumanoidModel<?> getHumanoidArmorModel(LivingEntity entity, ItemStack stack,
                                                          EquipmentSlot slot,
                                                          HumanoidModel<?> original) {
                return armour().forSlot(slot);
            }
        }, pieces.toArray(new Item[0]));
    }

    /** Baked on first use: the model set is not built until the client has loaded its resources. */
    private static GeoArmorModel armour() {
        if (model == null) {
            model = new GeoArmorModel(Minecraft.getInstance().getEntityModels().bakeLayer(LAYER),
                    built().slotParts());
        }
        return model;
    }
}
