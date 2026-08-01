package com.berlord.emi.ali;

import com.sammy.malum.core.listeners.ReapingDataReloadListener;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Reads Malum's soul-reaping drop table ({@code ReapingDataReloadListener.REAPING_DATA}, populated from
 * {@code data/malum/reaping_data/*.json} on datapack reload) into a Malum-free shape.
 *
 * <p>ALL references to Malum classes live in this class ONLY, so it must never be loaded unless
 * {@code malum} is present — {@link MalumReapingAliPlugin} checks that before touching it.
 */
final class MalumReapingSource {

    private MalumReapingSource() {
    }

    /** One reaped drop: which entity drops what, with the roll chance and count range. */
    record Drop(ResourceLocation entityId, ItemStack stack, float chance, int min, int max) {
    }

    /**
     * Snapshot the current reaping table. Called lazily (when ALI builds loot info) rather than at
     * registration, because the map is only filled once datapacks have loaded.
     */
    static List<Drop> collect() {
        List<Drop> out = new ArrayList<>();
        Map<ResourceLocation, List<ReapingDataReloadListener.MalumReapingDropsData>> data =
                ReapingDataReloadListener.REAPING_DATA;
        if (data == null || data.isEmpty()) {
            return out;
        }
        for (Map.Entry<ResourceLocation, List<ReapingDataReloadListener.MalumReapingDropsData>> e : data.entrySet()) {
            ResourceLocation entityId = e.getKey();
            List<ReapingDataReloadListener.MalumReapingDropsData> drops = e.getValue();
            if (entityId == null || drops == null) {
                continue;
            }
            for (ReapingDataReloadListener.MalumReapingDropsData d : drops) {
                try {
                    if (d == null || d.drop == null) {
                        continue;
                    }
                    // The drop is an Ingredient; show its first matching stack (these are all single items).
                    ItemStack[] items = d.drop.getItems();
                    if (items.length == 0 || items[0].isEmpty()) {
                        continue;
                    }
                    out.add(new Drop(entityId, items[0].copy(), d.chance, d.min, d.max));
                } catch (Throwable ignored) {
                    // one malformed reaping entry must not sink the rest
                }
            }
        }
        return out;
    }
}
