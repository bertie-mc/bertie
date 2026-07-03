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

        // Declare which existing ores are "locked" and what cracked twin they become when exploded.
        // TEST TARGET: vanilla diamond ore -> our cracked twin.
        // To add more later: register the source block + a matching twin in ModBlocks.
        LockedOres.register(Blocks.DIAMOND_ORE, ModBlocks.CRACKED_DIAMOND_ORE);
        LockedOres.register(Blocks.DEEPSLATE_DIAMOND_ORE, ModBlocks.CRACKED_DEEPSLATE_DIAMOND_ORE);
    }
}
