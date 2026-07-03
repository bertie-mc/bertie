package com.berlord.explodetomine;

import net.minecraft.world.item.BlockItem;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(ExplodeToMine.MOD_ID);

    // BlockItem for the twin so it exists as an item (silk-touch / pick-block / completeness).
    public static final DeferredItem<BlockItem> CRACKED_DIAMOND_ORE_ITEM =
            ITEMS.registerSimpleBlockItem(ModBlocks.CRACKED_DIAMOND_ORE);

    public static final DeferredItem<BlockItem> CRACKED_DEEPSLATE_DIAMOND_ORE_ITEM =
            ITEMS.registerSimpleBlockItem(ModBlocks.CRACKED_DEEPSLATE_DIAMOND_ORE);

    private ModItems() {}
}
