package io.github.bertie_mc.fletching;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;

/** Stores batches, not fluid millibuckets: one bottle supplies two four-arrow crafts. */
public final class PotionTank {
    public static final int CAPACITY = 16;
    private PotionContents contents = PotionContents.EMPTY;
    private int batches;

    public static boolean isPotion(ItemStack stack) {
        return stack.is(Items.POTION) || stack.is(Items.SPLASH_POTION) || stack.is(Items.LINGERING_POTION);
    }

    public boolean fill(ItemStack bottle) {
        if (!isPotion(bottle) || batches > CAPACITY - 2) return false;
        PotionContents incoming = bottle.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY);
        if (batches > 0 && !contents.equals(incoming)) return false;
        contents = incoming;
        batches += 2;
        return true;
    }

    public void consume() {
        if (batches == 0) return;
        if (--batches == 0) contents = PotionContents.EMPTY;
    }

    public int batches() {
        return batches;
    }

    public PotionContents contents() {
        return contents;
    }

    public ItemStack displayStack() {
        if (batches == 0) return ItemStack.EMPTY;
        ItemStack stack = new ItemStack(Items.POTION);
        stack.set(DataComponents.POTION_CONTENTS, contents);
        return stack;
    }

    public CompoundTag save(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        tag.putInt("Batches", batches);
        if (batches > 0) tag.put("Potion", displayStack().save(registries));
        return tag;
    }

    public static PotionTank load(CompoundTag tag, HolderLookup.Provider registries) {
        PotionTank tank = new PotionTank();
        ItemStack bottle = ItemStack.parseOptional(registries, tag.getCompound("Potion"));
        if (isPotion(bottle)) {
            tank.batches = Math.clamp(tag.getInt("Batches"), 0, CAPACITY);
            if (tank.batches > 0)
                tank.contents = bottle.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY);
        }
        return tank;
    }
}
