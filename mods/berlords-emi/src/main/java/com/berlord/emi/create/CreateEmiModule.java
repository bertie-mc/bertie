package com.berlord.emi.create;

import com.berlord.emi.framework.GenericEmiCategory;
import com.berlord.emi.framework.GenericEmiRecipe;
import com.berlord.emi.framework.MachineDescriptor;
import com.simibubi.create.AllRecipeTypes;
import com.simibubi.create.content.kinetics.deployer.ItemApplicationRecipe;
import com.simibubi.create.content.processing.recipe.HeatCondition;
import com.simibubi.create.content.processing.recipe.ProcessingOutput;
import com.simibubi.create.content.processing.recipe.ProcessingRecipe;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.neoforge.NeoForgeEmiIngredient;
import dev.emi.emi.api.neoforge.NeoForgeEmiStack;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.recipe.VanillaEmiRecipeCategories;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient;

import java.util.List;
import java.util.Locale;

/**
 * Native EMI categories for Create's machine recipes. 13 of Create's types share the
 * {@link ProcessingRecipe} base, so one {@link #toDescriptor} mapper covers them all
 * (~1000 recipes). Deploying / item application use {@link ItemApplicationRecipe}'s explicit
 * base/held accessors so the held item can show as a catalyst when it isn't consumed.
 *
 * <p>Not yet ported (bespoke layouts): {@code mechanical_crafting} (NxN grid) and
 * {@code sequenced_assembly} (multi-step). Only ever called when {@code create} is loaded.
 */
public final class CreateEmiModule {
    private CreateEmiModule() {
    }

    private static final String NS = "berlords_emi";

    public static void register(EmiRegistry reg) {
        RecipeManager rm = reg.getRecipeManager();

        processing(reg, rm, AllRecipeTypes.MILLING, "create_milling", "millstone", "Milling");
        processing(reg, rm, AllRecipeTypes.CRUSHING, "create_crushing", "crushing_wheel", "Crushing Wheels");
        processing(reg, rm, AllRecipeTypes.PRESSING, "create_pressing", "mechanical_press", "Pressing");
        processing(reg, rm, AllRecipeTypes.MIXING, "create_mixing", "mechanical_mixer", "Mixing");
        processing(reg, rm, AllRecipeTypes.COMPACTING, "create_compacting", "mechanical_press", "Compacting");
        processing(reg, rm, AllRecipeTypes.CUTTING, "create_cutting", "mechanical_saw", "Cutting");
        processing(reg, rm, AllRecipeTypes.SPLASHING, "create_splashing", "encased_fan", "Bulk Washing");
        processing(reg, rm, AllRecipeTypes.HAUNTING, "create_haunting", "encased_fan", "Bulk Haunting");
        processing(reg, rm, AllRecipeTypes.SANDPAPER_POLISHING, "create_sandpaper", "sand_paper", "Sandpaper Polishing");
        processing(reg, rm, AllRecipeTypes.FILLING, "create_filling", "spout", "Filling");
        processing(reg, rm, AllRecipeTypes.EMPTYING, "create_emptying", "item_drain", "Emptying");

        application(reg, rm, AllRecipeTypes.DEPLOYING, "create_deploying", "deployer", "Deploying");
        application(reg, rm, AllRecipeTypes.ITEM_APPLICATION, "create_item_application", "depot", "Item Application");

        // "Bulk" fan processing reuses VANILLA recipes that EMI already shows, so we don't make new
        // categories — we just register the Encased Fan as a workstation on them. Per Create's fan types:
        // fan+lava (Bulk Blasting) applies BLASTING + SMELTING + SMOKING; fan+fire (Bulk Smoking) applies
        // SMOKING. (Bulk Washing/Haunting are the custom splashing/haunting categories above.)
        EmiIngredient fan = createStack("encased_fan");
        if (!fan.isEmpty()) {
            reg.addWorkstation(VanillaEmiRecipeCategories.SMELTING, fan);
            reg.addWorkstation(VanillaEmiRecipeCategories.BLASTING, fan);
            reg.addWorkstation(VanillaEmiRecipeCategories.SMOKING, fan);
        }
    }

    /** The 11 plain ProcessingRecipe categories. */
    private static void processing(EmiRegistry reg, RecipeManager rm, AllRecipeTypes type,
                                   String key, String workstation, String name) {
        EmiRecipeCategory cat = category(reg, key, workstation, name);
        for (RecipeHolder<?> h : recipesFor(rm, type)) {
            if (!(h.value() instanceof ProcessingRecipe<?, ?> r)) {
                continue;
            }
            try {
                reg.addRecipe(new GenericEmiRecipe(cat, h.id(), toDescriptor(r)));
            } catch (Throwable ignored) {
            }
        }
    }

    /** Deployer / manual application: base item + held item (catalyst if kept) -> output. */
    private static void application(EmiRegistry reg, RecipeManager rm, AllRecipeTypes type,
                                    String key, String workstation, String name) {
        EmiRecipeCategory cat = category(reg, key, workstation, name);
        for (RecipeHolder<?> h : recipesFor(rm, type)) {
            if (!(h.value() instanceof ItemApplicationRecipe r)) {
                continue;
            }
            try {
                MachineDescriptor d = new MachineDescriptor();
                d.itemIn(EmiIngredient.of(r.getProcessedItem()));
                EmiIngredient held = EmiIngredient.of(r.getRequiredHeldItem());
                if (r.shouldKeepHeldItem()) {
                    d.catalyst(held);
                } else {
                    d.itemIn(held);
                }
                outputs(d, r);
                info(d, r);
                reg.addRecipe(new GenericEmiRecipe(cat, h.id(), d));
            } catch (Throwable ignored) {
            }
        }
    }

    private static MachineDescriptor toDescriptor(ProcessingRecipe<?, ?> r) {
        MachineDescriptor d = new MachineDescriptor();
        for (Ingredient ing : r.getIngredients()) {
            d.itemIn(EmiIngredient.of(ing));
        }
        for (SizedFluidIngredient f : r.getFluidIngredients()) {
            d.fluidIn(NeoForgeEmiIngredient.of(f));
        }
        outputs(d, r);
        info(d, r);
        return d;
    }

    private static void outputs(MachineDescriptor d, ProcessingRecipe<?, ?> r) {
        for (ProcessingOutput o : r.getRollableResults()) {
            d.itemOut(EmiStack.of(o.getStack()).setChance(o.getChance()));
        }
        for (FluidStack f : r.getFluidResults()) {
            d.fluidOut(NeoForgeEmiStack.of(f));
        }
    }

    private static void info(MachineDescriptor d, ProcessingRecipe<?, ?> r) {
        int t = r.getProcessingDuration();
        if (t > 0) {
            d.info(Component.literal(seconds(t)));
        }
        HeatCondition heat = r.getRequiredHeat();
        if (heat != null && heat != HeatCondition.NONE) {
            d.info(Component.literal(capitalize(heat.getSerializedName())));
        }
    }

    private static EmiRecipeCategory category(EmiRegistry reg, String key, String workstation, String name) {
        EmiStack icon = createStack(workstation);
        GenericEmiCategory cat = new GenericEmiCategory(
                ResourceLocation.fromNamespaceAndPath(NS, key), icon, Component.literal(name));
        reg.addCategory(cat);
        if (!icon.isEmpty()) {
            reg.addWorkstation(cat, icon);
        }
        return cat;
    }

    private static EmiStack createStack(String path) {
        Item item = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("create", path));
        return EmiStack.of(item);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static List<RecipeHolder<?>> recipesFor(RecipeManager rm, AllRecipeTypes type) {
        RecipeType rt = type.getType();
        List raw = rm.getAllRecipesFor(rt);
        return (List<RecipeHolder<?>>) raw;
    }

    private static String seconds(int ticks) {
        double s = ticks / 20.0;
        return (s == Math.floor(s) ? String.valueOf((long) s) : String.format(Locale.ROOT, "%.1f", s)) + "s";
    }

    private static String capitalize(String s) {
        return s == null || s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
