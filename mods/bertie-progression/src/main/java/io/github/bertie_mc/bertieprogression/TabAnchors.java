package io.github.bertie_mc.bertieprogression;

import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;

/**
 * Keeps the creative-tab entries that other mods use as insertion anchors.
 *
 * <p>NeoForge's {@code insertAfter} looks its anchor up in a map keyed by item <em>and</em> data
 * components, and throws {@code IllegalArgumentException} - a hard crash during mod loading - when
 * the key is missing. A mod that anchors on a vanilla item therefore dies if anything in the pack
 * removed that item from the tab, or merely left a component on it.
 *
 * <p>Antarchy anchors its Duct Tape on {@code minecraft:shears} in Tools and Utilities. It picks
 * the tab by PATH and ignores the namespace, so it aims at AnvilCraft's
 * {@code anvilcraft:tools_and_utilities} as well - a tab that has never held a pair of shears. That
 * took the pack down on load, and then again the moment the inventory screen built its tabs.
 *
 * <p>This class runs before Antarchy - see the {@code ordering="BEFORE"} dependency in the
 * mods.toml - and guarantees a plain, component-free Shears in every tab whose path is
 * {@code tools_and_utilities}, in the parent list and the search list both. Where one is already
 * there it does nothing at all.
 */
public final class TabAnchors {

    @SubscribeEvent
    public static void onBuildTabContents(BuildCreativeModeTabContentsEvent event) {
        // By path, not by key: Antarchy matches tabs the same way, so AnvilCraft's tab of the
        // same name needs the anchor too.
        if (!event.getTabKey().location().getPath().equals("tools_and_utilities")) {
            return;
        }
        ItemStack plain = new ItemStack(Items.SHEARS);
        if (contains(event.getParentEntries(), plain) && contains(event.getSearchEntries(), plain)) {
            return;
        }
        event.accept(plain, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
    }

    private static boolean contains(Iterable<ItemStack> entries, ItemStack wanted) {
        for (ItemStack stack : entries) {
            if (ItemStack.isSameItemSameComponents(stack, wanted)) {
                return true;
            }
        }
        return false;
    }

    private TabAnchors() {}
}
