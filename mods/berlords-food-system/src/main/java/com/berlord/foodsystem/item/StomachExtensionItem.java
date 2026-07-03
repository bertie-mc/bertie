package com.berlord.foodsystem.item;

import com.berlord.foodsystem.Config;
import com.berlord.foodsystem.stomach.Stomach;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Potion that permanently grows the stomach by one slot, up to the configured maximum.
 * At the maximum it cannot be drunk at all — the item is never wasted.
 */
public class StomachExtensionItem extends Item {

    public StomachExtensionItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (Stomach.unlockedSlots(player) >= Config.maxSlots()) {
            if (!level.isClientSide()) {
                player.displayClientMessage(
                        Component.translatable("item.berlords_food_system.stomach_extension.at_max"), true);
            }
            return InteractionResultHolder.fail(player.getItemInHand(hand));
        }
        return ItemUtils.startUsingInstantly(level, player, hand);
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 32;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.DRINK;
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (!level.isClientSide() && entity instanceof Player player) {
            if (Stomach.addSlot(player)) {
                level.playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.8F, 1.2F);
                player.displayClientMessage(Component.translatable(
                        "item.berlords_food_system.stomach_extension.grown",
                        Stomach.unlockedSlots(player)), true);
                if (player instanceof ServerPlayer serverPlayer) {
                    Stomach.sync(serverPlayer);
                }
                if (!player.getAbilities().instabuild) {
                    stack.shrink(1);
                    ItemStack bottle = new ItemStack(Items.GLASS_BOTTLE);
                    if (stack.isEmpty()) {
                        return bottle;
                    }
                    if (!player.getInventory().add(bottle)) {
                        player.drop(bottle, false);
                    }
                }
            }
        }
        return stack;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.berlords_food_system.stomach_extension.tooltip", Config.maxSlots()));
    }
}
