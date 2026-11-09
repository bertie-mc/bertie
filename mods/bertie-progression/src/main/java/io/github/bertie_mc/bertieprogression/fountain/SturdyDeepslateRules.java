package io.github.bertie_mc.bertieprogression.fountain;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.level.block.Block;

public final class SturdyDeepslateRules {
    public static final ResourceLocation ID = ResourceLocation.parse("anvilcraft:sturdy_deepslate");

    public static boolean applies(Block block) {
        return ID.equals(BuiltInRegistries.BLOCK.getKey(block));
    }

    public static float hardness(float original) {
        // Native Sturdy Deepslate has no effective pickaxe; Haste II multiplies its speed 1 by 1.4.
        return original * Tiers.NETHERITE.getSpeed() / 1.4F;
    }

    private SturdyDeepslateRules() {}
}
