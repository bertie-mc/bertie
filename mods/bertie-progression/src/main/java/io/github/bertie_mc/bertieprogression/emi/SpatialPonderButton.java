package io.github.bertie_mc.bertieprogression.emi;

import dev.emi.emi.api.widget.Bounds;
import dev.emi.emi.api.widget.Widget;
import io.github.bertie_mc.bertieprogression.ponder.SpatialPonderPlugin;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/** Native-sized button without EMI's unrelated atlas icons. */
public final class SpatialPonderButton extends Widget {
    private final Bounds bounds;
    private final ResourceLocation recipe;

    public SpatialPonderButton(int x, int y, ResourceLocation recipe) {
        bounds = new Bounds(x, y, 58, 18);
        this.recipe = recipe;
    }

    @Override
    public Bounds getBounds() {
        return bounds;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        boolean hovered = bounds.contains(mouseX, mouseY);
        graphics.blitSprite(
                ResourceLocation.withDefaultNamespace(hovered ? "widget/button_highlighted" : "widget/button"),
                bounds.x(),
                bounds.y(),
                bounds.width(),
                bounds.height());
        graphics.drawCenteredString(
                Minecraft.getInstance().font,
                Component.literal("Ponder"),
                bounds.x() + bounds.width() / 2,
                bounds.y() + 5,
                0xFFFFFFFF);
    }

    @Override
    public boolean mouseClicked(int mouseX, int mouseY, int button) {
        if (button != 0 || !bounds.contains(mouseX, mouseY)) return false;
        SpatialPonderPlugin.open(recipe);
        return true;
    }
}
