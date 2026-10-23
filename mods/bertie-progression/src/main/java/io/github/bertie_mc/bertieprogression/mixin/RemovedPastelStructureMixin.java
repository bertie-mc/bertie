package io.github.bertie_mc.bertieprogression.mixin;

import io.github.bertie_mc.bertieprogression.worldgen.PastelStructureRemoval;
import net.minecraft.core.HolderGetter;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(StructureTemplate.class)
public abstract class RemovedPastelStructureMixin {
    @Inject(method = "load", at = @At("HEAD"), require = 1)
    private void bertie$removePastelContent(HolderGetter<Block> blocks, CompoundTag tag, CallbackInfo ci) {
        PastelStructureRemoval.clean(tag);
    }
}
