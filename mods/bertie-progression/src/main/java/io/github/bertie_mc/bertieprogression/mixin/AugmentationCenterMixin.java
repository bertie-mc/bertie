package io.github.bertie_mc.bertieprogression.mixin;

import io.github.bertie_mc.bertieprogression.cybernetics.AugmentationCores;
import io.github.bertie_mc.bertieprogression.cybernetics.CenterEnergyAccess;
import io.github.bertie_mc.bertieprogression.cybernetics.ClosedEnergyStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import rearth.oritech.api.energy.EnergyApi;
import rearth.oritech.api.energy.containers.SimpleEnergyStorage;
import rearth.oritech.api.networking.NetworkedBlockEntity;
import rearth.oritech.block.entity.augmenter.AugmentApplicationEntity;

@Mixin(value = AugmentApplicationEntity.class, remap = false)
public abstract class AugmentationCenterMixin implements CenterEnergyAccess {
    @Shadow
    @Final
    private SimpleEnergyStorage energyStorage;

    @Unique
    private EnergyApi.EnergyStorage bertieprogression$closedEnergy;

    @Unique
    private boolean bertieprogression$legacyCoresChecked;

    @Override
    public EnergyApi.EnergyStorage bertieprogression$internalEnergy() {
        return energyStorage;
    }

    @Inject(
            method = {"getEnergyStorage", "getEnergyStorageForMultiblock"},
            at = @At("HEAD"),
            cancellable = true)
    private void bertieprogression$closeEnergy(Direction side, CallbackInfoReturnable<EnergyApi.EnergyStorage> cir) {
        if (bertieprogression$closedEnergy == null)
            bertieprogression$closedEnergy = new ClosedEnergyStorage(energyStorage);
        cir.setReturnValue(bertieprogression$closedEnergy);
    }

    @Inject(method = "serverTick", at = @At("HEAD"))
    private void bertieprogression$validateCasing(
            Level level, BlockPos pos, BlockState state, NetworkedBlockEntity entity, CallbackInfo ci) {
        if (!bertieprogression$legacyCoresChecked)
            bertieprogression$legacyCoresChecked =
                    AugmentationCores.restoreLegacy((AugmentApplicationEntity) (Object) this);
    }
}
