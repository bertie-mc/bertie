package com.berlord.foodsystem.item;

import com.berlord.foodsystem.BFS;
import com.berlord.foodsystem.stomach.Stomach;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;

/**
 * Lunchbox: a non-stackable food container for the stomach system.
 * <ul>
 *   <li><b>Use</b> (right-click): dispense from the box into every empty stomach slot
 *       (one food per slot, skipping foods already in the stomach to keep variety).</li>
 *   <li><b>Sneak + use</b>: pack foods from your inventory into the box — one of each
 *       distinct food first (so a single pack covers a varied meal), then tops up.</li>
 * </ul>
 * Contents live in the {@link BFS#LUNCHBOX_CONTENTS} component (saved + synced), so they
 * survive drops/death with no loss and show on the tooltip.
 */
public class LunchboxItem extends Item {

    /** maximum number of distinct food stacks the box holds */
    public static final int CAPACITY = 8;

    public LunchboxItem(Properties properties) {
        super(properties);
    }

    private static List<ItemStack> read(ItemStack box) {
        List<ItemStack> stored = box.getOrDefault(BFS.LUNCHBOX_CONTENTS.get(), List.of());
        List<ItemStack> out = new ArrayList<>(stored.size());
        for (ItemStack s : stored) {
            if (!s.isEmpty()) out.add(s.copy());
        }
        return out;
    }

    private static void write(ItemStack box, List<ItemStack> contents) {
        contents.removeIf(ItemStack::isEmpty);
        box.set(BFS.LUNCHBOX_CONTENTS.get(), List.copyOf(contents));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack box = player.getItemInHand(hand);
        if (level.isClientSide()) {
            return InteractionResultHolder.success(box);
        }
        if (player.isSecondaryUseActive()) {
            int packed = pack(box, player);
            if (packed > 0) {
                level.playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.7F, 1.0F);
                player.displayClientMessage(
                        Component.translatable("item.berlords_food_system.lunchbox.packed", packed), true);
            } else {
                player.displayClientMessage(
                        Component.translatable("item.berlords_food_system.lunchbox.nothing_to_pack"), true);
            }
        } else {
            int eaten = dispense(box, player);
            if (eaten > 0) {
                level.playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.GENERIC_EAT, SoundSource.PLAYERS, 0.8F, 1.0F);
                player.displayClientMessage(
                        Component.translatable("item.berlords_food_system.lunchbox.ate", eaten), true);
            } else {
                player.displayClientMessage(
                        Component.translatable("item.berlords_food_system.lunchbox.cannot_eat"), true);
            }
        }
        return InteractionResultHolder.success(box);
    }

    /** fills empty stomach slots from the box; returns how many foods were eaten */
    private static int dispense(ItemStack box, Player player) {
        List<ItemStack> contents = read(box);
        if (contents.isEmpty()) return 0;
        int eaten = 0;
        while (true) {
            int slot = Stomach.findEmptySlot(player);
            if (slot < 0) break;
            ItemStack pick = null;
            for (ItemStack c : contents) {
                if (c.isEmpty() || !Stomach.isFoodForSystem(c)) continue;
                if (Stomach.findSlotWith(player, c) >= 0) continue; // already in the stomach
                pick = c;
                break;
            }
            if (pick == null) break;
            Stomach.fillSlot(player, slot, pick); // grants health; BuffEngine applies buffs next tick
            pick.shrink(1);
            eaten++;
        }
        if (eaten > 0) write(box, contents);
        return eaten;
    }

    /** moves foods from the player's inventory into the box; returns how many were packed */
    private static int pack(ItemStack box, Player player) {
        List<ItemStack> contents = read(box);
        Inventory inv = player.getInventory();
        int packed = 0;

        // pass 1: one of each distinct food not yet represented (variety first)
        for (int i = 0; i < inv.getContainerSize() && contents.size() < CAPACITY; i++) {
            ItemStack s = inv.getItem(i);
            if (s == box || !Stomach.isFoodForSystem(s)) continue;
            if (containsItem(contents, s)) continue;
            contents.add(inv.removeItem(i, 1));
            packed++;
        }
        // pass 2: top up the stacks already in the box
        for (ItemStack c : contents) {
            int space = c.getMaxStackSize() - c.getCount();
            for (int i = 0; i < inv.getContainerSize() && space > 0; i++) {
                ItemStack s = inv.getItem(i);
                if (s == box || s.isEmpty() || !ItemStack.isSameItemSameComponents(c, s)) continue;
                ItemStack taken = inv.removeItem(i, Math.min(space, s.getCount()));
                int n = taken.getCount();
                c.grow(n);
                space -= n;
                packed += n;
            }
        }

        if (packed > 0) write(box, contents);
        return packed;
    }

    private static boolean containsItem(List<ItemStack> contents, ItemStack stack) {
        for (ItemStack c : contents) {
            if (ItemStack.isSameItemSameComponents(c, stack)) return true;
        }
        return false;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        List<ItemStack> contents = read(stack);
        if (contents.isEmpty()) {
            tooltip.add(Component.translatable("item.berlords_food_system.lunchbox.empty")
                    .withStyle(ChatFormatting.GRAY));
        } else {
            int total = 0;
            for (ItemStack c : contents) total += c.getCount();
            tooltip.add(Component.translatable("item.berlords_food_system.lunchbox.contents", total)
                    .withStyle(ChatFormatting.GRAY));
            for (ItemStack c : contents) {
                tooltip.add(Component.translatable("item.berlords_food_system.lunchbox.entry",
                        c.getCount(), c.getHoverName()).withStyle(ChatFormatting.DARK_GRAY));
            }
        }
        tooltip.add(Component.translatable("item.berlords_food_system.lunchbox.usage")
                .withStyle(ChatFormatting.DARK_GRAY));
    }
}
