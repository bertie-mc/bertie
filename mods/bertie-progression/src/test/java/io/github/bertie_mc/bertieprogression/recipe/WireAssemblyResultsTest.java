package io.github.bertie_mc.bertieprogression.recipe;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import io.github.bertie_mc.bertieprogression.ModItems;
import java.util.List;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.Test;

class WireAssemblyResultsTest {
    private static final ResourceLocation RECIPE =
            ResourceLocation.parse("bertieprogression:sequenced_assembly/copper_wire");

    @Test
    void completionKeepsGuaranteedWireForEitherWeightedResult() {
        for (var byproduct : List.of(Items.COPPER_INGOT, ModItems.COPPER_WIRE.get())) {
            var rolled = new ItemStack(byproduct);
            WireAssemblyResults.mark(RECIPE, rolled, true);
            var results = WireAssemblyResults.expand(List.of(rolled));
            assertEquals(2, results.size());
            assertEquals(ModItems.COPPER_WIRE.get(), results.getFirst().getItem());
            assertEquals(byproduct, results.getLast().getItem());
            assertFalse(rolled.has(DataComponents.CUSTOM_DATA));
            assertEquals(2, WireAssemblyResults.expand(results).size(), "bonus cannot repeat");
        }
    }

    @Test
    void intermediateStepsAndUnrelatedRecipesDoNotProduceBonusWire() {
        var unfinished = new ItemStack(ModItems.INCOMPLETE_COPPER_WIRE.get());
        WireAssemblyResults.mark(RECIPE, unfinished, false);
        assertEquals(1, WireAssemblyResults.expand(List.of(unfinished)).size());
        var unrelated = new ItemStack(Items.COPPER_INGOT);
        WireAssemblyResults.mark(ResourceLocation.parse("create:sequenced_assembly/copper_wire"), unrelated, true);
        assertEquals(1, WireAssemblyResults.expand(List.of(unrelated)).size());
        assertFalse(unrelated.has(DataComponents.CUSTOM_DATA));
    }
}
