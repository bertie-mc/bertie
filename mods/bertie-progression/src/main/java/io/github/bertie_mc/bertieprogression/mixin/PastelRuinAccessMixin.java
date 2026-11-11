package io.github.bertie_mc.bertieprogression.mixin;

import io.github.bertie_mc.bertieprogression.worldgen.PastelStructureRemoval;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(
        targets = {
            "earth.terrarium.pastel.blocks.structure.PreservationControllerBlockEntity",
            "earth.terrarium.pastel.blocks.structure.TreasureChestBlockEntity"
        },
        remap = false)
public abstract class PastelRuinAccessMixin {
    @Inject(method = "loadAdditional", at = @At("HEAD"), require = 1)
    private void bertie$clearRetiredProgressionGate(
            CompoundTag tag, HolderLookup.Provider registries, CallbackInfo ci) {
        PastelStructureRemoval.clearObsoleteAccess(tag);
    }
}
