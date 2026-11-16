package io.github.bertie_mc.bertieprogression.mixin;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Native cable connection updates keep the same item while changing its block ID. */
@Pseudo
@Mixin(targets = "dev.compactmods.crafting.recipes.components.BlockComponent", remap = false)
public abstract class MiniSuperconductorComponentMixin {
    @Shadow
    @Final
    private Block block;

    @Shadow
    @Final
    private java.util.Map<?, ?> filters;

    @Inject(method = "matches", at = @At("HEAD"), cancellable = true, require = 1)
    private void bertieprogression$superconductorVariant(BlockState state, CallbackInfoReturnable<Boolean> callback) {
        String expected = BuiltInRegistries.BLOCK.getKey(block).toString();
        String actual = BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();
        if (expected.equals(actual) || !filters.isEmpty()) return;
        if ((expected.equals("oritech:superconductor") || expected.equals("oritech:superconductor_connection"))
                && (actual.equals("oritech:superconductor") || actual.equals("oritech:superconductor_connection"))) {
            callback.setReturnValue(true);
        }
    }
}
