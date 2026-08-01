package com.berlord.hephaestusarchitecture.mixin;

import com.berlord.hephaestusarchitecture.structure.ForgeLayout;
import com.berlord.hephaestusarchitecture.structure.ForgeLayouts;
import com.berlord.hephaestusarchitecture.structure.PedestalRouter;
import com.stal111.forbidden_arcanus.common.block.HephaestusForgeBlock;
import com.stal111.forbidden_arcanus.common.block.entity.forge.HephaestusForgeBlockEntity;
import com.stal111.forbidden_arcanus.common.block.entity.forge.HephaestusForgeLevel;
import com.stal111.forbidden_arcanus.common.block.properties.ModBlockStateProperties;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(HephaestusForgeBlock.class)
public abstract class HephaestusForgeBlockMixin {

    @Shadow(remap = false)
    @Final
    private HephaestusForgeLevel level;

    @Inject(method = "updateState", at = @At("HEAD"), cancellable = true, remap = false)
    private void hephaestusarchitecture$validateTierLayout(BlockState state,
                                                            Level level,
                                                            BlockPos pos,
                                                            CallbackInfo ci) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        ForgeLayout.Match match = ForgeLayouts.match(serverLevel, pos, this.level.getAsInt());
        boolean valid = match != null;

        if (state.getValue(ModBlockStateProperties.ACTIVATED) != valid) {
            level.setBlockAndUpdate(pos, state.setValue(ModBlockStateProperties.ACTIVATED, valid));
        }

        if (serverLevel.getBlockEntity(pos) instanceof HephaestusForgeBlockEntity forge) {
            if (match != null) {
                PedestalRouter.rebuild(serverLevel, pos, forge, match);
            } else {
                PedestalRouter.clear(serverLevel, forge);
            }
        }

        ci.cancel();
    }

    /**
     * F&A's interaction methods retain the pre-validation BlockState in a local
     * variable. Without this guard, the same gavel click that discovers a broken
     * multiblock could still start one ritual before the deactivation sync lands.
     */
    @Inject(method = "useItemOn", at = @At("HEAD"), cancellable = true, remap = false)
    private void hephaestusarchitecture$rejectRitualOnInvalidLayout(ItemStack stack,
                                                                    BlockState state,
                                                                    Level level,
                                                                    BlockPos pos,
                                                                    Player player,
                                                                    InteractionHand hand,
                                                                    BlockHitResult hit,
                                                                    CallbackInfoReturnable<ItemInteractionResult> cir) {
        if (level instanceof ServerLevel serverLevel
                && ForgeLayouts.match(serverLevel, pos, this.level.getAsInt()) == null) {
            if (state.getValue(ModBlockStateProperties.ACTIVATED)) {
                level.setBlockAndUpdate(pos, state.setValue(ModBlockStateProperties.ACTIVATED, false));
            }
            if (serverLevel.getBlockEntity(pos) instanceof HephaestusForgeBlockEntity forge) {
                PedestalRouter.clear(serverLevel, forge);
            }
            cir.setReturnValue(ItemInteractionResult.FAIL);
        }
    }

    @Inject(method = "useWithoutItem", at = @At("HEAD"), cancellable = true, remap = false)
    private void hephaestusarchitecture$rejectMenuOnInvalidLayout(BlockState state,
                                                                  Level level,
                                                                  BlockPos pos,
                                                                  Player player,
                                                                  BlockHitResult hit,
                                                                  CallbackInfoReturnable<InteractionResult> cir) {
        if (level instanceof ServerLevel serverLevel
                && ForgeLayouts.match(serverLevel, pos, this.level.getAsInt()) == null) {
            if (state.getValue(ModBlockStateProperties.ACTIVATED)) {
                level.setBlockAndUpdate(pos, state.setValue(ModBlockStateProperties.ACTIVATED, false));
            }
            if (serverLevel.getBlockEntity(pos) instanceof HephaestusForgeBlockEntity forge) {
                PedestalRouter.clear(serverLevel, forge);
            }
            cir.setReturnValue(InteractionResult.FAIL);
        }
    }
}
