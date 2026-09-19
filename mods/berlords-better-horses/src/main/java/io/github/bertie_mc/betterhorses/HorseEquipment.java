package io.github.bertie_mc.betterhorses;

import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;

public interface HorseEquipment {
    SimpleContainer betterhorses$shoes();

    SimpleContainer betterhorses$saddleInventory();

    ItemStack betterhorses$syncedShoes();

    ItemStack betterhorses$syncedSaddle();

    default ShoeTier betterhorses$tier() {
        return betterhorses$syncedShoes().getItem() instanceof HorseshoeItem item ? item.tier : ShoeTier.NONE;
    }
}
