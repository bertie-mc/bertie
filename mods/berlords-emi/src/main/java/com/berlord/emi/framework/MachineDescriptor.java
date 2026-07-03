package com.berlord.emi.framework;

import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * A declarative description of one machine recipe: N item/fluid inputs, optional non-consumed
 * catalysts, M item/fluid outputs, and free-text info lines (time/xp/etc.). A per-mod module fills
 * one of these per recipe; {@link GenericEmiRecipe} turns it into widgets. Empty entries are dropped
 * by the {@code xIn/xOut} helpers so callers don't have to null-check.
 */
public class MachineDescriptor {
    public final List<EmiIngredient> itemInputs = new ArrayList<>();
    public final List<EmiIngredient> fluidInputs = new ArrayList<>();
    public final List<EmiIngredient> catalysts = new ArrayList<>();
    public final List<EmiStack> itemOutputs = new ArrayList<>();
    public final List<EmiStack> fluidOutputs = new ArrayList<>();
    public final List<Component> info = new ArrayList<>();

    public MachineDescriptor itemIn(EmiIngredient i) {
        if (i != null && !i.isEmpty()) itemInputs.add(i);
        return this;
    }

    public MachineDescriptor fluidIn(EmiIngredient s) {
        if (s != null && !s.isEmpty()) fluidInputs.add(s);
        return this;
    }

    public MachineDescriptor catalyst(EmiIngredient i) {
        if (i != null && !i.isEmpty()) catalysts.add(i);
        return this;
    }

    public MachineDescriptor itemOut(EmiStack s) {
        if (s != null && !s.isEmpty()) itemOutputs.add(s);
        return this;
    }

    public MachineDescriptor fluidOut(EmiStack s) {
        if (s != null && !s.isEmpty()) fluidOutputs.add(s);
        return this;
    }

    public MachineDescriptor info(Component c) {
        if (c != null) info.add(c);
        return this;
    }

    public boolean hasFluids() {
        return !fluidInputs.isEmpty() || !fluidOutputs.isEmpty();
    }

    public int inputCells() {
        return itemInputs.size() + fluidInputs.size() + catalysts.size();
    }

    public int outputCells() {
        return itemOutputs.size() + fluidOutputs.size();
    }
}
