package io.github.bertie_mc.bertieprogression.backpack;

import net.minecraft.core.component.DataComponentMap;
import net.minecraft.world.item.ItemStack;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.BackpackItem;
import net.p3pp3rf1y.sophisticatedcore.init.ModCoreDataComponents;

/**
 * Moves a backpack's identity from one tier to the next.
 *
 * <p>Sophisticated Backpacks keeps a backpack's contents in world-saved storage keyed by the
 * {@code sophisticatedcore:storage_uuid} data component, so an upgraded pack keeps what was inside
 * it only if that component - and the settings, colours and upgrades beside it - come across.
 * {@code sophisticatedbackpacks:backpack_upgrade}, the crafting-grid recipe type, does exactly that.
 * The pack's iron tier is a Hephaestus ritual and its diamond tier a Create sequenced assembly, and
 * neither of those recipe systems has any notion of carrying components, so both go through here.
 *
 * <p>Copying the components alone is not enough. The slot counts are components too, so a copper
 * pack's 33 slots would follow it into an iron one; the stock recipe re-sets both after copying and
 * so does {@link #retier}.
 */
public final class BackpackHandover {

    /** Whether this stack is one of Sophisticated's storages, i.e. worth carrying anything from. */
    public static boolean isStorage(ItemStack stack) {
        return !stack.isEmpty() && stack.has(ModCoreDataComponents.STORAGE_UUID.get());
    }

    /**
     * Copy every component from {@code from} onto {@code to}, then correct the slot counts if
     * {@code to} is a backpack of a different tier. Does nothing unless {@code from} is a storage.
     */
    public static void carry(ItemStack from, ItemStack to) {
        if (to.isEmpty() || !isStorage(from)) {
            return;
        }
        DataComponentMap components = from.getComponents();
        to.applyComponents(components);
        retier(to);
    }

    /**
     * Give a backpack the slot counts its own item declares, discarding whatever tier the copied
     * components came from.
     */
    public static void retier(ItemStack stack) {
        if (stack.getItem() instanceof BackpackItem backpack) {
            stack.set(ModCoreDataComponents.NUMBER_OF_INVENTORY_SLOTS.get(), backpack.getNumberOfSlots());
            stack.set(ModCoreDataComponents.NUMBER_OF_UPGRADE_SLOTS.get(), backpack.getNumberOfUpgradeSlots());
        }
    }

    private BackpackHandover() {}
}
