package io.github.bertie_mc.emi.integration.stellaris;

import dev.emi.emi.api.recipe.BasicEmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.SlotWidget;
import dev.emi.emi.api.widget.WidgetHolder;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;

/** Operations on a placed rocket, drawn as the launch platform or the rocket's actual inventory. */
final class RocketGuideEmiRecipe extends BasicEmiRecipe {
    enum Kind {
        PAD,
        INVENTORY,
        UPGRADE,
        FUEL,
        LAUNCH,
        ANTENNA
    }

    private static final int INK = 0xff404040;
    private static final int GOLD = 0xffba8b28;
    private final Kind kind;
    private final EmiStack rocket;
    private final EmiIngredient subject;
    private final RocketLayouts.UpgradeSlot target;
    private final String title;
    private final String summary;
    private final String detail;
    private final EmiStack motor;

    RocketGuideEmiRecipe(
            EmiRecipeCategory category,
            String id,
            Kind kind,
            EmiStack rocket,
            EmiIngredient subject,
            RocketLayouts.UpgradeSlot target,
            String title,
            String summary,
            String detail,
            EmiStack result,
            EmiStack motor,
            boolean returnsBucket) {
        super(category, ResourceLocation.fromNamespaceAndPath("bertieemi", "stellaris/rocket/" + id), 176, 132);
        this.kind = kind;
        this.rocket = rocket;
        this.subject = subject;
        this.target = target;
        this.title = title;
        this.summary = summary;
        this.detail = detail;
        this.motor = motor;
        this.inputs = kind == Kind.FUEL ? List.of(subject) : List.of();
        this.catalysts = new ArrayList<>(List.of(rocket));
        if (kind != Kind.FUEL && !subject.isEmpty()) catalysts.add(subject);
        if (!motor.isEmpty()) catalysts.add(motor);
        this.outputs = new ArrayList<>();
        if (!result.isEmpty()) outputs.add(result);
        if (returnsBucket) outputs.add(EmiStack.of(Items.BUCKET));
    }

    @Override
    public boolean supportsRecipeTree() {
        return false;
    }

    @Override
    public void addWidgets(WidgetHolder widgets) {
        text(widgets, title, 2, 2, 172, detail);
        switch (kind) {
            case PAD -> pad(widgets);
            case INVENTORY -> inventory(widgets);
            case UPGRADE -> upgrade(widgets);
            case FUEL -> fuel(widgets);
            case LAUNCH -> launch(widgets);
            case ANTENNA -> antenna(widgets);
        }
        text(widgets, summary, 2, 108, 172, detail);
        String access =
                switch (kind) {
                    case INVENTORY, UPGRADE, FUEL -> "Open: crouch + empty-hand use";
                    case PAD -> "Keep the space above clear";
                    case LAUNCH -> "Pack up: crouch-hit an empty Rocket";
                    case ANTENNA -> "Open the Antenna menu to register";
                };
        text(widgets, access, 2, 120, 172, detail);
    }

    private void pad(WidgetHolder widgets) {
        EmiStack pad = RocketGuides.stack("rocket_launch_pad");
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                slot(
                        widgets,
                        pad,
                        14 + col * 20,
                        31 + row * 20,
                        true,
                        row == 1 && col == 1
                                ? "Use the Rocket on this centre block."
                                : "Place nine Launch Pad blocks in a flat 3x3 square.");
            }
        }
        outline(widgets, 33, 50, 20, 20);
        widgets.addFillingArrow(85, 52, 2000);
        slot(
                widgets,
                rocket,
                133,
                52,
                true,
                "Place the Rocket on the activated centre. Leave the space above the pad clear.");
        text(widgets, "Top view", 14, 18, 70, "The raised, activated centre is the placement target.");
        text(widgets, "Place", 130, 76, 42, detail);
    }

    private void inventory(WidgetHolder widgets) {
        cargo(widgets);
        slot(
                widgets,
                RocketGuides.stack("fuel_bucket"),
                20,
                28,
                false,
                "Fuel input. Its type must match the installed motor.");
        slot(widgets, EmiStack.of(Items.BUCKET), 20, 62, false, "Empty fluid containers appear here.");
        gauge(widgets);
        for (var position : RocketLayouts.UpgradeSlot.values()) {
            EmiIngredient icon =
                    switch (position) {
                        case MOTOR -> RocketGuides.stack("hydrogen_motor");
                        case TANK -> RocketGuides.stack("big_fuel_tank_upgrade");
                        case SKIN -> RocketGuides.stack("skin_normal");
                        case MODEL -> RocketGuides.stack("small_rocket_upgrade");
                    };
            slot(
                    widgets,
                    icon,
                    position.x,
                    position.y,
                    false,
                    position.label
                            + " upgrade slot. Motor and tank upgrades can be removed only with an empty fuel tank.");
        }
        text(
                widgets,
                "Motor | Tank | Skin | Model",
                74,
                96,
                98,
                "The four upgrade slots are ordered left to right. Speed upgrades belong to the Rover, not the Rocket.");
    }

    private void upgrade(WidgetHolder widgets) {
        cargo(widgets);
        for (var position : RocketLayouts.UpgradeSlot.values()) {
            boolean selected = position == target;
            slot(
                    widgets,
                    selected ? subject : EmiStack.EMPTY,
                    position.x,
                    position.y,
                    false,
                    position.label + " slot. " + (selected ? detail : ""));
        }
        outline(widgets, target.x - 1, target.y - 1, 20, 20);
        slot(widgets, subject, 14, 45, true, detail);
        arrow(widgets, 39, 54, 70, 68);
        arrow(widgets, 70, 68, target.x + 9, target.y - 3);
        EmiStack preview = outputs.isEmpty() ? rocket : outputs.getFirst();
        var previewSlot = slot(
                widgets,
                preview,
                20,
                78,
                true,
                "Configured Rocket preview. The upgrade stays in its slot; removing it restores the default configuration.");
        if (!outputs.isEmpty()) previewSlot.recipeContext(this);
        text(widgets, "Insert in " + target.label + " slot", 73, 14, 101, detail);
    }

    private void fuel(WidgetHolder widgets) {
        cargo(widgets);
        slot(widgets, subject, 20, 28, false, "Fuel input: one item adds 1,000 fuel, capped by the installed tank.");
        var containerSlot = slot(
                widgets,
                outputs.size() > 1 ? outputs.get(1) : EmiStack.EMPTY,
                20,
                62,
                false,
                "Regular Fuel and Hydrogen return an empty Bucket. Radioactive fuel consumes an ingot.");
        if (outputs.size() > 1) containerSlot.recipeContext(this);
        gauge(widgets);
        for (var position : RocketLayouts.UpgradeSlot.values()) {
            slot(
                    widgets,
                    position == RocketLayouts.UpgradeSlot.MOTOR ? motor : EmiStack.EMPTY,
                    position.x,
                    position.y,
                    false,
                    position.label + " slot. " + detail);
        }
        slot(widgets, outputs.getFirst(), 147, 10, true, "Rocket after adding one fuel item. " + detail)
                .recipeContext(this);
        text(widgets, motor.isEmpty() ? "Default motor" : "Required motor", 77, 95, 95, detail);
    }

    private void launch(WidgetHolder widgets) {
        slot(widgets, rocket, 14, 44, true, "Right-click to board. The first passenger pilots the Rocket.");
        widgets.addFillingArrow(43, 45, 2000);
        slot(
                widgets,
                EmiStack.of(Items.CLOCK),
                78,
                44,
                true,
                "Press the Jump control to start the 200-tick (10-second) countdown. Current binding: "
                        + Minecraft.getInstance()
                                .options
                                .keyJump
                                .getTranslatedKeyMessage()
                                .getString());
        widgets.addFillingArrow(106, 45, 2000);
        slot(widgets, RocketGuides.stack("moon_stone"), 144, 44, true, detail);
        text(widgets, "Board", 13, 70, 43, detail);
        text(widgets, "Jump", 76, 70, 45, detail);
        text(widgets, "Choose target", 128, 70, 46, detail);
    }

    private void antenna(WidgetHolder widgets) {
        slot(
                widgets,
                RocketGuides.stack("rocket_launch_pad"),
                36,
                30,
                true,
                "Activated centre of the 3x3 Launch Pad; side view.");
        slot(widgets, subject, 36, 50, true, detail);
        arrow(widgets, 62, 49, 117, 49);
        slot(
                widgets,
                rocket,
                136,
                40,
                true,
                "A registered pad is available as a named destination in the planet-selection screen.");
        text(widgets, "Under centre", 13, 77, 88, detail);
        text(widgets, "Named landing", 103, 77, 70, detail);
    }

    private void cargo(WidgetHolder widgets) {
        for (int col = 0; col < 4; col++) {
            for (int row = 0; row < 2; row++)
                slot(widgets, EmiStack.EMPTY, 82 + col * 18, 28 + row * 18, false, "Cargo slot.");
        }
    }

    private void gauge(WidgetHolder widgets) {
        widgets.addDrawable(52, 28, 12, 46, (graphics, mx, my, delta) -> {
            graphics.fill(0, 0, 12, 46, 0xff555555);
            graphics.fill(1, 30, 11, 45, 0xff428caa);
        });
        widgets.addTooltipText(
                List.of(Component.literal("Fuel gauge. Basic tank: 3,000; Big Fuel Tank upgrade: 5,000.")),
                52,
                28,
                12,
                46);
    }

    private SlotWidget slot(
            WidgetHolder widgets, EmiIngredient ingredient, int x, int y, boolean scenery, String hint) {
        SlotWidget slot = widgets.addSlot(ingredient, x, y).drawBack(!scenery);
        slot.appendTooltip(Component.literal(hint));
        if (ingredient.isEmpty()) widgets.addTooltipText(List.of(Component.literal(hint)), x, y, 18, 18);
        return slot;
    }

    private static void outline(WidgetHolder widgets, int x, int y, int width, int height) {
        widgets.addDrawable(
                x, y, width, height, (graphics, mx, my, delta) -> graphics.renderOutline(0, 0, width, height, GOLD));
    }

    private static void arrow(WidgetHolder widgets, int x1, int y1, int x2, int y2) {
        widgets.addDrawable(0, 0, 176, 128, (graphics, mx, my, delta) -> {
            int dx = x2 - x1, dy = y2 - y1;
            int length = Math.max(Math.abs(dx), Math.abs(dy));
            for (int i = 0; i <= length; i++) {
                int x = x1 + dx * i / Math.max(1, length), y = y1 + dy * i / Math.max(1, length);
                graphics.fill(x, y, x + 1, y + 1, INK);
            }
            double angle = Math.atan2(dy, dx);
            for (int side : new int[] {-1, 1})
                for (int i = 0; i < 5; i++) {
                    int x = x2 - (int) Math.round(Math.cos(angle + side * 0.6) * i);
                    int y = y2 - (int) Math.round(Math.sin(angle + side * 0.6) * i);
                    graphics.fill(x, y, x + 1, y + 1, INK);
                }
        });
    }

    private static void text(WidgetHolder widgets, String value, int x, int y, int available, String hint) {
        widgets.addDrawable(x, y, available, 10, (graphics, mx, my, delta) -> {
            var font = Minecraft.getInstance().font;
            float scale = Math.min(1, available / (float) Math.max(1, font.width(value)));
            graphics.pose().pushPose();
            graphics.pose().scale(scale, scale, 1);
            graphics.drawString(font, value, 0, 0, INK, false);
            graphics.pose().popPose();
        });
        widgets.addTooltipText(List.of(Component.literal(hint)), x, y, available, 10);
    }
}
