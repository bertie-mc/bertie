package io.github.bertie_mc.betterhorses.client;

import com.mojang.blaze3d.platform.InputConstants;
import io.github.bertie_mc.betterhorses.*;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.*;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(modid = BetterHorses.ID, value = Dist.CLIENT)
public final class HorseTooltipHandler {
    private HorseTooltipHandler() {}

    @SubscribeEvent
    public static void tooltip(ItemTooltipEvent event) {
        if (!(event.getItemStack().getItem() instanceof HorseshoeItem)
                && !event.getItemStack().is(BetterHorses.WARRIOR.get())) return;
        Minecraft client = Minecraft.getInstance();
        boolean expanded = crouchHeld(client);
        List<Component> lines = event.getToolTip();
        int hintIndex = -1;
        for (int i = lines.size() - 1; i >= 0; i--) {
            if (!(lines.get(i).getContents() instanceof TranslatableContents text)) continue;
            Component detail =
                    switch (text.getKey()) {
                        case "tooltip.betterhorses.water" ->
                            Component.translatable("tooltip.betterhorses.water_detail");
                        case "tooltip.betterhorses.lava" -> Component.translatable("tooltip.betterhorses.lava_detail");
                        case "tooltip.betterhorses.warrior" ->
                            Component.translatable(
                                    "tooltip.betterhorses.warrior_detail",
                                    Math.round(BetterHorses.DAMAGE_TRANSFER.get() * 100));
                        default -> null;
                    };
            if (detail == null) continue;
            if (hintIndex < 0) hintIndex = i + 1;
            if (expanded) {
                var wrapped = client.font.getSplitter().splitLines(detail, 240, Style.EMPTY).stream()
                        .map(line ->
                                (Component) Component.literal(line.getString()).withStyle(ChatFormatting.GRAY))
                        .toList();
                lines.addAll(i + 1, wrapped);
            }
        }
        if (!expanded && hintIndex >= 0)
            lines.add(
                    hintIndex,
                    Component.translatable(
                                    "tooltip.betterhorses.hold_crouch",
                                    client.options.keyShift.getTranslatedKeyMessage())
                            .withStyle(ChatFormatting.DARK_GRAY));
    }

    private static boolean crouchHeld(Minecraft client) {
        var binding = client.options.keyShift;
        // Read the physical binding: inventory screens do not update KeyMapping, and toggle-crouch is not a hold.
        if (!binding.getKeyModifier().isActive(null)) return false;
        var key = binding.getKey();
        long window = client.getWindow().getWindow();
        if (key.getType() == InputConstants.Type.MOUSE)
            return GLFW.glfwGetMouseButton(window, key.getValue()) == GLFW.GLFW_PRESS;
        if (key.getType() == InputConstants.Type.KEYSYM)
            return key.getValue() != GLFW.GLFW_KEY_UNKNOWN && InputConstants.isKeyDown(window, key.getValue());
        for (int code = GLFW.GLFW_KEY_SPACE; code <= GLFW.GLFW_KEY_LAST; code++)
            if (GLFW.glfwGetKeyScancode(code) == key.getValue() && InputConstants.isKeyDown(window, code)) return true;
        return false;
    }
}
