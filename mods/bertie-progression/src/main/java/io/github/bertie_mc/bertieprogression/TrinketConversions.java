package io.github.bertie_mc.bertieprogression;

import java.util.List;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.neoforged.bus.api.SubscribeEvent;
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
}
