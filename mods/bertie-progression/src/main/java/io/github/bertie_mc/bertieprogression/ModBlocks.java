package io.github.bertie_mc.bertieprogression;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(BertieProgression.MODID);

    // The Echo Lock, this register's other block, went with the Echo and Below questline.

    /**
     * Eezo ore. It reads as bedrock at a glance and is meant to: it generates in the last few layers
     * of deepslate above the floor, where a player who is not looking for it walks past it.
     *
     * <p>Ancient debris' hardness and blast resistance, so no explosion opens it and nothing below a
     * netherite pickaxe drops it - the netherite floor is the {@code neoforge:needs_netherite_tool}
     * tag in this mod's data, which NeoForge already folds into every lower tier's
     * {@code incorrect_for_*_tool}.
     */
    public static final DeferredBlock<Block> EEZO_ORE = BLOCKS.registerSimpleBlock(
            "eezo_ore",
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_BLACK)
                    .strength(30.0F, 1200.0F)
                    .sound(SoundType.DEEPSLATE)
                    .requiresCorrectToolForDrops());

    private ModBlocks() {}
}
