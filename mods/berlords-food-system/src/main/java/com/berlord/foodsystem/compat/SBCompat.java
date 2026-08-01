package com.berlord.foodsystem.compat;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Sophisticated Backpacks compat: the Advanced Feeding Upgrade is redundant under the
 * stomach system (identical wrapper logic, just more filter slots — and the only two
 * extra GUI buttons it adds key off the vanilla hunger bar we pin). It is disabled
 * entirely: no recipe (conditional override), hidden from EMI/JEI/REI (c tag), removed
 * from creative tabs, and its wrapper never feeds.
 */
public final class SBCompat {

    public static final ResourceLocation ADVANCED_FEEDING_UPGRADE_ID =
            ResourceLocation.fromNamespaceAndPath("sophisticatedbackpacks", "advanced_feeding_upgrade");

    public static boolean isAdvancedFeedingUpgrade(ItemStack stack) {
        return BuiltInRegistries.ITEM.getKey(stack.getItem()).equals(ADVANCED_FEEDING_UPGRADE_ID);
    }

    /** the item, when SB is installed */
    public static Item advancedFeedingUpgrade() {
        return BuiltInRegistries.ITEM.getOptional(ADVANCED_FEEDING_UPGRADE_ID).orElse(null);
    }

    private SBCompat() {}
}
