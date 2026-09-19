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
        EquipmentTooltips.begin(lines);
        EquipmentTooltips.stat(lines, "speed", tier.speed);
        EquipmentTooltips.stat(lines, "fall", tier.safeFall);
        if (tier.jumpHeight > 0) EquipmentTooltips.stat(lines, "jump", tier.jumpHeight);
        if (tier.stepHeight > 0) EquipmentTooltips.stat(lines, "step", tier.stepHeight);
        if (tier.waterWalking()) EquipmentTooltips.ability(lines, "water");
        if (tier.lavaWalking()) EquipmentTooltips.ability(lines, "lava");
    }
}
