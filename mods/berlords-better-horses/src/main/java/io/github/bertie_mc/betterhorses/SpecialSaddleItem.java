package io.github.bertie_mc.betterhorses;

import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.world.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.horse.Horse;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;

public final class SpecialSaddleItem extends SaddleItem {
    private final String kind;

    public SpecialSaddleItem(String kind) {
        super(new Properties().stacksTo(1));
        this.kind = kind;
    }

    @Override
    public InteractionResult interactLivingEntity(
            ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        return target instanceof Horse
                ? super.interactLivingEntity(stack, player, target, hand)
                : InteractionResult.PASS;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag) {
        if (kind.equals("warrior"))
            lines.add(Component.translatable(
                    "tooltip.betterhorses.warrior", Math.round(BetterHorses.DAMAGE_TRANSFER.get() * 100)));
        else lines.add(Component.translatable("tooltip.betterhorses." + kind));
    }
}
