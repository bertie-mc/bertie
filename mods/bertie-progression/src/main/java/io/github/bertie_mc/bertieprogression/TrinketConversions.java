package io.github.bertie_mc.bertieprogression;

import java.util.List;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.ItemAttributeModifierEvent;
import net.neoforged.neoforge.event.ModifyDefaultComponentsEvent;

/**
 * Demotes two helmets to head trinkets.
 *
 * <p>Both are worn for one effect and nothing else, so they keep the effect and lose everything
 * that made them armour: the armour points, the toughness, the knockback resistance and the
 * durability. Slot membership is data — {@code #curios:hat}, written by the generator — and the
 * stat-stripping is here, because an item's default components are fixed at registration and
 * {@link ModifyDefaultComponentsEvent} is the only way to reach another mod's.
 *
 * <p>The Howling Helmet still goes in the helmet slot, deliberately: it is worth looking at. The
 * Fallen King's Crown is not, so {@code HeadTrinketSlotMixin} keeps it out of the armour slots and
 * reads {@link #blocksArmourSlot} to decide.
 */
public final class TrinketConversions {

    private TrinketConversions() {}

    private static final List<ResourceLocation> HEAD_TRINKETS = List.of(
            ResourceLocation.parse("ancient_forgemastery:howling_helmet"),
            ResourceLocation.parse("antarchy:fallen_king_crown"));

    private static final ResourceLocation ARMOUR_SLOT_BLOCKED =
            ResourceLocation.parse("antarchy:fallen_king_crown");

    /** True for a demoted helmet that must not occupy an armour slot at all. */
    public static boolean blocksArmourSlot(Item item) {
        return BuiltInRegistries.ITEM.getKey(item).equals(ARMOUR_SLOT_BLOCKED);
    }

    private static boolean isDemoted(Item item) {
        return HEAD_TRINKETS.contains(BuiltInRegistries.ITEM.getKey(item));
    }

    @SubscribeEvent
    public static void onModifyDefaultComponents(ModifyDefaultComponentsEvent event) {
        for (ResourceLocation id : HEAD_TRINKETS) {
            // Both mods are optional; an absent one simply has nothing to demote.
            BuiltInRegistries.ITEM.getOptional(id).ifPresent(item -> event.modify(item, builder -> builder
                    .set(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY)
                    .remove(DataComponents.MAX_DAMAGE)
                    .remove(DataComponents.DAMAGE)));
        }
    }

    /**
     * Clears the stats again at the point they are read.
     *
     * <p>Emptying {@code ATTRIBUTE_MODIFIERS} is not enough on its own: an {@code ArmorItem} whose
     * material carries defence, toughness and knockback resistance still hands them out from code,
     * so the component is never consulted and the demoted helmet keeps its armour bar. This event
     * is the last step before the modifiers are applied and displayed, which makes it the only
     * place that catches both sources.
     */
    public static final class Attributes {

        private Attributes() {}

        @SubscribeEvent
        public static void onGatherAttributes(ItemAttributeModifierEvent event) {
            if (isDemoted(event.getItemStack().getItem())) {
                event.clearModifiers();
            }
        }
    }
}
