package io.github.bertie_mc.mekanismcoversfix.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "mekanism.common.tile.transmitter.TileEntityTransmitter", priority = 900, remap = false)
public abstract class TileEntityTransmitterMixin extends BlockEntity {
    @Unique
    private String mekanismcoversfix$lastCoverState = "";

    protected TileEntityTransmitterMixin(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Inject(
            method = {"handleUpdateTag", "loadAdditional"},
            at = @At("TAIL"))
    private void mekanismcoversfix$refreshCoverModel(
            CompoundTag tag, HolderLookup.Provider provider, CallbackInfo callback) {
        String coverState = tag.getString("CoverState");
        if (coverState.equals(mekanismcoversfix$lastCoverState)) {
            return;
        }
        mekanismcoversfix$lastCoverState = coverState;
        if (level != null && level.isClientSide()) {
            // Cover packets do not change connections, so Mekanism skips its normal model refresh.
            // Queue fresh model data before rebuilding the section, including when the cover is removed.
            requestModelDataUpdate();
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_ALL);
        }
    }
}
