package io.github.bertie_mc.bertieprogression.cybernetics;

import rearth.oritech.api.energy.EnergyApi;

/** Exposes buffer telemetry without opening an external charging or export route. */
public final class ClosedEnergyStorage extends EnergyApi.EnergyStorage {
    private final EnergyApi.EnergyStorage delegate;

    public ClosedEnergyStorage(EnergyApi.EnergyStorage delegate) {
        this.delegate = delegate;
    }

    @Override
    public boolean supportsInsertion() {
        return false;
    }

    @Override
    public boolean supportsExtraction() {
        return false;
    }

    @Override
    public long insert(long amount, boolean simulate) {
        return 0;
    }

    @Override
    public long extract(long amount, boolean simulate) {
        return 0;
    }

    @Override
    public void setAmount(long amount) {}

    @Override
    public long getAmount() {
        return delegate.getAmount();
    }

    @Override
    public long getCapacity() {
        return delegate.getCapacity();
    }

    @Override
    public void update() {}
}
