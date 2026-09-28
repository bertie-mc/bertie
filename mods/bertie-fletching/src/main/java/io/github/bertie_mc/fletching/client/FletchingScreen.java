package io.github.bertie_mc.fletching.client;

import com.fletchery.mod.config.ModConfig;
import com.fletchery.mod.entity.CustomArrowEntity;
import com.fletchery.mod.registry.ModRegistries;
import com.mojang.blaze3d.vertex.PoseStack;
import io.github.bertie_mc.fletching.FletchingMenu;
import io.github.bertie_mc.fletching.PotionTank;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.MultiBufferSource.BufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import org.joml.Quaternionf;

public class FletchingScreen extends AbstractContainerScreen<FletchingMenu> {
    private static final ResourceLocation TEXTURE_EN =
            ResourceLocation.parse("fletchery_expanded:textures/gui/fletching_table_en.png");
    private static final ResourceLocation TEXTURE_RU =
            ResourceLocation.parse("fletchery_expanded:textures/gui/fletching_table_ru.png");
    private CompoundTag lastPreviewNbt = new CompoundTag();
    private CustomArrowEntity previewEntity = null;
    private static final int PREVIEW_X = 133;
    private static final int PREVIEW_Y = 2;
    private static final int PREVIEW_WIDTH = 39;
    private static final int PREVIEW_HEIGHT = 67;
    private static final long[] ROTATE_INTERVAL_MS = new long[] {6000L, 4000L, 5000L, 4500L};
    private static final List<ResourceLocation> FEATHER_PLACEHOLDERS = List.of(
            placeholder("feather", "feather"),
            placeholder("feather", "phantom_membrane"),
            placeholder("feather", "wheat"));
    private static final List<ResourceLocation> SHAFT_PLACEHOLDERS = List.of(
            placeholder("shaft", "blaze_rod"),
            placeholder("shaft", "bone"),
            placeholder("shaft", "fishing_rod"),
            placeholder("shaft", "chain"),
            placeholder("shaft", "stick"));
    private static final List<ResourceLocation> TIP_PLACEHOLDERS = List.of(
            placeholder("tip", "amethyst_shard"),
            placeholder("tip", "diamond"),
            placeholder("tip", "echo_shard"),
            placeholder("tip", "flint"),
            placeholder("tip", "iron_ingot"),
            placeholder("tip", "prismarine_shard"),
            placeholder("tip", "quartz"),
            placeholder("tip", "shulker_shell"));
    private static final List<ResourceLocation> EFFECT_PLACEHOLDERS = List.of(
            placeholder("effect", "blaze_powder"),
            placeholder("effect", "dragon_breath"),
            placeholder("effect", "ender_pearl"),
            placeholder("effect", "fire_charge"),
            placeholder("effect", "firework_rocket"),
            placeholder("effect", "firework_star"),
            placeholder("effect", "gunpowder"),
            placeholder("effect", "heart_of_the_sea"),
            placeholder("effect", "honeycomb"),
            placeholder("effect", "lapis_lazuli"),
            placeholder("effect", "slime_ball"),
            placeholder("effect", "torch"),
            placeholder("effect", "turtle_helmet"),
            placeholder("effect", "wind_charge"));
    private static final List<List<ResourceLocation>> SLOT_PLACEHOLDER_SETS =
            List.of(FEATHER_PLACEHOLDERS, SHAFT_PLACEHOLDERS, TIP_PLACEHOLDERS, EFFECT_PLACEHOLDERS);
    private static final int[][] SLOT_POS = new int[][] {{8, 48}, {26, 48}, {44, 48}, {98, 8}};
    private boolean isDragging = false;
    private double dragStartX = 0.0;
    private float manualRotation = 0.0F;
    private float autoRotation = 0.0F;
    private boolean useManualRotation = false;
    private long lastDragTime = 0L;
    private static final long AUTO_RESUME_DELAY = 3000L;

    private static ResourceLocation placeholder(String category, String fileName) {
        return ResourceLocation.parse(
                "fletchery_expanded:textures/gui/placeholder/" + category + "/" + fileName + ".png");
    }

    public FletchingScreen(FletchingMenu handler, Inventory inventory, Component title) {
        super(handler, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
    }

    private ResourceLocation getTexture() {
        String lang = Minecraft.getInstance().getLanguageManager().getSelected();
        return lang.startsWith("ru") ? TEXTURE_RU : TEXTURE_EN;
    }

    private boolean isMouseInPreview(double mouseX, double mouseY) {
        int px = this.leftPos + 133;
        int py = this.topPos + 2;
        return mouseX >= px && mouseX <= px + 39 && mouseY >= py && mouseY <= py + 67;
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && isMouseInTank(mouseX, mouseY)) {
            if (this.minecraft != null && this.minecraft.gameMode != null)
                this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, 0);
            return true;
        }
        if (button == 0 && this.isMouseInPreview(mouseX, mouseY) && this.previewEntity != null) {
            this.isDragging = true;
            this.dragStartX = mouseX;
            this.useManualRotation = true;
            this.lastDragTime = System.currentTimeMillis();
            return true;
        } else {
            return super.mouseClicked(mouseX, mouseY, button);
        }
    }

    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0 && this.isDragging) {
            this.isDragging = false;
            this.lastDragTime = System.currentTimeMillis();
            return true;
        } else {
            return super.mouseReleased(mouseX, mouseY, button);
        }
    }

    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (button == 0 && this.isDragging) {
            this.manualRotation += (float) dragX * 1.5F;
            this.manualRotation %= 360.0F;
            this.lastDragTime = System.currentTimeMillis();
            return true;
        } else {
            return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
        }
    }

    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {}

    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        guiGraphics.blit(
                this.getTexture(), this.leftPos, this.topPos, 0.0F, 0.0F, this.imageWidth, this.imageHeight, 256, 256);
        this.renderEmptySlotPlaceholders(guiGraphics);
        renderTank(guiGraphics);
    }

    private void renderEmptySlotPlaceholders(GuiGraphics guiGraphics) {
        long now = System.currentTimeMillis();

        for (int i = 0; i < SLOT_POS.length; i++) {
            ItemStack stack = this.menu.getSlot(i).getItem();
            if (stack.isEmpty()) {
                List<ResourceLocation> icons = SLOT_PLACEHOLDER_SETS.get(i);
                if (!icons.isEmpty()) {
                    int frame = (int) (now / ROTATE_INTERVAL_MS[i] % icons.size());
                    ResourceLocation icon = icons.get(frame);
                    int slotX = this.leftPos + SLOT_POS[i][0];
                    int slotY = this.topPos + SLOT_POS[i][1];
                    guiGraphics.blit(icon, slotX, slotY, 0.0F, 0.0F, 16, 16, 16, 16);
                }
            }
        }
    }

    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
        this.renderArrowPreview(guiGraphics, partialTick);
        this.renderSlotHints(guiGraphics, mouseX, mouseY);
        if (isMouseInTank(mouseX, mouseY)) renderTankTooltip(guiGraphics, mouseX, mouseY);
        if (this.previewEntity != null && this.isMouseInPreview(mouseX, mouseY)) {
            guiGraphics.renderTooltip(
                    this.font, Component.translatable("fletchery_expanded.preview.hint"), mouseX, mouseY);
        }
    }

    private void renderSlotHints(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        Slot hovered = this.hoveredSlot;
        if (hovered != null && hovered.index < 4) {
            if (!hovered.hasItem()) {
                String key =
                        switch (hovered.getContainerSlot()) {
                            case 0 -> "fletchery_expanded.slot.feather";
                            case 1 -> "fletchery_expanded.slot.shaft";
                            case 2 -> "fletchery_expanded.slot.tip";
                            case 3 -> "fletchery_expanded.slot.effect";
                            default -> null;
                        };
                if (key != null) {
                    guiGraphics.renderTooltip(this.font, Component.translatable(key), mouseX, mouseY);
                }
            }
        }
    }

    private void renderArrowPreview(GuiGraphics guiGraphics, float partialTick) {
        ItemStack result = this.menu.preview();
        if (result.isEmpty()) {
            this.previewEntity = null;
            this.useManualRotation = false;
            this.isDragging = false;
        } else {
            CompoundTag newNbt = null;
            CustomData customData = (CustomData) result.get(DataComponents.CUSTOM_DATA);
            if (customData != null) {
                newNbt = customData.copyTag();
            }

            if (newNbt == null) {
                newNbt = new CompoundTag();
            }

            if (this.previewEntity == null || !newNbt.equals(this.lastPreviewNbt)) {
                this.lastPreviewNbt = newNbt.copy();
                Level level = Minecraft.getInstance().level;
                if (level == null) {
                    return;
                }

                this.previewEntity = new CustomArrowEntity(ModRegistries.CUSTOM_ARROW_ENTITY.get(), level);
                if (!newNbt.isEmpty()) {
                    this.previewEntity.setCustomProperties(newNbt);
                }
            }

            long now = System.currentTimeMillis();
            long timeSinceDrag = now - this.lastDragTime;
            float rotation;
            if (this.useManualRotation && timeSinceDrag < 3000L) {
                rotation = this.manualRotation;
            } else {
                if (this.useManualRotation && timeSinceDrag >= 3000L) {
                    this.useManualRotation = false;
                    this.autoRotation = this.manualRotation;
                }

                float rotationSpeed = ModConfig.get().previewRotationSpeed;
                if (rotationSpeed <= 0.0F) {
                    rotationSpeed = 10.0F;
                }

                float autoSpeed = (float) ((double) now / rotationSpeed % 360.0);
                rotation = this.autoRotation + autoSpeed;
            }

            int previewX = this.leftPos + 133;
            int previewY = this.topPos + 2;
            PoseStack poseStack = guiGraphics.pose();
            poseStack.pushPose();
            poseStack.translate(previewX + 19.5, previewY + 33.5 - 15.0, 50.0);
            poseStack.mulPose(new Quaternionf().rotationX((float) Math.toRadians(90.0)));
            poseStack.mulPose(new Quaternionf().rotationZ((float) Math.toRadians(rotation)));
            float scale = ModConfig.get().previewScale;
            poseStack.scale(scale, scale, scale);
            BufferSource buffer = Minecraft.getInstance().renderBuffers().bufferSource();
            EntityRenderDispatcher dispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
            dispatcher.setRenderShadow(false);
            dispatcher.render(this.previewEntity, 0.0, 0.0, 0.0, 0.0F, partialTick, poseStack, buffer, 15728880);
            buffer.endBatch();
            poseStack.popPose();
        }
    }

    private boolean isMouseInTank(double x, double y) {
        return x >= leftPos + 69 && x < leftPos + 87 && y >= topPos + 12 && y < topPos + 48;
    }

    private void renderTank(GuiGraphics g) {
        int x = leftPos + 69, y = topPos + 12;
        g.fill(x, y, x + 18, y + 36, 0xff373737);
        g.fill(x + 1, y + 1, x + 17, y + 35, 0xff8b8b8b);
        g.fill(x + 1, y + 35, x + 18, y + 36, 0xffffffff);
        g.fill(x + 17, y + 1, x + 18, y + 36, 0xffffffff);
        if (!menu.tankDisplay().isEmpty()) {
            var contents = menu.tankDisplay()
                    .getOrDefault(
                            DataComponents.POTION_CONTENTS, net.minecraft.world.item.alchemy.PotionContents.EMPTY);
            int color = contents.getColor() | 0xff000000;
            int height = Math.max(1, 32 * menu.tankBatches() / PotionTank.CAPACITY);
            g.fill(x + 2, y + 34 - height, x + 16, y + 34, color);
            for (int row = y + 35 - height; row < y + 33; row += 5) g.fill(x + 3, row, x + 6, row + 1, 0x45ffffff);
            g.fill(x + 2, y + 34 - height, x + 16, y + 35 - height, 0x65ffffff);
        }
        g.fill(x + 2, y + 2, x + 3, y + 33, 0x30ffffff);
        for (int level = 1; level < 8; level++) {
            int lineY = y + 2 + level * 4;
            int lineEnd = level == 4 ? x + 17 : x + 4;
            g.fill(x + 1, lineY, lineEnd, lineY + 1, 0xff373737);
        }
    }

    private void renderTankTooltip(GuiGraphics g, int x, int y) {
        java.util.ArrayList<Component> lines = new java.util.ArrayList<>();
        lines.add(Component.translatable("bertiefletching.tank"));
        ItemStack bottle = menu.tankDisplay();
        if (bottle.isEmpty()) lines.add(Component.translatable("bertiefletching.tank.empty"));
        else {
            lines.add(bottle.getHoverName());
            var contents = bottle.getOrDefault(
                    DataComponents.POTION_CONTENTS, net.minecraft.world.item.alchemy.PotionContents.EMPTY);
            for (var effect : contents.getAllEffects())
                lines.add(Component.translatable(effect.getDescriptionId()).append(" " + (effect.getAmplifier() + 1)));
        }
        lines.add(Component.translatable("bertiefletching.tank.fill"));
        g.renderComponentTooltip(font, lines, x, y);
    }

    public void onClose() {
        super.onClose();
        this.previewEntity = null;
    }
}
