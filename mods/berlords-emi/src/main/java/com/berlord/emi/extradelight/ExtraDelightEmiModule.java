package com.berlord.emi.extradelight;

import com.berlord.emi.framework.Categories;
import com.berlord.emi.framework.GenericEmiRecipe;
import com.berlord.emi.framework.MachineDescriptor;
import com.berlord.emi.framework.Recipes;
import com.lance5057.extradelight.workstations.chiller.ChillerRecipe;
import com.lance5057.extradelight.workstations.doughshaping.recipes.DoughShapingRecipe;
import com.lance5057.extradelight.workstations.dryingrack.DryingRackRecipe;
import com.lance5057.extradelight.workstations.juicer.JuicerRecipe;
import com.lance5057.extradelight.workstations.meltingpot.MeltingPotRecipe;
import com.lance5057.extradelight.workstations.mixingbowl.recipes.MixingBowlRecipe;
import com.lance5057.extradelight.workstations.mortar.recipes.MortarRecipe;
import com.lance5057.extradelight.workstations.oven.recipes.OvenRecipe;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.neoforge.NeoForgeEmiIngredient;
import dev.emi.emi.api.neoforge.NeoForgeEmiStack;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeManager;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient;

/** ExtraDelight workstations. Deferred (bespoke): feast, vat (multi-stage), evaporator (loot-table), bottle_fluid, tool_on_block. */
public final class ExtraDelightEmiModule {
    private ExtraDelightEmiModule() {
    }

    private static final RegistryAccess REG = RegistryAccess.EMPTY;

    public static void register(EmiRegistry reg) {
        RecipeManager rm = reg.getRecipeManager();

        EmiRecipeCategory oven = Categories.machine(reg, "extradelight_oven", "extradelight:oven", "Oven");
        Recipes.forEach(rm, OvenRecipe.class, (id, r) -> {
            MachineDescriptor d = new MachineDescriptor();
            for (Ingredient ing : r.getIngredients()) d.itemIn(EmiIngredient.of(ing));
            d.itemOut(EmiStack.of(r.output));
            cook(d, r.getCookTime(), r.getExperience());
            reg.addRecipe(new GenericEmiRecipe(oven, id, d));
        });

        EmiRecipeCategory mix = Categories.machine(reg, "extradelight_mixing_bowl", "extradelight:mixing_bowl", "Mixing Bowl");
        Recipes.forEach(rm, MixingBowlRecipe.class, (id, r) -> {
            MachineDescriptor d = new MachineDescriptor();
            for (Ingredient ing : r.getIngredients()) d.itemIn(EmiIngredient.of(ing));
            for (SizedFluidIngredient f : r.getFluids()) d.fluidIn(NeoForgeEmiIngredient.of(f));
            d.catalyst(EmiIngredient.of(r.getUtensil()));
            d.itemOut(EmiStack.of(r.getResultItem(REG)));
            reg.addRecipe(new GenericEmiRecipe(mix, id, d));
        });

        EmiRecipeCategory chill = Categories.machine(reg, "extradelight_chiller", "extradelight:chiller", "Chiller");
        Recipes.forEach(rm, ChillerRecipe.class, (id, r) -> {
            MachineDescriptor d = new MachineDescriptor();
            for (Ingredient ing : r.getIngredients()) d.itemIn(EmiIngredient.of(ing));
            fluidInIf(d, r.getFluid());
            d.itemOut(EmiStack.of(r.output));
            cook(d, r.getCookTime(), r.getExperience());
            reg.addRecipe(new GenericEmiRecipe(chill, id, d));
        });

        EmiRecipeCategory mortar = Categories.machine(reg, "extradelight_mortar", "extradelight:mortar_stone", "Mortar and Pestle");
        Recipes.forEach(rm, MortarRecipe.class, (id, r) -> {
            MachineDescriptor d = new MachineDescriptor();
            for (Ingredient ing : r.getIngredients()) d.itemIn(EmiIngredient.of(ing));
            d.itemOut(EmiStack.of(r.getResultItem(REG)));
            fluidOutIf(d, r.getFluid());
            reg.addRecipe(new GenericEmiRecipe(mortar, id, d));
        });

        EmiRecipeCategory dough = Categories.machine(reg, "extradelight_dough_shaping", "extradelight:dough_shaping", "Dough Shaping");
        Recipes.forEach(rm, DoughShapingRecipe.class, (id, r) -> {
            MachineDescriptor d = new MachineDescriptor();
            for (Ingredient ing : r.getIngredients()) d.itemIn(EmiIngredient.of(ing));
            d.itemOut(EmiStack.of(r.getResultItem(REG)));
            reg.addRecipe(new GenericEmiRecipe(dough, id, d));
        });

        EmiRecipeCategory melt = Categories.machine(reg, "extradelight_melting_pot", "extradelight:melting_pot", "Melting Pot");
        Recipes.forEach(rm, MeltingPotRecipe.class, (id, r) -> {
            MachineDescriptor d = new MachineDescriptor();
            d.itemIn(EmiIngredient.of(r.input));
            fluidOutIf(d, r.result);
            if (r.cooktime > 0) d.info(Component.literal(Categories.seconds(r.cooktime)));
            reg.addRecipe(new GenericEmiRecipe(melt, id, d));
        });

        EmiRecipeCategory juicer = Categories.machine(reg, "extradelight_juicer", "extradelight:juicer", "Juicer");
        Recipes.forEach(rm, JuicerRecipe.class, (id, r) -> {
            MachineDescriptor d = new MachineDescriptor();
            d.itemIn(EmiIngredient.of(r.getInput()));
            EmiStack out = EmiStack.of(r.getResultItem(REG));
            if (r.getChance() > 0 && r.getChance() < 100) out.setChance(r.getChance() / 100f);
            d.itemOut(out);
            fluidOutIf(d, r.getFluid());
            reg.addRecipe(new GenericEmiRecipe(juicer, id, d));
        });

        EmiRecipeCategory dry = Categories.machine(reg, "extradelight_drying_rack", "extradelight:drying_rack", "Drying Rack");
        Recipes.forEach(rm, DryingRackRecipe.class, (id, r) -> {
            MachineDescriptor d = new MachineDescriptor();
            for (Ingredient ing : r.getIngredients()) d.itemIn(EmiIngredient.of(ing));
            d.itemOut(EmiStack.of(r.getResultItem(REG)));
            if (r.getCookingTime() > 0) d.info(Component.literal(Categories.seconds(r.getCookingTime())));
            reg.addRecipe(new GenericEmiRecipe(dry, id, d));
        });
    }

    private static void cook(MachineDescriptor d, int time, float xp) {
        if (time > 0) d.info(Component.literal(Categories.seconds(time)));
        if (xp > 0) d.info(Component.literal(xp + " XP"));
    }

    private static void fluidInIf(MachineDescriptor d, FluidStack fs) {
        if (fs != null && !fs.isEmpty()) d.fluidIn(NeoForgeEmiStack.of(fs));
    }

    private static void fluidOutIf(MachineDescriptor d, FluidStack fs) {
        if (fs != null && !fs.isEmpty()) d.fluidOut(NeoForgeEmiStack.of(fs));
    }
}
