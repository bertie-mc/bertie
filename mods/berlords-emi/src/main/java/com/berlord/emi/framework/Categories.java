package com.berlord.emi.framework;

import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.stack.EmiStack;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

import java.util.Locale;

/** Helpers for declaring machine categories (id under berlords_emi, literal name, workstation icon). */
public final class Categories {
    private Categories() {
    }

    private static final String NS = "berlords_emi";

    /** Create + register a category and its workstation. {@code workstationItemId} is "namespace:path". */
    public static GenericEmiCategory machine(EmiRegistry reg, String key, String workstationItemId, String name) {
        EmiStack icon = stack(workstationItemId);
        GenericEmiCategory cat = new GenericEmiCategory(
                ResourceLocation.fromNamespaceAndPath(NS, key), icon, Component.literal(name));
        reg.addCategory(cat);
        if (!icon.isEmpty()) {
            reg.addWorkstation(cat, icon);
        }
        return cat;
    }

    /** Category with an icon but NO workstation (for mechanics with no machine block). */
    public static GenericEmiCategory machineNoStation(EmiRegistry reg, String key, String iconItemId, String name) {
        GenericEmiCategory cat = new GenericEmiCategory(
                ResourceLocation.fromNamespaceAndPath(NS, key), stack(iconItemId), Component.literal(name));
        reg.addCategory(cat);
        return cat;
    }

    public static EmiStack stack(String itemId) {
        Item item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(itemId));
        return EmiStack.of(item);
    }

    public static String seconds(int ticks) {
        double s = ticks / 20.0;
        return (s == Math.floor(s) ? String.valueOf((long) s) : String.format(Locale.ROOT, "%.1f", s)) + "s";
    }

    public static String capitalize(String s) {
        return s == null || s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
