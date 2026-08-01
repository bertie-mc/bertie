package com.berlord.filters;

import dev.architectury.event.EventResult;
import dev.ftb.mods.ftbfiltersystem.api.event.CustomFilterEvent;
import dev.lopyluna.slag.content.items.modular.DataDynamicParts;
import dev.lopyluna.slag.register.AllDataComponents;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

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
    private WoodenSlagFilter() {
    }

    public static void register() {
        CustomFilterEvent.MATCH_ITEM.register(WoodenSlagFilter::matchItem);
    }

    private static EventResult matchItem(ItemStack stack, String eventId, String extraData) {
        if (!WoodenFilterPolicy.handles(eventId)) {
            return EventResult.pass();
        }

        DataComponentType<ResourceLocation> modType = AllDataComponents.MODULAR_TYPE.get();
        ResourceLocation mt = stack.get(modType);
        DataComponentType<DataDynamicParts> dpType = AllDataComponents.DYNAMIC_PARTS.get();
        DataDynamicParts parts = stack.get(dpType);
        if (mt == null || parts == null) {
            return EventResult.pass();
        }

        DataComponentType<ResourceLocation> matType = AllDataComponents.MATERIAL_TYPE.get();
        List<String> materialIds = new ArrayList<>();
        for (ItemStack part : parts.items()) {
            ResourceLocation pm = part.get(matType);
            if (pm != null) {
                materialIds.add(pm.toString());
            }
        }

        return WoodenFilterPolicy.matches(extraData, mt.toString(), materialIds)
                ? EventResult.interruptTrue()
                : EventResult.pass();
    }
}
