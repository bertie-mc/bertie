package com.berlord.foodsystem.stomach;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.util.INBTSerializable;

/**
 * Per-player stomach: up to {@link #MAX_SLOTS} food slots plus regen bookkeeping.
 * Attached on both sides; the server pushes full snapshots to the owning client.
 */
public class StomachData implements INBTSerializable<CompoundTag> {

    public static final int MAX_SLOTS = 5;

    // NBT keys — shared by serialize/deserialize so a typo can't silently drop a field
    private static final String KEY_SLOTS = "slots";
    private static final String KEY_STACK = "stack";
    private static final String KEY_DURATION = "duration";
    private static final String KEY_HEALTH_BOOST = "health_boost";
    private static final String KEY_REGEN_FACTOR = "regen_factor";
    private static final String KEY_EAT_ORDER = "eat_order";
    private static final String KEY_UNLOCKED_SLOTS = "unlocked_slots";
    private static final String KEY_REGEN_COOLDOWN = "regen_cooldown";
    private static final String KEY_EAT_COUNTER = "eat_counter";

    public static class FoodSlot {
        public ItemStack stack = ItemStack.EMPTY;
        public double duration = 0.0;
        public double healthBoost = 0.0;
        public double regenFactor = 0.0;
        /** consumption sequence number; orders eternal foods left-to-right on the HUD */
        public long eatOrder = 0;

        public boolean isEmpty() {
            return stack.isEmpty();
        }

        public boolean isActive() {
            return !stack.isEmpty() && duration >= 2.0;
        }

        public void clear() {
            stack = ItemStack.EMPTY;
            duration = 0.0;
            healthBoost = 0.0;
            regenFactor = 0.0;
            eatOrder = 0;
        }
    }

    public final FoodSlot[] slots = new FoodSlot[MAX_SLOTS];
    /** 0 = "unset", resolved against config defaults via Stomach.unlockedSlots() */
    public int unlockedSlots = 0;
    public double regenCooldown = 0.0;
    public long regenCounter = 0;
    public long eatCounter = 0;
    public boolean syncDirty = false;
    /**
     * Bumped whenever a stomach change OPENS a new auto-feed opportunity (a slot freed by
     * expiry/gruel/emetic, or a slot added by Stomach Extension). The SB feeding wake polls
     * this to force an immediate rescan. Runtime-only, server-authoritative: not saved, not
     * synced, not copied on respawn (a reset to 0 is harmless — it only ever triggers one
     * extra scan against a fresh wrapper).
     */
    public long feedWakeStamp = 0;
    /**
     * Afterglow ledger: each entry is {absorption amount, gameTime it expires}. Each
     * slot-expiry's converted hearts live EXACTLY 30s — pooling them into one vanilla
     * absorption timer would let staggered expiries keep refreshing older portions.
     * Transient (server-side, combat-scale lifetime; not saved or synced).
     */
    public final java.util.List<double[]> afterglow = new java.util.ArrayList<>();

    public StomachData() {
        for (int i = 0; i < MAX_SLOTS; i++) {
            slots[i] = new FoodSlot();
        }
    }

    public void markDirty() {
        syncDirty = true;
    }

    public void copyFrom(StomachData other) {
        for (int i = 0; i < MAX_SLOTS; i++) {
            slots[i].stack = other.slots[i].stack.copy();
            slots[i].duration = other.slots[i].duration;
            slots[i].healthBoost = other.slots[i].healthBoost;
            slots[i].regenFactor = other.slots[i].regenFactor;
            slots[i].eatOrder = other.slots[i].eatOrder;
        }
        unlockedSlots = other.unlockedSlots;
        regenCooldown = other.regenCooldown;
        eatCounter = other.eatCounter;
        // regenCounter, syncDirty, feedWakeStamp and afterglow are transient/server-runtime and intentionally not copied
    }

    @Override
    public CompoundTag serializeNBT(HolderLookup.Provider provider) {
        CompoundTag nbt = new CompoundTag();
        ListTag list = new ListTag();
        for (FoodSlot slot : slots) {
            CompoundTag s = new CompoundTag();
            s.put(KEY_STACK, slot.stack.saveOptional(provider));
            s.putDouble(KEY_DURATION, slot.duration);
            s.putDouble(KEY_HEALTH_BOOST, slot.healthBoost);
            s.putDouble(KEY_REGEN_FACTOR, slot.regenFactor);
            s.putLong(KEY_EAT_ORDER, slot.eatOrder);
            list.add(s);
        }
        nbt.put(KEY_SLOTS, list);
        nbt.putInt(KEY_UNLOCKED_SLOTS, unlockedSlots);
        nbt.putDouble(KEY_REGEN_COOLDOWN, regenCooldown);
        nbt.putLong(KEY_EAT_COUNTER, eatCounter);
        return nbt;
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider provider, CompoundTag nbt) {
        ListTag list = nbt.getList(KEY_SLOTS, CompoundTag.TAG_COMPOUND);
        for (int i = 0; i < MAX_SLOTS; i++) {
            FoodSlot slot = slots[i];
            if (i < list.size()) {
                CompoundTag s = list.getCompound(i);
                slot.stack = ItemStack.parseOptional(provider, s.getCompound(KEY_STACK));
                slot.duration = s.getDouble(KEY_DURATION);
                slot.healthBoost = s.getDouble(KEY_HEALTH_BOOST);
                slot.regenFactor = s.getDouble(KEY_REGEN_FACTOR);
                slot.eatOrder = s.getLong(KEY_EAT_ORDER);
            } else {
                slot.clear();
            }
        }
        unlockedSlots = nbt.getInt(KEY_UNLOCKED_SLOTS);
        regenCooldown = nbt.getDouble(KEY_REGEN_COOLDOWN);
        eatCounter = nbt.getLong(KEY_EAT_COUNTER);
    }
}
