package io.github.bertie_mc.bertieprogression.mixin;

import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** The native scan stream reuses a mutable cursor; map keys must be snapshots. */
@Pseudo
@Mixin(targets = "dev.compactmods.crafting.recipes.blocks.RecipeBlocks", remap = false)
public abstract class MiniRecipeScanPositionsMixin {
    @ModifyVariable(method = "lambda$create$1", at = @At("HEAD"), argsOnly = true, ordinal = 0, require = 1)
    private static BlockPos bertieprogression$immutablePosition(BlockPos position) {
        return position.immutable();
    }
}
