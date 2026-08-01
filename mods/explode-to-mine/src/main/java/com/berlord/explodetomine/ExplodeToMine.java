package com.berlord.explodetomine;

import net.minecraft.world.level.block.Blocks;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

@Mod(ExplodeToMine.MOD_ID)
public class ExplodeToMine {
    public static final String MOD_ID = "explodetomine";

    public ExplodeToMine(IEventBus modBus) {
        ModBlocks.BLOCKS.register(modBus);
        ModItems.ITEMS.register(modBus);

        // Each locked source needs a matching cracked block and its packaged resources.
        LockedOres.register(Blocks.DIAMOND_ORE, ModBlocks.CRACKED_DIAMOND_ORE);
        LockedOres.register(Blocks.DEEPSLATE_DIAMOND_ORE, ModBlocks.CRACKED_DEEPSLATE_DIAMOND_ORE);
    }
}
