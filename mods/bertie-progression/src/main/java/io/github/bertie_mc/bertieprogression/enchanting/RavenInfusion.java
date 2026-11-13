package io.github.bertie_mc.bertieprogression.enchanting;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public final class RavenInfusion {
    public static final int LEVEL_COST = 200;
    private static final ResourceLocation INPUT =
            ResourceLocation.fromNamespaceAndPath("apothic_enchanting", "apothic_enchanting_table");
    private static final ResourceLocation OUTPUT =
            ResourceLocation.fromNamespaceAndPath("apothic_enchanting", "raven_enchanting_table");

    private RavenInfusion() {}

    public static boolean isInput(ItemStack stack) {
        return !stack.isEmpty() && INPUT.equals(BuiltInRegistries.ITEM.getKey(stack.getItem()));
    }

    public static boolean isOutput(ItemStack stack) {
        return !stack.isEmpty() && OUTPUT.equals(BuiltInRegistries.ITEM.getKey(stack.getItem()));
    }

    public static boolean matchesStats(float eterna, float quanta, float arcana) {
        // Summing shelf contributions can differ by a few float ULPs between insertion orders.
        return Math.abs(eterna - 99F) <= 0.0001F
                && Math.abs(quanta - 99.5F) <= 0.0001F
                && arcana >= 1.2F - 0.0001F
                && arcana <= 1.3F + 0.0001F;
    }
}
