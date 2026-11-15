package io.github.bertie_mc.emi.integration.stellaris;

import static org.junit.jupiter.api.Assertions.*;

import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.SlotWidget;
import dev.emi.emi.api.widget.Widget;
import dev.emi.emi.api.widget.WidgetHolder;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.Test;

class RocketGuideTest {
    @Test
    void constructionPreservesAllFourteenIngredientPositions() {
        List<EmiIngredient> ingredients = new ArrayList<>();
        for (int i = 0; i < 14; i++)
            ingredients.add(EmiStack.of(Items.IRON_INGOT).setAmount(i + 1));
        var recipe = new RocketStationEmiRecipe(
                null, ResourceLocation.parse("bertieemi:station_fixture"), ingredients, EmiStack.of(Items.MINECART));
        var holder = new Holder(recipe.getDisplayWidth(), recipe.getDisplayHeight());
        recipe.addWidgets(holder);
        var slots = holder.widgets.stream()
                .filter(widget -> widget instanceof SlotWidget)
                .map(widget -> (SlotWidget) widget)
                .toList();
        assertEquals(15, slots.size());
        for (int i = 0; i < 14; i++) assertEquals(i + 1, slots.get(i).getStack().getAmount());
        var nose = slots.get(0).getBounds();
        var engine = slots.get(12).getBounds();
        assertEquals(nose.x(), engine.x());
        assertTrue(nose.y() < slots.get(1).getBounds().y());
        assertTrue(slots.get(11).getBounds().x() < engine.x());
        assertTrue(slots.get(13).getBounds().x() > engine.x());
        assertSame(recipe, slots.getLast().getRecipe());
        for (var slot : slots) {
            var bounds = slot.getBounds();
            assertTrue(bounds.x() >= 0 && bounds.x() + bounds.width() <= recipe.getDisplayWidth());
            assertTrue(bounds.y() >= 0 && bounds.y() + bounds.height() <= recipe.getDisplayHeight());
        }
    }

    @Test
    void upgradesAreInstalledEquipmentRatherThanConsumedIngredients() {
        var rocket = EmiStack.of(Items.MINECART);
        var module = EmiStack.of(Items.CHEST);
        var recipe = new RocketGuideEmiRecipe(
                null,
                "upgrade_fixture",
                RocketGuideEmiRecipe.Kind.UPGRADE,
                rocket,
                module,
                RocketLayouts.UpgradeSlot.TANK,
                "Tank",
                "Capacity",
                "Equipment fixture",
                rocket,
                EmiStack.EMPTY,
                false);
        assertTrue(recipe.getInputs().isEmpty());
        assertEquals(2, recipe.getCatalysts().size());
        assertFalse(recipe.supportsRecipeTree());
        assertEquals(1, recipe.getOutputs().size());
    }

    @Test
    void fuelConsumesTheFuelItemAndReturnsItsContainerOnlyWhenApplicable() {
        for (boolean returnsBucket : List.of(false, true)) {
            var recipe = new RocketGuideEmiRecipe(
                    null,
                    "fuel_fixture",
                    RocketGuideEmiRecipe.Kind.FUEL,
                    EmiStack.of(Items.MINECART),
                    EmiStack.of(Items.LAVA_BUCKET),
                    RocketLayouts.UpgradeSlot.MOTOR,
                    "Fuel",
                    "Range",
                    "Fuel fixture",
                    EmiStack.of(Items.MINECART),
                    EmiStack.EMPTY,
                    returnsBucket);
            assertEquals(1, recipe.getInputs().size());
            assertEquals(1, recipe.getCatalysts().size());
            assertEquals(returnsBucket ? 2 : 1, recipe.getOutputs().size());
            if (returnsBucket)
                assertEquals(
                        Items.BUCKET,
                        recipe.getOutputs().getLast().getItemStack().getItem());
            assertFalse(recipe.supportsRecipeTree());
        }
    }

    @Test
    void upgradeAndFuelWidgetsFitIncludingTheirAccessInstructions() {
        for (var kind : List.of(RocketGuideEmiRecipe.Kind.UPGRADE, RocketGuideEmiRecipe.Kind.FUEL)) {
            var recipe = new RocketGuideEmiRecipe(
                    null,
                    "bounds_fixture",
                    kind,
                    EmiStack.of(Items.MINECART),
                    EmiStack.of(Items.LAVA_BUCKET),
                    RocketLayouts.UpgradeSlot.MOTOR,
                    "Fixture",
                    "Summary",
                    "Details",
                    EmiStack.of(Items.MINECART),
                    EmiStack.EMPTY,
                    true);
            var holder = new Holder(recipe.getDisplayWidth(), recipe.getDisplayHeight());
            recipe.addWidgets(holder);
            for (var widget : holder.widgets) {
                var bounds = widget.getBounds();
                assertTrue(bounds.x() >= 0 && bounds.x() + bounds.width() <= holder.width, kind + ": " + bounds);
                assertTrue(bounds.y() >= 0 && bounds.y() + bounds.height() <= holder.height, kind + ": " + bounds);
            }
        }
    }

    private static final class Holder implements WidgetHolder {
        private final int width;
        private final int height;
        final List<Widget> widgets = new ArrayList<>();

        Holder(int width, int height) {
            this.width = width;
            this.height = height;
        }

        @Override
        public int getWidth() {
            return width;
        }

        @Override
        public int getHeight() {
            return height;
        }

        @Override
        public <T extends Widget> T add(T widget) {
            widgets.add(widget);
            return widget;
        }
    }
}
