package io.github.bertie_mc.bertieprogression;

import java.util.Set;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.material.Fluid;

/** Viewer suppression for retired liquids, independent of chemical registry identities. */
public final class RemovedFluids {
    private static final Set<ResourceLocation> IDS = Set.copyOf(RemovedItems.read("/removed_fluids.json"));

    public static boolean isRemoved(Object key) {
        return key instanceof Fluid fluid && IDS.contains(BuiltInRegistries.FLUID.getKey(fluid));
    }

    private RemovedFluids() {}
}
