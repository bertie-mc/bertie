package com.berlord.foodsystem.stomach;

import com.berlord.foodsystem.BFS;
import com.berlord.foodsystem.Config;
import com.berlord.foodsystem.compat.FeedingContext;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;

/**
 * Owns the eat gate and slot insertion. Slotted foods can be re-eaten at ANY time to
 * refresh their timer — accidental waste is prevented by a short vanilla item cooldown
 * (gray sweep overlay) applied after each eat instead of a hard refresh window.
 */
@EventBusSubscriber(modid = BFS.MODID)
public final class EatingHandler {

    /** can this stack currently go into (or refresh) a stomach slot? */
    public static boolean canEat(Player player, ItemStack stack) {
        if (!Stomach.isFoodForSystem(stack)) return true; // not ours to police
        if (stack.is(Stomach.ALWAYS_EDIBLE)) return true;
        int sameSlot = Stomach.findSlotWith(player, stack);
        if (sameSlot >= 0) {
            // an eternal slot never needs (or accepts) a refresh — eating the same food is
            // pointless waste, so it's blocked; a regular slot refreshes anytime (and an
            // eternal copy of the food may upgrade it)
            return !Stomach.isEternal(Stomach.get(player).slots[sameSlot].stack);
        }
        if (Stomach.findEmptySlot(player) >= 0) return true;
        // replacement needs a non-eternal slot to evict
        return Config.REPLACE_OLDEST.get() && Stomach.findOldestSlot(player) >= 0;
    }

    @SubscribeEvent
    public static void onUseItemStart(LivingEntityUseItemEvent.Start event) {
        if (event.getEntity() instanceof Player player && !canEat(player, event.getItem())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onUseItemFinish(LivingEntityUseItemEvent.Finish event) {
        if (!(event.getEntity() instanceof Player player)) return;
        ItemStack stack = event.getItem();
        if (!Stomach.isFoodForSystem(stack)) return;

        Item item = stack.getItem();
        int target = Stomach.findSlotWith(player, stack);
        if (target >= 0 && Stomach.isEternal(Stomach.get(player).slots[target].stack)) {
            return; // eternal slots are immutable (gate blocks this path; guards SB & modded eats)
        }
        if (target < 0) target = Stomach.findEmptySlot(player);
        if (target < 0 && Config.REPLACE_OLDEST.get() && !FeedingContext.isActive()) {
            target = Stomach.findOldestSlot(player); // -1 when only eternal slots remain
        }
        if (target >= 0) {
            Stomach.fillSlot(player, target, stack);
        }

        if (!player.level().isClientSide()) {
            int cooldown = Config.EAT_COOLDOWN_TICKS.get();
            if (cooldown > 0 && !stack.is(Stomach.ALWAYS_EDIBLE)) {
                player.getCooldowns().addCooldown(item, cooldown);
            }
            if (!Config.ALLOW_SATURATION.get()) {
                stripFoodEffects(player);
            }
        }
    }

    /** Reforged cleared these via commands; we use the API (effects looked up lazily, mods optional) */
    private static void stripFoodEffects(Player player) {
        removeIfPresent(player, "farmersdelight:nourishment");
        removeIfPresent(player, "farmersdelight:comfort");
        removeIfPresent(player, "minecraft:saturation");
    }

    private static void removeIfPresent(Player player, String id) {
        BuiltInRegistries.MOB_EFFECT.getHolder(ResourceLocation.parse(id))
                .ifPresent(player::removeEffect);
    }
}
