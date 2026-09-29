package io.github.bertie_mc.bertieprogression.combat;

import java.util.ArrayList;
import java.util.List;

public final class CombatHitState {
    private double energyRemaining = 1.0;
    private double flatSubtraction;
    private float healthBefore;
    private double extraLifeSteal;
    private boolean finished;
    private boolean applyingDefenses;
    private boolean beforeVanillaDamage;
    private boolean dikeProcessed;
    private boolean inMagicStage;

    private record Defense(int priority, Runnable apply) {}

    private final List<Defense> afterEnergy = new ArrayList<>();
    private final List<Runnable> debts = new ArrayList<>();
    private final List<Runnable> transfers = new ArrayList<>();
    private boolean applyingDebts;

    public void multiplyEnergy(double remaining) {
        energyRemaining *= Math.max(0, remaining);
    }

    public double energyRemaining() {
        return energyRemaining;
    }

    public void subtractLater(double amount) {
        flatSubtraction += Math.max(0, amount);
    }

    public double flatSubtraction() {
        return flatSubtraction;
    }

    public void recordHealth(float health) {
        healthBefore = health;
    }

    public float healthBefore() {
        return healthBefore;
    }

    public void addLifeSteal(double fraction) {
        extraLifeSteal += Math.max(0, fraction);
    }

    public double extraLifeSteal() {
        return extraLifeSteal;
    }

    public boolean beforeVanillaDamage() {
        return beforeVanillaDamage;
    }

    public void beforeVanillaDamage(boolean value) {
        beforeVanillaDamage = value;
    }

    public boolean dikeProcessed() {
        return dikeProcessed;
    }

    public void markDikeProcessed() {
        dikeProcessed = true;
    }

    public boolean inMagicStage() {
        return inMagicStage;
    }

    public void inMagicStage(boolean value) {
        inMagicStage = value;
    }

    public boolean finished() {
        return finished;
    }

    public void finish() {
        finished = true;
    }

    public boolean applyingDefenses() {
        return applyingDefenses;
    }

    public void deferDefense(Runnable defense) {
        deferDefense(10, defense);
    }

    public void deferDefense(int priority, Runnable defense) {
        afterEnergy.add(new Defense(priority, defense));
    }

    public void deferDebt(Runnable debt) {
        debts.add(debt);
    }

    public void deferTransfer(Runnable transfer) {
        transfers.add(transfer);
    }

    public void applyTransfers() {
        try {
            transfers.forEach(Runnable::run);
        } finally {
            transfers.clear();
        }
    }

    public boolean applyingDebts() {
        return applyingDebts;
    }

    public void applyDebts() {
        applyingDebts = true;
        try {
            debts.forEach(Runnable::run);
        } finally {
            applyingDebts = false;
            debts.clear();
        }
    }

    public void applyDefenses() {
        applyingDefenses = true;
        try {
            afterEnergy.sort(java.util.Comparator.comparingInt(Defense::priority));
            for (Defense defense : afterEnergy) {
                defense.apply().run();
            }
        } finally {
            applyingDefenses = false;
            afterEnergy.clear();
        }
    }
}
