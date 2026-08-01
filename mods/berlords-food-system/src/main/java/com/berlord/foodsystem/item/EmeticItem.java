package com.berlord.foodsystem.item;

import com.berlord.foodsystem.stomach.Stomach;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
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
 * Drink to empty your stomach: clears food slots (and with them all food buffs),
 * dropping you back to base health, plus a short bout of nausea.
 * The regular Emetic spares eternal foods; the Demonic Gruel clears those too.
 */
public class EmeticItem extends Item {

    private final boolean clearEternal;
    private final String tooltipKey;

    public EmeticItem(Properties properties, boolean clearEternal, String tooltipKey) {
        super(properties);
        this.clearEternal = clearEternal;
        this.tooltipKey = tooltipKey;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
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
            Stomach.reset(player, clearEternal);
            player.setHealth(Math.min(player.getHealth(), player.getMaxHealth()));
            if (clearEternal) {
                // demonic: purging eternal foods exacts a heavier toll (15s each)
                player.addEffect(new MobEffectInstance(MobEffects.POISON, 300, 1, false, true, true));
                player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 300, 4, false, true, true));
                player.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 300, 0, false, true, true));
                player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 300, 0, false, true, true));
                player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 300, 0, false, true, true));
            } else {
                player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 200, 0, false, true, true));
            }
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.PLAYER_BURP, SoundSource.PLAYERS, 1.0F, 0.6F);

            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
                ItemStack bowl = new ItemStack(Items.BOWL);
                if (stack.isEmpty()) {
                    return bowl;
                }
                if (!player.getInventory().add(bowl)) {
                    player.drop(bowl, false);
                }
            }
        }
        return stack;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable(tooltipKey));
    }
}
