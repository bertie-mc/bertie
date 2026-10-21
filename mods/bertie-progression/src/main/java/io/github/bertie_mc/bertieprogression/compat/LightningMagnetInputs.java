package io.github.bertie_mc.bertieprogression.compat;

import io.github.bertie_mc.bertieprogression.BertieProgression;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public final class LightningMagnetInputs {
    private static final TagKey<Block> INPUTS = TagKey.create(
            Registries.BLOCK,
            ResourceLocation.fromNamespaceAndPath(BertieProgression.MODID, "lightning_magnet_inputs"));

    public static boolean accepts(BlockState state) {
        return state.is(INPUTS);
    }

    private LightningMagnetInputs() {}
}
