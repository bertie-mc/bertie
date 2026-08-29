package io.github.bertie_mc.bertieprogression;

import java.util.Collection;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.neoforged.bus.api.SubscribeEvent;
import top.theillusivec4.curios.api.SlotAttribute;
import top.theillusivec4.curios.api.event.CurioAttributeModifierEvent;

/**
 * The Triple Strip Cape widens the cape slot, not the back slot.
 *
 * <p>L2Hostility's cape hands out three extra slots, and names {@code back} directly in code, which
 * made it a backpack rack rather than a cape. It is a cape now, so the slots it opens should be
 * cape slots too.
 *
 * <p>Done through Curios' own attribute event rather than a mixin: the item builds its modifier map
 * fresh on every query, and this event is the supported place to edit that map on the way out.
 */
public final class CapeSlotHandler {

    private static final ResourceLocation CAPE = ResourceLocation.parse("l2hostility:triple_strip_cape");
    private static final String FROM = "back";
    private static final String TO = "cape";

    private CapeSlotHandler() {
    }

    @SubscribeEvent
    public static void onCurioAttributes(CurioAttributeModifierEvent event) {
        if (!CAPE.equals(BuiltInRegistries.ITEM.getKey(event.getItemStack().getItem()))) {
            return;
        }
        Holder<Attribute> from = SlotAttribute.getOrCreate(FROM);
        Collection<AttributeModifier> moved = event.removeAttribute(from);
        if (moved == null || moved.isEmpty()) {
            return;
        }
        Holder<Attribute> to = SlotAttribute.getOrCreate(TO);
        for (AttributeModifier modifier : moved) {
            event.addModifier(to, modifier);
        }
    }
}
