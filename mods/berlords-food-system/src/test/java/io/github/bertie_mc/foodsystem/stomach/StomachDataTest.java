package io.github.bertie_mc.foodsystem.stomach;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.Test;

class StomachDataTest {

    @Test
    void slotActivityUsesTheTwoTickExpiryBoundary() {
        StomachData.FoodSlot slot = new StomachData.FoodSlot();
        slot.stack = new ItemStack(Items.APPLE);
        slot.duration = 1.99;
        assertFalse(slot.isActive());
        slot.duration = 2.0;
        assertTrue(slot.isActive());

        slot.healthBoost = 4;
        slot.regenFactor = 0.5;
        slot.eatOrder = 9;
        slot.clear();
        assertTrue(slot.isEmpty());
        assertEquals(0, slot.duration);
        assertEquals(0, slot.healthBoost);
        assertEquals(0, slot.regenFactor);
        assertEquals(0, slot.eatOrder);
    }

    @Test
    void respawnCopyDeepCopiesPersistentStateOnly() {
        StomachData source = new StomachData();
        source.slots[0].stack = new ItemStack(Items.GOLDEN_CARROT, 2);
        source.slots[0].duration = 800;
        source.slots[0].healthBoost = 6;
        source.slots[0].regenFactor = 0.25;
        source.slots[0].eatOrder = 3;
        source.unlockedSlots = 4;
        source.regenCooldown = 20;
        source.eatCounter = 12;
        source.regenCounter = 99;
        source.feedWakeStamp = 7;
        source.afterglow.add(new double[] {2, 400});

        StomachData copy = new StomachData();
        copy.copyFrom(source);

        assertNotSame(source.slots[0].stack, copy.slots[0].stack);
        assertEquals(2, copy.slots[0].stack.getCount());
        assertEquals(800, copy.slots[0].duration);
        assertEquals(6, copy.slots[0].healthBoost);
        assertEquals(0.25, copy.slots[0].regenFactor);
        assertEquals(3, copy.slots[0].eatOrder);
        assertEquals(4, copy.unlockedSlots);
        assertEquals(20, copy.regenCooldown);
        assertEquals(12, copy.eatCounter);
        assertEquals(0, copy.regenCounter);
        assertEquals(0, copy.feedWakeStamp);
        assertTrue(copy.afterglow.isEmpty());

        source.slots[0].stack.shrink(1);
        assertEquals(2, copy.slots[0].stack.getCount());
    }
}
