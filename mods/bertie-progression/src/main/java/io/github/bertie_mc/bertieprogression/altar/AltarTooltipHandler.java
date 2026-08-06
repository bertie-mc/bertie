package io.github.bertie_mc.bertieprogression.altar;

import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

/**
 * Tells the player what the altar now wants, since none of it is discoverable in game.
 *
 * <p>Added as a tooltip rather than as a lang override: Cataclysm does not read a {@code .desc} key
 * for this block, so there is no stock line to override.
 */
public final class AltarTooltipHandler {

    private AltarTooltipHandler() {}

    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent event) {
        if (!BuiltInRegistries.ITEM.getKey(event.getItemStack().getItem()).equals(AltarOfAmethystRules.ALTAR_ITEM)) {
            return;
        }
        event.getToolTip()
                .add(Component.translatable("tooltip.bertieprogression.altar_of_amethyst")
                        .withStyle(ChatFormatting.GRAY));
    }
}
