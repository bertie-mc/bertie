package io.github.bertie_mc.bertieprogression;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
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
 * <p>The mod carries finished sprites, models and translation strings for a fifth Flamebearer
 * armour tier and five curios, but no registry entry, so none of it can be reached in game. These
 * are registered here under the mod's own ids, which is what {@link RegisterEvent} allows and a
 * {@code DeferredRegister} does not: the ids are the mod's, and the wall, the recipes and any save
 * that already names them keep working if the mod ever ships its own.
 *
 * <p>What this does not recover is the mod's renderers. Both the armour's worn model and the
 * curios' held models are Geckolib geometry that the mod draws only for items it registered, so
 * the armour wears invisibly and the curios use flat sprites projected from that geometry by
 * {@code texture-work/make_hazen_unreleased.py}. Stats are placeholders.
 */
public final class HazenUnreleased {

    private HazenUnreleased() {}

    public static final String HAZEN = "hazennstuff";

    private static final String SOUL = "garments_of_the_first_flamebearer_soul_";

    private static final List<String> CURIOS = List.of(
            "chronicles_of_neptune", "ebony_scroll", "lunarnomicon",
            "radiant_crown_of_scrolls", "grimoire_of_flight");

    /** Placeholder stats, roughly the tier the base Tyros set sits at. */
    public static final DeferredRegister<ArmorMaterial> ARMOR_MATERIALS =
            DeferredRegister.create(Registries.ARMOR_MATERIAL, BertieProgression.MODID);

    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> FLAMEBEARER_SOUL =
            ARMOR_MATERIALS.register("flamebearer_soul", () -> {
                Map<ArmorItem.Type, Integer> defence = new EnumMap<>(ArmorItem.Type.class);
                defence.put(ArmorItem.Type.HELMET, 4);
                defence.put(ArmorItem.Type.CHESTPLATE, 9);
                defence.put(ArmorItem.Type.LEGGINGS, 7);
                defence.put(ArmorItem.Type.BOOTS, 4);
                defence.put(ArmorItem.Type.BODY, 11);
                // No layers: the worn model belongs to the mod's Geckolib renderer, and a vanilla
                // layer would only paint the wrong texture over the player.
                return new ArmorMaterial(defence, 15, SoundEvents.ARMOR_EQUIP_NETHERITE,
                        () -> Ingredient.EMPTY, List.of(), 3.0F, 0.1F);
            });

    private static ResourceLocation hazen(String path) {
        return ResourceLocation.fromNamespaceAndPath(HAZEN, path);
    }

    @SubscribeEvent
    public static void onRegister(RegisterEvent event) {
        if (!ModList.get().isLoaded(HAZEN)) {
            return;
        }
        event.register(Registries.ITEM, helper -> {
            for (ArmorItem.Type type : new ArmorItem.Type[]{ArmorItem.Type.HELMET,
                    ArmorItem.Type.CHESTPLATE, ArmorItem.Type.LEGGINGS, ArmorItem.Type.BOOTS}) {
                ResourceLocation id = hazen(SOUL + type.getName());
                if (BuiltInRegistries.ITEM.containsKey(id)) {
                    continue;
                }
                helper.register(id, new ArmorItem(FLAMEBEARER_SOUL, type,
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
            for (ArmorItem.Type type : new ArmorItem.Type[]{ArmorItem.Type.HELMET,
                    ArmorItem.Type.CHESTPLATE, ArmorItem.Type.LEGGINGS, ArmorItem.Type.BOOTS}) {
                accept(event, SOUL + type.getName());
            }
        } else if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            CURIOS.forEach(path -> accept(event, path));
        }
    }

    private static void accept(BuildCreativeModeTabContentsEvent event, String path) {
        Holder.Reference<Item> item = BuiltInRegistries.ITEM.getHolder(hazen(path)).orElse(null);
        if (item != null) {
            event.accept(item.value());
        }
    }
}
