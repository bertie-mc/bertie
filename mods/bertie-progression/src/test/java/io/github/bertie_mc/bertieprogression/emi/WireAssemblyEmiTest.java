package io.github.bertie_mc.bertieprogression.emi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;

import dev.emi.emi.api.recipe.BasicEmiRecipe;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.SlotWidget;
import dev.emi.emi.api.widget.Widget;
import dev.emi.emi.api.widget.WidgetHolder;
import io.github.bertie_mc.bertieprogression.ModItems;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.Test;

class WireAssemblyEmiTest {
    private static final ResourceLocation WIRE = ResourceLocation.parse(
            "bertieemi:create_sequenced_assembly/bertieprogression/sequenced_assembly/copper_wire");

    @Test
    void displayIndexesGuaranteedWireAndBothWeightedOutcomes() {
        var weighted = List.of(EmiStack.of(ModItems.COPPER_WIRE.get()).setChance(0.3f),
                EmiStack.of(Items.COPPER_INGOT).setChance(0.7f));
        var outputs = WireAssemblyEmi.outputs(WIRE, weighted);
        assertEquals(3, outputs.size());
        assertEquals(ModItems.COPPER_WIRE.get(), outputs.getFirst().getItemStack().getItem());
        assertEquals(1, outputs.getFirst().getChance());
        assertEquals(0.3, outputs.get(1).getChance(), 0.00001);
        assertEquals(0.7, outputs.get(2).getChance(), 0.00001);
        assertEquals(2, weighted.size());
        assertSame(weighted, WireAssemblyEmi.outputs(ResourceLocation.parse("create:precision_mechanism"), weighted));
    }

    @Test
    void guaranteedSlotFitsAboveNativeResultsWithoutAddingProse() {
        var widgets = new ArrayList<Widget>();
        WidgetHolder holder = new WidgetHolder() {
            public int getWidth() { return 240; }
            public int getHeight() { return 40; }
            public <T extends Widget> T add(T widget) { widgets.add(widget); return widget; }
        };
        var recipe = new BasicEmiRecipe(null, WIRE, 240, 40) {
            @Override
            public void addWidgets(WidgetHolder ignored) {}
        };
        WireAssemblyEmi.decorate(recipe, holder);
        assertEquals(1, widgets.size());
        var slot = assertInstanceOf(SlotWidget.class, widgets.getFirst());
        assertEquals(202, slot.getBounds().x());
        assertEquals(2, slot.getBounds().y());
        assertEquals(18, slot.getBounds().width());
        assertEquals(18, slot.getBounds().height());
    }
}
