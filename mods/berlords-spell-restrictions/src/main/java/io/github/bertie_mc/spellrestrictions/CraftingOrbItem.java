package io.github.bertie_mc.spellrestrictions;

import io.redspace.ironsspellbooks.api.spells.SpellRarity;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

public final class CraftingOrbItem extends Item {
    private final int tier;

    public CraftingOrbItem(int tier) {
        super(new Properties());
        this.tier = tier;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        var stack = player.getItemInHand(hand);
        if (player instanceof ServerPlayer server) {
            if (!Restrictions.state(player).unlockTier(tier)) {
                player.sendSystemMessage(
                        Component.translatable("message.berlordsspellrestrictions.tier_already_known"));
                return InteractionResultHolder.fail(stack);
            }
            if (!player.getAbilities().instabuild) stack.shrink(1);
            ProgressPacket.send(server);
            player.sendSystemMessage(Component.translatable(
                    "message.berlordsspellrestrictions.tier_unlocked", SpellRarity.values()[tier].getDisplayName()));
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flags) {
        lines.add(Component.translatable(
                        "tooltip.berlordsspellrestrictions.orb", SpellRarity.values()[tier].getDisplayName())
                .withStyle(ChatFormatting.GRAY));
    }
}
