package com.berlord.filters;

import dev.architectury.event.EventResult;
import dev.ftb.mods.ftbfiltersystem.api.event.CustomFilterEvent;
import dev.lopyluna.slag.content.items.modular.DataDynamicParts;
import dev.lopyluna.slag.register.AllDataComponents;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/**
 * The actual {@code bertie:wooden} custom-filter handler. References Slag-n-Embers types directly, so
 * it is only ever loaded/called from a path guarded by {@code ModList.isLoaded("slag")}
 * (see {@link BertieFilters}). If Slag is absent this class never loads.
 *
 * <p>Match contract (FFS {@code CustomFilterEvent.matchItem}):
 * <ul>
 *   <li>eventId  - the custom filter id; we only handle exactly {@code "bertie:wooden"}.</li>
 *   <li>extraData - the slag modular type path the quest is looking for, e.g.
 *       {@code "pickaxe"}, {@code "helmet"}, {@code "chestplate"}, {@code "leggings"}, {@code "boots"}.</li>
 * </ul>
 * Returns {@link EventResult#interruptTrue()} only when the stack is a slag modular item whose
 * modular type equals {@code slag:<extraData>} AND at least one of its dynamic parts has material
 * type {@code slag:wooden}. Otherwise {@link EventResult#pass()}.
 */
public final class WoodenSlagFilter {
    private static final ResourceLocation WOODEN = ResourceLocation.fromNamespaceAndPath("slag", "wooden");

    private WoodenSlagFilter() {
    }

    public static void register() {
        CustomFilterEvent.MATCH_ITEM.register(WoodenSlagFilter::matchItem);
    }

    private static EventResult matchItem(ItemStack stack, String eventId, String extraData) {
        if (!"bertie:wooden".equals(eventId)) {
            return EventResult.pass();
        }

        // 1) the item must be a slag modular item of the requested type (extraData = e.g. "pickaxe")
        DataComponentType<ResourceLocation> modType = AllDataComponents.MODULAR_TYPE.get();
        ResourceLocation mt = stack.get(modType);
        if (mt == null || !mt.equals(ResourceLocation.fromNamespaceAndPath("slag", extraData))) {
            return EventResult.pass();
        }

        // 2) its nested parts must contain a WOODEN part
        DataComponentType<DataDynamicParts> dpType = AllDataComponents.DYNAMIC_PARTS.get();
        DataDynamicParts parts = stack.get(dpType);
        if (parts == null) {
            return EventResult.pass();
        }

        DataComponentType<ResourceLocation> matType = AllDataComponents.MATERIAL_TYPE.get();
        for (ItemStack part : parts.items()) {
            ResourceLocation pm = part.get(matType);
            if (WOODEN.equals(pm)) {
                return EventResult.interruptTrue();
            }
        }
        return EventResult.pass();
    }
}
