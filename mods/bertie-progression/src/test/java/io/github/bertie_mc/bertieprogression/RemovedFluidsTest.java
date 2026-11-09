package io.github.bertie_mc.bertieprogression;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import mekanism.api.MekanismAPI;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.material.Fluids;
import org.junit.jupiter.api.Test;

class RemovedFluidsTest {
    @Test
    void liquidRemovalDoesNotRemoveTheChemicalWithTheSameIdentifier() {
        var id = ResourceLocation.parse("mekanism:sulfuric_acid");
        assertTrue(BuiltInRegistries.FLUID.containsKey(id));
        assertTrue(RemovedFluids.isRemoved(BuiltInRegistries.FLUID.get(id)));
        assertTrue(RemovedFluids.isRemoved(
                BuiltInRegistries.FLUID.get(ResourceLocation.parse("mekanism:flowing_sulfuric_acid"))));
        assertTrue(MekanismAPI.CHEMICAL_REGISTRY.containsKey(id));
        assertFalse(RemovedFluids.isRemoved(MekanismAPI.CHEMICAL_REGISTRY.get(id)));
        assertFalse(RemovedFluids.isRemoved(id));
        assertFalse(RemovedFluids.isRemoved(Fluids.WATER));
    }
}
