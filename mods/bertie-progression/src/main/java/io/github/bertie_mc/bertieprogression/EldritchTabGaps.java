package io.github.bertie_mc.bertieprogression;

import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;

/**
 * Puts Discerning The Eldritch's tab-less items back in its own creative tab.
 *
 * <p>The mod registers 72 items and adds 65 of them to {@code discerning_the_eldritch:dte_items_tab}.
 * The nine below are registered, named, craftable and quest-referenced, but sit in no tab at all -
 * King's Visage among them. EMI builds its index from the creative search list, so an item outside
 * every tab is invisible there even though it exists and works.
 *
 * <p>Adding them here rather than filing it upstream keeps the pack's own quest lines legible: a
 * quest can name an item the player has no way to look up otherwise.
 */
public final class EldritchTabGaps {

    private static final ResourceLocation TAB =
            ResourceLocation.fromNamespaceAndPath("discerning_the_eldritch", "dte_items_tab");

    private static final List<String> MISSING = List.of(
            "kings_effigy",
            "staff_of_eldritch",
            "frozen_folio",
            "guardian_guidebook",
            "dream_reaver_ymir",
            "bucket_of_malice",
            "apothic_acolyte_spawn_egg",
            "apothic_crusader_spawn_egg",
            "apothic_summoner_spawn_egg");

    private EldritchTabGaps() {}

    @SubscribeEvent
    public static void onBuildTabContents(BuildCreativeModeTabContentsEvent event) {
        if (!event.getTabKey().location().equals(TAB)) {
            return;
        }
        for (String path : MISSING) {
            ResourceLocation id = ResourceLocation.fromNamespaceAndPath("discerning_the_eldritch", path);
            Item item = BuiltInRegistries.ITEM.getOptional(id).orElse(null);
            // The mod could add one of these itself in a later version; do not double it up.
            if (item == null || contains(event.getParentEntries(), item)) {
                continue;
            }
            event.accept(new ItemStack(item), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
        }
    }

    private static boolean contains(Iterable<ItemStack> entries, Item wanted) {
        for (ItemStack entry : entries) {
            if (entry.getItem() == wanted) {
                return true;
            }
        }
        return false;
    }
}
