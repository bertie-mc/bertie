package io.github.bertie_mc.bertieprogression.mixin;

import net.minecraft.world.level.levelgen.placement.RarityFilter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(RarityFilter.class)
public interface FountainRarityAccessor {
    @Accessor("chance")
    int bertieprogression$chance();
}
