package io.github.bertie_mc.betterhorses;

import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;

public final class HorseshoeItem extends Item {
    public final ShoeTier tier;

    public HorseshoeItem(ShoeTier tier) {
        super(tier == ShoeTier.NETHERITE ? new Properties().stacksTo(1).fireResistant() : new Properties().stacksTo(1));
        this.tier = tier;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag) {
        lines.add(Component.translatable("tooltip.betterhorses.speed", tier.speed));
        lines.add(Component.translatable("tooltip.betterhorses.fall", tier.safeFall));
        if (tier.jumpHeight > 0) lines.add(Component.translatable("tooltip.betterhorses.jump", tier.jumpHeight));
        if (tier.stepHeight > 0) lines.add(Component.translatable("tooltip.betterhorses.step", tier.stepHeight));
        if (tier.waterWalking()) lines.add(Component.translatable("tooltip.betterhorses.water"));
        if (tier.lavaWalking()) lines.add(Component.translatable("tooltip.betterhorses.lava"));
    }
}
