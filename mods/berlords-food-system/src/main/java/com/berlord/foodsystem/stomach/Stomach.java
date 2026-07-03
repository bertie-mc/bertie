package com.berlord.foodsystem.stomach;

import com.berlord.foodsystem.BFS;
import com.berlord.foodsystem.Config;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * The stomach API: slot queries, food stats, insertion, reset, health reconciliation, sync.
 * Mechanics follow Spice of Life: Valheim Reforged 1.1.4 (used with permission), generalized
 * from 3 hardcoded slots to 1-5 dynamic slots.
 */
public final class Stomach {

    public static final TagKey<Item> NOT_FOOD = ItemTags.create(rl("not_food"));
    public static final TagKey<Item> ALWAYS_EDIBLE = ItemTags.create(rl("always_edible"));
    public static final TagKey<Item> LOW_NUTRIENTS = ItemTags.create(rl("low_nutrients"));
    public static final TagKey<Item> HIGH_NUTRIENTS = ItemTags.create(rl("high_nutrients"));
    public static final TagKey<Item> SLICEABLE = ItemTags.create(rl("sliceable_food"));
    public static final TagKey<Block> SLICEABLE_BLOCK = BlockTags.create(rl("sliceable_food"));

    public static final ResourceLocation FOOD_HEALTH_MODIFIER = rl("food_health");

    private static ResourceLocation rl(String path) {
        return ResourceLocation.fromNamespaceAndPath(BFS.MODID, path);
    }

    public static StomachData get(Player player) {
        return player.getData(BFS.STOMACH);
    }

    /** unlocked slot count resolved against config (default..max, potions can raise it) */
    public static int unlockedSlots(Player player) {
        StomachData data = get(player);
        int def = Config.defaultSlots();
        int max = Config.maxSlots();
        int stored = data.unlockedSlots;
        return Math.clamp(stored <= 0 ? def : stored, def, max);
    }

    /** grows the stomach by one slot; returns false when already at the configured max */
    public static boolean addSlot(Player player) {
        int current = unlockedSlots(player);
        if (current >= Config.maxSlots()) return false;
        StomachData data = get(player);
        data.unlockedSlots = current + 1;
        data.feedWakeStamp++; // new empty slot: wake the SB feeder
        data.markDirty();
        return true;
    }

    public static boolean isFoodForSystem(ItemStack stack) {
        return stack.has(DataComponents.FOOD) && !stack.is(NOT_FOOD);
    }

    public static boolean isEternal(ItemStack stack) {
        return stack.has(BFS.ETERNAL.get());
    }

    /** slot index holding this item (eternal and regular count as the same food), or -1 */
    public static int findSlotWith(Player player, ItemStack stack) {
        Item item = stack.getItem();
        StomachData data = get(player);
        int unlocked = unlockedSlots(player);
        for (int i = 0; i < unlocked; i++) {
            ItemStack slotted = data.slots[i].stack;
            if (!slotted.isEmpty() && slotted.getItem() == item) return i;
        }
        return -1;
    }

    /** true when every unlocked slot is empty (no food at all, eternal foods included) */
    public static boolean isEmpty(Player player) {
        StomachData data = get(player);
        int unlocked = unlockedSlots(player);
        for (int i = 0; i < unlocked; i++) {
            if (!data.slots[i].isEmpty()) return false;
        }
        return true;
    }

    public static int findEmptySlot(Player player) {
        StomachData data = get(player);
        int unlocked = unlockedSlots(player);
        for (int i = 0; i < unlocked; i++) {
            if (data.slots[i].isEmpty()) return i;
        }
        return -1;
    }

    /** unlocked, replaceable (non-eternal) slot with the least remaining time, or -1 */
    public static int findOldestSlot(Player player) {
        StomachData data = get(player);
        int unlocked = unlockedSlots(player);
        int oldest = -1;
        for (int i = 0; i < unlocked; i++) {
            if (Double.isInfinite(data.slots[i].duration)) continue; // eternal foods are never replaced
            if (oldest < 0 || data.slots[i].duration < data.slots[oldest].duration) oldest = i;
        }
        return oldest;
    }

    /** Reforged food math for a food item: instant health, regen factor, and slot duration in ticks. */
    public record FoodStats(double healthBoost, double regenFactor, double duration) {}

    public static FoodStats foodStats(ItemStack stack) {
        if (stack.is(LOW_NUTRIENTS)) {
            return new FoodStats(1.0, 5.0, 6000.0);
        }
        if (stack.is(HIGH_NUTRIENTS)) {
            return new FoodStats(14.0, 70.0, 72000.0);
        }
        var food = stack.getFoodProperties(null);
        double nutrition = food != null ? food.nutrition() : 0.0;
        double saturation = food != null ? food.saturation() : 0.0;
        double perSaturation = Math.clamp(Config.FOOD_DURATION.get(), 60.0, 600.0) / 120.0;
        double factor = stack.getMaxStackSize() <= 16 && Config.BONUS_DURATION.get() ? 1440.0 : 1200.0;
        // 5 min floor, no upper cap (the old 72000-tick / 60-min ceiling was removed)
        double duration = Math.max(saturation * perSaturation * factor, 6000.0);
        return new FoodStats(nutrition, nutrition * 5.0, duration);
    }

    /** writes a food into a slot and grants its instant health */
    public static void fillSlot(Player player, int index, ItemStack stack) {
        FoodStats stats = foodStats(stack);
        StomachData data = get(player);
        StomachData.FoodSlot slot = data.slots[index];
        slot.stack = stack.copyWithCount(1);
        slot.healthBoost = stats.healthBoost();
        slot.regenFactor = stats.regenFactor();
        slot.duration = isEternal(stack) ? Double.POSITIVE_INFINITY : stats.duration();
        slot.eatOrder = ++data.eatCounter;
        data.markDirty();
        updateHealth(player);
        player.setHealth(player.getHealth() + (float) stats.healthBoost());
    }

    public static void clearSlot(Player player, int index) {
        StomachData data = get(player);
        data.slots[index].clear();
        data.feedWakeStamp++; // slot freed: wake the SB feeder
        data.markDirty();
        if (!player.level().isClientSide()) {
            updateHealth(player);
            clampHealth(player);
        }
    }

    /**
     * Empties the stomach and resets regen state; unlocked slot count is kept.
     * Eternal foods survive unless {@code includeEternal} (Demonic Gruel, not Emetic/death).
     */
    public static void reset(Player player, boolean includeEternal) {
        StomachData data = get(player);
        for (StomachData.FoodSlot slot : data.slots) {
            if (!includeEternal && !slot.isEmpty() && isEternal(slot.stack)) continue;
            slot.clear();
        }
        data.regenCooldown = 0.0;
        data.feedWakeStamp++; // slots emptied (gruel/emetic): wake the SB feeder
        data.markDirty();
        if (!player.level().isClientSide()) {
            updateHealth(player);
            clampHealth(player);
        }
    }

    /** ticks-per-heal for passive regen: base minus food regen, min 10 */
    public static double regenFactor(Player player) {
        StomachData data = get(player);
        double sum = 0;
        int unlocked = unlockedSlots(player);
        for (int i = 0; i < unlocked; i++) {
            if (data.slots[i].isActive()) sum += data.slots[i].regenFactor;
        }
        sum += com.berlord.foodsystem.buffs.SynergyEngine.varietyRegenBonus(player); // food-variety synergy
        return Math.max(Math.clamp(Config.BASE_REGEN_RATE.get(), 20.0, 600.0) - sum, 10.0);
    }

    /** reconciles the max-health attribute modifier with the current stomach contents */
    public static void updateHealth(Player player) {
        if (player.level().isClientSide()) return;
        AttributeInstance maxHealth = player.getAttribute(Attributes.MAX_HEALTH);
        if (maxHealth == null) return;

        double boostSum = 0;
        StomachData data = get(player);
        int unlocked = unlockedSlots(player);
        for (int i = 0; i < unlocked; i++) {
            if (data.slots[i].isActive()) boostSum += data.slots[i].healthBoost;
        }
        double cap = Math.clamp(Config.TOTAL_HEALTH.get(), 20.0, 800.0) - maxHealth.getBaseValue();
        double amount = Math.min(cap, boostSum);

        maxHealth.removeModifier(FOOD_HEALTH_MODIFIER);
        if (amount > 0) {
            maxHealth.addTransientModifier(
                    new AttributeModifier(FOOD_HEALTH_MODIFIER, amount, AttributeModifier.Operation.ADD_VALUE));
        }
    }

    /** unlike Reforged (which healed to full on slot loss) we only clamp overflow */
    public static void clampHealth(Player player) {
        if (player.getHealth() > player.getMaxHealth()) {
            player.setHealth(player.getMaxHealth());
        }
    }

    /** absorption afterglow lifetime per granted portion: 30s */
    public static final int AFTERGLOW_TICKS = 600;

    /**
     * Health cut off by a natural slot expiry converts to absorption (yellow hearts).
     * Each cut is its own ledger entry that expires EXACTLY 30s after it was granted —
     * staggered expiries can NOT refresh older portions (one shared vanilla timer would
     * allow chaining ~minutes of max absorption from deliberately offset foods).
     * The vanilla Absorption effect is only the display/cap layer: raw
     * setAbsorptionAmount is clamped by MAX_ABSORPTION, which only the effect raises.
     * Absorption from other sources present at grant time is folded in as one entry.
     */
    public static void grantAfterglow(Player player, float overflow) {
        StomachData data = get(player);
        long now = player.level().getGameTime();
        double external = player.getAbsorptionAmount() - afterglowSum(data);
        if (external > 0.01) {
            data.afterglow.add(new double[]{external, now + AFTERGLOW_TICKS});
        }
        data.afterglow.add(new double[]{overflow, now + AFTERGLOW_TICKS});
        applyAfterglow(player, data, now);
    }

    public static double afterglowSum(StomachData data) {
        double sum = 0;
        for (double[] entry : data.afterglow) sum += entry[0];
        return sum;
    }

    private static void applyAfterglow(Player player, StomachData data, long now) {
        double total = afterglowSum(data);
        long latest = now;
        for (double[] entry : data.afterglow) latest = Math.max(latest, (long) entry[1]);
        int amplifier = Math.min((int) Math.ceil(total / 4.0) - 1, 49);
        player.removeEffect(net.minecraft.world.effect.MobEffects.ABSORPTION);
        // non-ambient so the buff engine's stale-effect sweep never touches it;
        // duration = until the LAST entry lapses, amounts are managed by the ledger tick
        player.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                net.minecraft.world.effect.MobEffects.ABSORPTION, (int) (latest - now), amplifier, false, false, true));
        player.setAbsorptionAmount((float) total);
    }

    /** per-tick ledger upkeep: damage consumes oldest entries; expired entries deflate the pool */
    public static void tickAfterglow(Player player, StomachData data) {
        if (data.afterglow.isEmpty()) return;
        long now = player.level().getGameTime();
        double actual = player.getAbsorptionAmount();

        if (actual <= 0.01) {
            data.afterglow.clear(); // hearts eaten through; vanilla removes the dead effect itself
            return;
        }
        // damage since last tick: charge it to the oldest entries (they'd lapse first anyway)
        double expected = afterglowSum(data);
        double consumed = expected - actual;
        while (consumed > 0.01 && !data.afterglow.isEmpty()) {
            double[] oldest = data.afterglow.get(0);
            double take = Math.min(oldest[0], consumed);
            oldest[0] -= take;
            consumed -= take;
            if (oldest[0] <= 0.01) data.afterglow.remove(0);
        }
        // lapse expired entries
        boolean lapsed = data.afterglow.removeIf(entry -> entry[1] <= now);
        if (lapsed) {
            double total = afterglowSum(data);
            if (total <= 0.01) {
                data.afterglow.clear();
                player.removeEffect(net.minecraft.world.effect.MobEffects.ABSORPTION);
                player.setAbsorptionAmount(0.0F);
            } else {
                player.setAbsorptionAmount((float) total);
            }
        }
    }

    public static void sync(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player,
                new StomachSyncPayload(get(player).serializeNBT(player.registryAccess())));
    }

    private Stomach() {}
}
