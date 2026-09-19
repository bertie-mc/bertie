package io.github.bertie_mc.betterhorses.client;

import io.github.bertie_mc.betterhorses.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public final class BetterHorseScreen extends AbstractContainerScreen<BetterHorseMenu> {
    public BetterHorseScreen(BetterHorseMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(
                ResourceLocation.withDefaultNamespace("textures/gui/container/horse.png"),
                leftPos,
                topPos,
                0,
                0,
                imageWidth,
                imageHeight);
        int previewRight = 78;
        graphics.fill(leftPos + 26, topPos + 17, leftPos + previewRight, topPos + 71, 0xff171717);
        if (menu.hasStorage())
            graphics.blitSprite(
                    ResourceLocation.withDefaultNamespace("container/horse/chest_slots"),
                    leftPos + 79,
                    topPos + 17,
                    90,
                    54);
        graphics.blitSprite(
                ResourceLocation.withDefaultNamespace("container/horse/saddle_slot"), leftPos + 7, topPos + 17, 18, 18);
        if (menu.getSlot(0).hasItem()) graphics.fill(leftPos + 8, topPos + 18, leftPos + 24, topPos + 34, 0xff8b8b8b);
        graphics.blitSprite(
                ResourceLocation.withDefaultNamespace("container/horse/armor_slot"), leftPos + 7, topPos + 35, 18, 18);
        graphics.fill(leftPos + 7, topPos + 53, leftPos + 25, topPos + 71, 0xff373737);
        graphics.fill(leftPos + 8, topPos + 54, leftPos + 25, topPos + 71, 0xffffffff);
        graphics.fill(leftPos + 8, topPos + 54, leftPos + 24, topPos + 70, 0xff8b8b8b);
        if (!menu.getSlot(2).hasItem())
            graphics.blit(
                    BetterHorses.id("textures/gui/horseshoe_slot.png"), leftPos + 8, topPos + 54, 0, 0, 16, 16, 16, 16);
        InventoryScreen.renderEntityInInventoryFollowsMouse(
                graphics,
                leftPos + 26,
                topPos + 18,
                leftPos + previewRight,
                topPos + 71,
                18,
                0.25F,
                mouseX,
                mouseY,
                menu.horse);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
        if (!menu.getSlot(2).hasItem() && isHovering(8, 54, 16, 16, mouseX, mouseY))
            graphics.renderTooltip(font, Component.translatable("container.betterhorses.horseshoes"), mouseX, mouseY);
    }
}
