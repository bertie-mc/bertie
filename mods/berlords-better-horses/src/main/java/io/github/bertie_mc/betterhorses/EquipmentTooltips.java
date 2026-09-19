package io.github.bertie_mc.betterhorses;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

public final class EquipmentTooltips {
    private EquipmentTooltips() {}

    public static void begin(List<Component> lines) {
        lines.add(Component.empty());
        lines.add(Component.translatable("tooltip.betterhorses.equipped").withStyle(ChatFormatting.GRAY));
    }

    public static void stat(List<Component> lines, String name, Object... values) {
        lines.add(Component.translatable("tooltip.betterhorses." + name, values).withStyle(ChatFormatting.BLUE));
    }

    public static void ability(List<Component> lines, String name) {
        lines.add(Component.translatable("tooltip.betterhorses." + name).withStyle(ChatFormatting.GOLD));
    }
}
