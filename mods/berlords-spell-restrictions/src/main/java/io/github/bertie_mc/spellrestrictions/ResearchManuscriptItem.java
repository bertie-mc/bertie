package io.github.bertie_mc.spellrestrictions;

import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

public final class ResearchManuscriptItem extends Item {
    private final ResourceLocation school;

    public ResearchManuscriptItem(ResourceLocation school) {
        super(new Properties());
        this.school = school;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        var stack = player.getItemInHand(hand);
        if (player instanceof ServerPlayer server && !Restrictions.research(server, stack, school))
            return InteractionResultHolder.fail(stack);
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    /** The name takes the colour of the school it researches, which is absent when its addon is. */
    @Override
    public Component getName(ItemStack stack) {
        var type = SchoolRegistry.getSchool(school);
        if (type == null) return super.getName(stack);
        var color = type.getDisplayName().getStyle().getColor();
        return super.getName(stack).copy().withStyle(style -> style.withColor(color));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flags) {
        lines.add(Component.translatable(getDescriptionId() + "_desc").withStyle(ChatFormatting.GRAY));
    }
}
