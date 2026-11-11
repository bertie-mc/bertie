package io.github.bertie_mc.bertieprogression.pastel;

import earth.terrarium.pastel.api.energy.InkPowered;
import java.util.stream.Collectors;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.DyeItem;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

public final class PastelFuelTooltip {
    private PastelFuelTooltip() {}

    @SubscribeEvent
    public static void tooltip(ItemTooltipEvent event) {
        if (!(event.getItemStack().getItem() instanceof InkPowered powered)) return;
        String dyes = powered.getUsedColors().stream()
                .flatMap(color -> color.getDyeColor().stream())
                .map(color -> DyeItem.byColor(color).getDescription().getString())
                .collect(Collectors.joining(", "));
        if (!dyes.isEmpty()) {
            event.getToolTip()
                    .add(Component.translatable("tooltip.bertieprogression.pastel_dye_fuel", dyes)
                            .withStyle(ChatFormatting.GRAY));
        }
    }
}
