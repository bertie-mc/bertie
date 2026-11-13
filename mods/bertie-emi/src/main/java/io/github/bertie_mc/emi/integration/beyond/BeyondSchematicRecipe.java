package io.github.bertie_mc.emi.integration.beyond;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.math.Axis;
import dev.emi.emi.api.recipe.BasicEmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.SlotWidget;
import dev.emi.emi.api.widget.WidgetHolder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import org.joml.Quaternionf;
import org.joml.Vector3f;

final class BeyondSchematicRecipe extends BasicEmiRecipe {
    private static final int INK = 0xff404040;
    private static final int GOLD = 0xffe6bb55;
    private final BeyondPages.Page page;
    private final List<Part> parts;

    BeyondSchematicRecipe(EmiRecipeCategory category, ResourceLocation id, BeyondPages.Page page) {
        super(category, id, BeyondPages.WIDTH, BeyondPages.HEIGHT);
        this.page = page;
        this.parts = page.nodes().stream().map(Part::new).toList();
        this.inputs = new ArrayList<>();
        this.catalysts = new ArrayList<>();
        this.outputs = new ArrayList<>();
        for (Part part : parts) {
            if (part.ingredient.isEmpty()) continue;
            switch (part.node.role()) {
                case INPUT -> inputs.add(part.ingredient);
                case CATALYST -> catalysts.add(part.ingredient);
                case OUTPUT -> outputs.addAll(part.ingredient.getEmiStacks());
                case DISPLAY -> {}
            }
        }
    }

    @Override
    public boolean supportsRecipeTree() {
        return false;
    }

    @Override
    public void addWidgets(WidgetHolder widgets) {
        text(widgets, page.title(), 1, 2);
        if (page.id().startsWith("casting_")) castingLegend(widgets);
        for (BeyondPages.Link link : page.links()) {
            widgets.addDrawable(0, 0, width, height, (graphics, mx, my, delta) -> drawArrow(graphics, link));
        }
        for (Part part : parts) {
            BeyondPages.Node node = part.node;
            SlotWidget slot = widgets.add(new SceneSlot(part));
            slot.drawBack(false).catalyst(node.role() == BeyondPages.Role.CATALYST);
            // Context marks actual results, not ingredients or scenery, as output slots in EMI.
            if (node.role() == BeyondPages.Role.OUTPUT) slot.recipeContext(this);
            slot.appendTooltip(Component.literal(node.hint()));
            if (!node.label().isEmpty()) label(widgets, node.label(), node.x() + 9, node.y() + 21, node.hint());
        }
        for (BeyondPages.Caption caption : page.captions()) {
            text(widgets, caption.text(), caption.x(), caption.y());
            int pixels = Minecraft.getInstance().font.width(caption.text());
            widgets.addTooltipText(List.of(Component.literal(caption.hint())), caption.x(), caption.y(), pixels, 10);
        }
        if (page.id().startsWith("casting_")) {
            widgets.addDrawable(
                    31, 51, 20, 20, (graphics, mx, my, delta) -> graphics.renderOutline(0, 0, 20, 20, GOLD));
            widgets.addTooltipText(
                    List.of(Component.literal("Right-click the centre with an empty hand.")), 31, 51, 20, 20);
        }
    }

    private void castingLegend(WidgetHolder widgets) {
        widgets.addDrawable(0, 0, width, height, (graphics, mx, my, delta) -> {
            for (Part part : parts) {
                if (part.node.kind() != BeyondPages.Kind.BLOCK) continue;
                boolean cracked = Boolean.parseBoolean(part.node.properties().get("powered"));
                graphics.fill(
                        part.node.x(),
                        part.node.y(),
                        part.node.x() + 18,
                        part.node.y() + 18,
                        cracked ? 0x30595959 : 0x6043a05c);
            }
            graphics.fill(91, 80, 97, 86, 0xff43a05c);
            graphics.fill(91, 92, 97, 98, 0xff595959);
        });
        text(widgets, "Solid", 101, 79);
        text(widgets, "Cracked", 101, 91);
    }

    private static void text(WidgetHolder widgets, String text, int x, int y) {
        widgets.addDrawable(x, y, BeyondPages.WIDTH - x, 10, (graphics, mx, my, delta) -> {
            var font = Minecraft.getInstance().font;
            float scale = Math.min(1, (BeyondPages.WIDTH - x) / (float) Math.max(1, font.width(text)));
            graphics.pose().pushPose();
            graphics.pose().scale(scale, scale, 1);
            graphics.drawString(font, text, 0, 0, INK, false);
            graphics.pose().popPose();
        });
    }

    private static void label(WidgetHolder widgets, String text, int centre, int y, String hint) {
        int pixels = Minecraft.getInstance().font.width(text);
        int x = Math.max(0, Math.min(BeyondPages.WIDTH - pixels, centre - pixels / 2));
        widgets.addText(Component.literal(text), x, y, INK, false);
        widgets.addTooltipText(List.of(Component.literal(hint)), x, y, pixels, 10);
    }

    private static void drawArrow(GuiGraphics graphics, BeyondPages.Link line) {
        int dx = line.x2() - line.x1();
        int dy = line.y2() - line.y1();
        int length = Math.max(Math.abs(dx), Math.abs(dy));
        for (int i = 0; i <= length; i++) {
            int x = line.x1() + dx * i / Math.max(1, length);
            int y = line.y1() + dy * i / Math.max(1, length);
            graphics.fill(x, y, x + 1, y + 1, INK);
        }
        double angle = Math.atan2(dy, dx);
        for (int side : new int[] {-1, 1}) {
            for (int i = 0; i < 5; i++) {
                int x = line.x2() - (int) Math.round(Math.cos(angle + side * 0.6) * i);
                int y = line.y2() - (int) Math.round(Math.sin(angle + side * 0.6) * i);
                graphics.fill(x, y, x + 1, y + 1, INK);
            }
        }
    }

    private static final class Part {
        final BeyondPages.Node node;
        final EmiIngredient ingredient;
        BlockState state;
        EntityType<?> entityType;
        LivingEntity entity;

        Part(BeyondPages.Node node) {
            this.node = node;
            ResourceLocation id = ResourceLocation.parse(node.id().split("\\|")[0]);
            if (node.kind() == BeyondPages.Kind.ALTERNATIVES) {
                ingredient = EmiIngredient.of(Arrays.stream(node.id().split("\\|"))
                        .map(value -> EmiStack.of(BuiltInRegistries.ITEM
                                .getOptional(ResourceLocation.parse(value))
                                .orElseThrow()))
                        .toList());
            } else if (node.kind() == BeyondPages.Kind.TAG) {
                ingredient = EmiIngredient.of(ItemTags.create(id), node.amount());
            } else if (node.kind() == BeyondPages.Kind.ENTITY) {
                entityType = BuiltInRegistries.ENTITY_TYPE.getOptional(id).orElseThrow();
                SpawnEggItem egg = SpawnEggItem.byId(entityType);
                ingredient = egg == null ? EmiStack.EMPTY : EmiStack.of(egg);
            } else if (node.kind() == BeyondPages.Kind.BLOCK) {
                state = BuiltInRegistries.BLOCK.getOptional(id).orElseThrow().defaultBlockState();
                for (var entry : node.properties().entrySet()) {
                    Property<?> property = state.getBlock().getStateDefinition().getProperty(entry.getKey());
                    if (property == null) throw new IllegalArgumentException("Unknown block property " + node);
                    state = setProperty(state, property, entry.getValue());
                }
                ingredient = EmiStack.of(state.getBlock());
            } else {
                ingredient = EmiStack.of(BuiltInRegistries.ITEM.getOptional(id).orElseThrow());
            }
            if (!ingredient.isEmpty()) {
                ingredient.setAmount(node.amount());
                if (ingredient instanceof EmiStack stack) stack.setChance(node.chance());
            }
        }

        private static <T extends Comparable<T>> BlockState setProperty(
                BlockState state, Property<T> property, String value) {
            return state.setValue(property, property.getValue(value).orElseThrow());
        }

        LivingEntity entity() {
            Minecraft mc = Minecraft.getInstance();
            if (mc.level != null && (entity == null || entity.level() != mc.level)) {
                if (entityType.create(mc.level) instanceof LivingEntity living) entity = living;
            }
            return entity;
        }
    }

    private static final class SceneSlot extends SlotWidget {
        private final Part part;

        SceneSlot(Part part) {
            super(part.ingredient, part.node.x(), part.node.y());
            this.part = part;
            if (part.node.kind() == BeyondPages.Kind.ENTITY) {
                appendTooltip(part.entityType.getDescription());
            } else if (part.state != null && part.ingredient.isEmpty()) {
                appendTooltip(part.state.getBlock().getName());
            }
        }

        @Override
        public void drawStack(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
            if (part.state != null) {
                Minecraft mc = Minecraft.getInstance();
                graphics.flush();
                graphics.pose().pushPose();
                graphics.pose().translate(x + 9, y + 9, 100);
                graphics.pose().scale(11, -11, 11);
                graphics.pose().mulPose(Axis.XP.rotationDegrees(30));
                graphics.pose().mulPose(Axis.YP.rotationDegrees(225));
                graphics.pose().translate(-0.5, -0.5, -0.5);
                Lighting.setupFor3DItems();
                mc.getBlockRenderer()
                        .renderSingleBlock(
                                part.state,
                                graphics.pose(),
                                graphics.bufferSource(),
                                15728880,
                                OverlayTexture.NO_OVERLAY);
                graphics.flush();
                graphics.pose().popPose();
                Lighting.setupFor3DItems();
            } else if (part.entityType != null && part.entity() != null) {
                LivingEntity entity = part.entity();
                int scale = Math.max(2, (int) (16 / Math.max(entity.getBbHeight(), entity.getBbWidth() * 1.4f)));
                InventoryScreen.renderEntityInInventory(
                        graphics,
                        x + 9,
                        y + 9,
                        scale,
                        new Vector3f(0, entity.getBbHeight() / 2, 0),
                        new Quaternionf().rotateZ((float) Math.PI).rotateY(0.5f),
                        new Quaternionf(),
                        entity);
            } else {
                super.drawStack(graphics, mouseX, mouseY, delta);
            }
        }
    }
}
