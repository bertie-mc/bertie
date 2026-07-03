package com.berlord.foodsystem.client;

import com.berlord.foodsystem.BFS;
import com.berlord.foodsystem.Config;
import com.berlord.foodsystem.stomach.Stomach;
import com.berlord.foodsystem.stomach.StomachData;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * Stomach slot HUD: 20px (scaled) spacing, foods sorted by remaining time with the
 * longest-lasting in the RIGHTMOST slot, minutes shown like vanilla stack counts.
 * The rightmost slot keeps Reforged's original anchor; additional unlocked slots
 * (beyond 3) extend to the LEFT.
 */
@EventBusSubscriber(modid = BFS.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class StomachHud {

    private static final ResourceLocation EMPTY_SLOT =
            ResourceLocation.fromNamespaceAndPath(BFS.MODID, "textures/screens/empty_slot.png");

    @SubscribeEvent
    public static void register(RegisterGuiLayersEvent event) {
        event.registerAboveAll(ResourceLocation.fromNamespaceAndPath(BFS.MODID, "stomach"), StomachHud::render);
    }

    private static void render(GuiGraphics graphics, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null || mc.options.hideGui || player.isCreative() || player.isSpectator()) return;

        double xPos, yPos, scale;
        String preset = Config.HUD_PRESET.get();
        switch (preset) {
            case "custom" -> {
                xPos = Config.HUD_X.get();
                yPos = Config.HUD_Y.get();
                scale = Config.HUD_SCALE.get();
            }
            case "bottom_left" -> {
                xPos = -171.0;
                yPos = -11.0;
                scale = 0.9;
            }
            case "bottom_right" -> {
                xPos = 135.0;
                yPos = -11.0;
                scale = 0.9;
            }
            default -> {
                xPos = 47.0;
                yPos = -39.0;
                scale = 0.9;
            }
        }

        int unlocked = Stomach.unlockedSlots(player);
        double spacing = 20.0 * scale;
        // Reforged's three slots sat at anchor, anchor+1, anchor+2 spacings; keep the
        // rightmost of those fixed and let extra slots grow further left.
        double rightX = mc.getWindow().getGuiScaledWidth() / 2.0 + xPos + 2 * spacing;
        double y = mc.getWindow().getGuiScaledHeight() + yPos;

        // visual order, left to right: empty slots, regular foods by remaining time
        // (longest right), then eternal foods claiming the RIGHTMOST slots — first
        // eternal eaten sits rightmost, later ones fill right-to-left
        StomachData data = Stomach.get(player);
        List<StomachData.FoodSlot> eternal = new ArrayList<>(unlocked);
        List<StomachData.FoodSlot> regular = new ArrayList<>(unlocked);
        List<StomachData.FoodSlot> empty = new ArrayList<>(unlocked);
        for (int i = 0; i < unlocked; i++) {
            StomachData.FoodSlot slot = data.slots[i];
            if (!slot.isActive()) empty.add(slot);
            else if (Double.isInfinite(slot.duration)) eternal.add(slot);
            else regular.add(slot);
        }
        eternal.sort((a, b) -> Long.compare(b.eatOrder, a.eatOrder)); // newest left, oldest rightmost
        regular.sort((a, b) -> Double.compare(a.duration, b.duration)); // ascending: left -> right
        List<StomachData.FoodSlot> shown = new ArrayList<>(unlocked);
        shown.addAll(empty);
        shown.addAll(regular);
        shown.addAll(eternal);

        // semi-translucent frames need blending explicitly enabled at this layer
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        for (int k = 0; k < unlocked; k++) {
            double x = rightX - (unlocked - 1 - k) * spacing;
            graphics.pose().pushPose();
            graphics.pose().translate(x, y, 0);
            graphics.pose().scale((float) (1.13 * scale), (float) (1.13 * scale), 1.0F);
            graphics.blit(EMPTY_SLOT, -8, -8, 0, 0, 16, 16, 16, 16);
            graphics.pose().popPose();
        }
        RenderSystem.disableBlend();

        for (int k = 0; k < unlocked; k++) {
            double x = rightX - (unlocked - 1 - k) * spacing;
            StomachData.FoodSlot slot = shown.get(k);
            if (!slot.isActive()) continue;

            graphics.pose().pushPose();
            graphics.pose().translate(x, y, 50);
            graphics.pose().scale((float) scale, (float) scale, 1.0F);
            graphics.renderItem(slot.stack, -8, -8);
            graphics.pose().popPose();
        }

        // text + last-30s emphasis last, well above the items (renderItem internally
        // occupies z up to ~+200); the timer is positioned like a vanilla stack count
        // (bottom-right of the item, with shadow). In the final 30s the slot also gets a
        // pulsing gray cooldown-style overlay and the timer flips from minutes to exact
        // seconds, so the player can read remaining time and restock order at a glance.
        for (int k = 0; k < unlocked; k++) {
            double x = rightX - (unlocked - 1 - k) * spacing;
            StomachData.FoodSlot slot = shown.get(k);
            if (!slot.isActive()) continue;
            if (Double.isInfinite(slot.duration)) continue; // eternal foods show no timer

            String text;
            int color;
            if (slot.duration > 600.0) {
                text = Long.toString(Math.round(slot.duration / 1200.0)); // whole minutes
                color = 0xFFFFFFFF;
            } else {
                // last 30s: ~1 pulse/second, shared by the gray slot flash and red text glow
                float pulse = (float) (0.5 + 0.5 * Math.sin(mc.level.getGameTime() * Math.PI / 10.0));

                // pulsing gray overlay (vanilla cooldown white at oscillating alpha), drawn
                // over the icon but under the timer text; no extra colour, just transparency
                int alpha = (int) (pulse * 0x66); // swings 0x00..0x66
                graphics.pose().pushPose();
                graphics.pose().translate(x, y, 300);
                graphics.pose().scale((float) scale, (float) scale, 1.0F);
                graphics.fill(RenderType.guiOverlay(), -8, -8, 8, 8, (alpha << 24) | 0xFFFFFF);
                graphics.pose().popPose();

                // exact seconds remaining (30 -> 1), pulsing red
                text = Long.toString((long) Math.ceil(slot.duration / 20.0));
                int dim = 0x30 + (int) (pulse * 0x50); // green/blue channels swing 0x30..0x80
                color = 0xFFFF0000 | (dim << 8) | dim;
            }
            graphics.pose().pushPose();
            graphics.pose().translate(x, y, 400);
            graphics.pose().scale((float) scale, (float) scale, 1.0F);
            // vanilla renderItemDecorations math relative to the item's top-left (-8,-8)
            graphics.drawString(mc.font, text, 9 - mc.font.width(text), 1, color, true);
            graphics.pose().popPose();
        }
    }

    private StomachHud() {}
}
