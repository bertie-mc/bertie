package com.berlord.emi.framework;

import dev.emi.emi.api.stack.EmiStack;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluids;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MachineDescriptorTest {

    @Test
    void ignoresEmptyEntriesAndCountsVisibleCells() {
        EmiStack item = EmiStack.of(Items.STONE);
        EmiStack fluid = EmiStack.of(Fluids.WATER, 1_000);
        MachineDescriptor descriptor = new MachineDescriptor()
                .itemIn(null)
                .itemIn(EmiStack.EMPTY)
                .itemIn(item)
                .fluidIn(fluid)
                .catalyst(item)
                .itemOut(item)
                .fluidOut(fluid)
                .info(null)
                .info(Component.literal("100 ticks"));

        assertEquals(3, descriptor.inputCells());
        assertEquals(2, descriptor.outputCells());
        assertTrue(descriptor.hasFluids());
        assertEquals(1, descriptor.info.size());
    }

    @Test
    void fluentMinimumWidthIsPreserved() {
        MachineDescriptor descriptor = new MachineDescriptor();
        assertSame(descriptor, descriptor.minWidth(144));
        assertEquals(144, descriptor.minWidth);
        assertFalse(descriptor.hasFluids());
    }
}
