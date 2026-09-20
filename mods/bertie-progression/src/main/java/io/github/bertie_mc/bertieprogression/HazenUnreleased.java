package io.github.bertie_mc.bertieprogression;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.RegisterEvent;

/**
 * Registers nine items Haze n Stuff ships art for and never registers.
 *
 * <p>The mod carries finished models, textures and translation strings for a fifth Flamebearer
 * armour tier and five curios, but no registry entry, so none of it can be reached in game. These
 * are registered here under the mod's own ids, which is what {@link RegisterEvent} allows and a
 * {@code DeferredRegister} does not: the ids stay the mod's, so the wall, the recipes and any save
 * that already names them keep working if it ever ships its own.
 *
 * <p>Registering an item does not bring its renderer, which for both the armour and the curios is
 * Geckolib geometry the mod draws only for its own. Both are recovered from that geometry instead:
 * the curios by converting it into vanilla item models, the armour by
 * {@code client/GeoArmorGeometry} rebuilding it as a humanoid layer. Stats are placeholders.
 */
public final class HazenUnreleased {

    private HazenUnreleased() {}

    public static final String HAZEN = "hazennstuff";

    private static final String SOUL = "garments_of_the_first_flamebearer_soul_";

    /** The mod's own worn-armour texture, handed to the armour layer by {@link SoulArmorItem}. */
    public static final ResourceLocation SOUL_ARMOR_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            HAZEN, "textures/armor/garments_of_the_first_flamebearer_soul_armor.png");

    private static final ArmorItem.Type[] PIECES = {ArmorItem.Type.HELMET,
            ArmorItem.Type.CHESTPLATE, ArmorItem.Type.LEGGINGS, ArmorItem.Type.BOOTS};

    private static final List<String> CURIOS = List.of(
            "chronicles_of_neptune", "ebony_scroll", "lunarnomicon",
            "radiant_crown_of_scrolls", "grimoire_of_flight");

    public static final DeferredRegister<ArmorMaterial> ARMOR_MATERIALS =
            DeferredRegister.create(Registries.ARMOR_MATERIAL, BertieProgression.MODID);

    /** Placeholder stats, roughly the tier the base Tyros set sits at. */
    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> FLAMEBEARER_SOUL =
            ARMOR_MATERIALS.register("flamebearer_soul", () -> {
                Map<ArmorItem.Type, Integer> defence = new EnumMap<>(ArmorItem.Type.class);
                defence.put(ArmorItem.Type.HELMET, 4);
                defence.put(ArmorItem.Type.CHESTPLATE, 9);
                defence.put(ArmorItem.Type.LEGGINGS, 7);
                defence.put(ArmorItem.Type.BOOTS, 4);
                defence.put(ArmorItem.Type.BODY, 11);
                // The layer this names is never read - SoulArmorItem answers with the mod's own
                // texture - but the armour layer iterates this list, so there has to be one.
                return new ArmorMaterial(defence, 15, SoundEvents.ARMOR_EQUIP_NETHERITE,
                        () -> Ingredient.EMPTY,
                        List.of(new ArmorMaterial.Layer(ResourceLocation.fromNamespaceAndPath(
                                BertieProgression.MODID, "flamebearer_soul"))),
                        3.0F, 0.1F);
            });

    /** Armour that wears the texture its own mod drew for it rather than a vanilla layer. */
    private static final class SoulArmorItem extends ArmorItem {
        private SoulArmorItem(Holder<ArmorMaterial> material, Type type, Properties properties) {
            super(material, type, properties);
        }

        @Override
        public ResourceLocation getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot,
                                                ArmorMaterial.Layer layer, boolean innerModel) {
            return SOUL_ARMOR_TEXTURE;
        }
    }

    public static List<ResourceLocation> soulArmourIds() {
        List<ResourceLocation> out = new ArrayList<>();
        for (ArmorItem.Type type : PIECES) {
            out.add(hazen(SOUL + type.getName()));
        }
        return out;
    }

    private static ResourceLocation hazen(String path) {
        return ResourceLocation.fromNamespaceAndPath(HAZEN, path);
    }

    @SubscribeEvent
    public static void onRegister(RegisterEvent event) {
        if (!ModList.get().isLoaded(HAZEN)) {
            return;
        }
        event.register(Registries.ITEM, helper -> {
            for (ArmorItem.Type type : PIECES) {
                ResourceLocation id = hazen(SOUL + type.getName());
                if (BuiltInRegistries.ITEM.containsKey(id)) {
                    continue;
                }
                helper.register(id, new SoulArmorItem(FLAMEBEARER_SOUL, type,
                        new Item.Properties().rarity(Rarity.EPIC)
                                .durability(type.getDurability(37))));
            }
            for (String path : CURIOS) {
                ResourceLocation id = hazen(path);
                if (BuiltInRegistries.ITEM.containsKey(id)) {
                    continue;
                }
                helper.register(id, new Item(new Item.Properties().stacksTo(1).rarity(Rarity.RARE)));
            }
        });
    }

    @SubscribeEvent
    public static void onBuildTabs(BuildCreativeModeTabContentsEvent event) {
        if (!ModList.get().isLoaded(HAZEN)) {
            return;
        }
        if (event.getTabKey() == CreativeModeTabs.COMBAT) {
            for (ArmorItem.Type type : PIECES) {
                accept(event, SOUL + type.getName());
            }
        } else if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            CURIOS.forEach(path -> accept(event, path));
        }
    }

    private static void accept(BuildCreativeModeTabContentsEvent event, String path) {
        BuiltInRegistries.ITEM.getOptional(hazen(path)).ifPresent(event::accept);
    }
}
