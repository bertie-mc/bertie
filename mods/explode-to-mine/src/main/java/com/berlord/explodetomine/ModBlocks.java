package com.berlord.explodetomine;

import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DropExperienceBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(ExplodeToMine.MOD_ID);

    // The cracked, mineable twin of diamond ore.
    // - Copies diamond ore's properties (hardness, correct-tool requirement, sound).
    // - DropExperienceBlock gives the same XP range as vanilla diamond ore (3-7).
    // - Its loot table (data/.../cracked_diamond_ore.json) drops the diamonds.
    public static final DeferredBlock<Block> CRACKED_DIAMOND_ORE = BLOCKS.registerBlock(
            "cracked_diamond_ore",
            props -> new DropExperienceBlock(UniformInt.of(3, 7), props),
            BlockBehaviour.Properties.ofFullCopy(Blocks.DIAMOND_ORE)
    );

    public static final DeferredBlock<Block> CRACKED_DEEPSLATE_DIAMOND_ORE = BLOCKS.registerBlock(
            "cracked_deepslate_diamond_ore",
            props -> new DropExperienceBlock(UniformInt.of(3, 7), props),
            BlockBehaviour.Properties.ofFullCopy(Blocks.DEEPSLATE_DIAMOND_ORE)
    );

    private ModBlocks() {}
}
