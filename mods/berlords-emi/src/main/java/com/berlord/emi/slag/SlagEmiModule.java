package com.berlord.emi.slag;

import com.berlord.emi.framework.GenericEmiCategory;
import com.berlord.emi.framework.GenericEmiRecipe;
import com.berlord.emi.framework.MachineDescriptor;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.neoforge.NeoForgeEmiStack;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.lopyluna.slag.content.blocks.basin.BasinCastingRecipe;
import dev.lopyluna.slag.content.blocks.crucible.AlloyingRecipe;
import dev.lopyluna.slag.content.blocks.forge.DoubleSmeltingRecipe;
import dev.lopyluna.slag.content.blocks.melter.MeltingRecipe;
import dev.lopyluna.slag.content.blocks.table.TableCastingRecipe;
import dev.lopyluna.slag.register.AllRecipes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.registries.DeferredHolder;

import java.util.List;
import java.util.Locale;
import java.util.function.Function;

/**
 * Native EMI categories for Slag 'n' Embers' 5 machine recipe types. Recipes are read from the live
 * {@link RecipeManager} (never re-parsed), each mapped to a {@link MachineDescriptor}. Only ever
 * called when {@code slag} is loaded, so referencing Slag classes here is safe.
 */
public final class SlagEmiModule {
    private SlagEmiModule() {
    }

    private static final String NS = "berlords_emi";

    private static GenericEmiCategory category(String key, String iconItem, String nameKey) {
        return new GenericEmiCategory(
                ResourceLocation.fromNamespaceAndPath(NS, key),
                slagStack(iconItem),
                Component.translatable(nameKey));
    }

    public static void register(EmiRegistry registry) {
        RecipeManager rm = registry.getRecipeManager();

        GenericEmiCategory doubleSmelting = category("slag_double_smelting", "brick_forge", "emi.category.berlords_emi.slag_double_smelting");
        GenericEmiCategory alloying = category("slag_alloying", "crucible", "emi.category.berlords_emi.slag_alloying");
        GenericEmiCategory melting = category("slag_melting", "melter", "emi.category.berlords_emi.slag_melting");
        GenericEmiCategory tableCasting = category("slag_table_casting", "table", "emi.category.berlords_emi.slag_table_casting");
        GenericEmiCategory basinCasting = category("slag_basin_casting", "basin", "emi.category.berlords_emi.slag_basin_casting");

        registry.addCategory(doubleSmelting);
        registry.addCategory(alloying);
        registry.addCategory(melting);
        registry.addCategory(tableCasting);
        registry.addCategory(basinCasting);

        addWorkstation(registry, doubleSmelting, "brick_forge");
        addWorkstation(registry, alloying, "crucible");
        addWorkstation(registry, melting, "melter");
        addWorkstation(registry, tableCasting, "table");
        addWorkstation(registry, tableCasting, "sandstone_mold");
        addWorkstation(registry, tableCasting, "terracotta_mold");
        addWorkstation(registry, basinCasting, "basin");

        // 1. Brick Forge — two items in, one item out (with cook time + xp).
        emit(registry, rm, AllRecipes.DOUBLE_SMELTING, doubleSmelting, DoubleSmeltingRecipe.class, r -> {
            MachineDescriptor d = new MachineDescriptor();
            d.itemIn(EmiIngredient.of(r.getInputA()));
            d.itemIn(EmiIngredient.of(r.getInputB()));
            d.itemOut(EmiStack.of(r.getOutput()));
            String time = formatSeconds(r.getCookingTime());
            if (time != null) d.info(Component.literal(time));
            if (r.getExperience() > 0) d.info(Component.literal(formatNumber(r.getExperience()) + " XP"));
            return d;
        });

        // 2. Crucible — fluids in, fluid out.
        emit(registry, rm, AllRecipes.ALLOYING, alloying, AlloyingRecipe.class, r -> {
            MachineDescriptor d = new MachineDescriptor();
            for (FluidStack fs : r.getInputs()) d.fluidIn(NeoForgeEmiStack.of(fs));
            d.fluidOut(NeoForgeEmiStack.of(r.getOutput()));
            return d;
        });

        // 3. Melter — one item in (Ingredient and/or exact stacks), fluid(s) out. Biggest category.
        emit(registry, rm, AllRecipes.MELTING, melting, MeltingRecipe.class, r -> {
            MachineDescriptor d = new MachineDescriptor();
            Ingredient ing = r.getInput();
            if (ing != null && !ing.isEmpty()) d.itemIn(EmiIngredient.of(ing));
            for (ItemStack st : r.getInputs()) d.itemIn(EmiStack.of(st)); // preserves components (dynamic_part)
            for (FluidStack fs : r.getOutputs()) d.fluidOut(NeoForgeEmiStack.of(fs));
            return d;
        });

        // 4. Casting Table — fluid in + reusable cast (catalyst) -> item out.
        emit(registry, rm, AllRecipes.TABLE_CASTING, tableCasting, TableCastingRecipe.class, r -> {
            MachineDescriptor d = new MachineDescriptor();
            d.fluidIn(NeoForgeEmiStack.of(r.getInput()));
            d.catalyst(castIngredient(r.getCastType())); // guarded: ~9 of 15 cast tags don't ship
            d.itemOut(EmiStack.of(r.getOutput()));
            return d;
        });

        // 5. Casting Basin — fluid in -> item out, no cast.
        emit(registry, rm, AllRecipes.BASIN_CASTING, basinCasting, BasinCastingRecipe.class, r -> {
            MachineDescriptor d = new MachineDescriptor();
            d.fluidIn(NeoForgeEmiStack.of(r.getInput()));
            d.itemOut(EmiStack.of(r.getOutput()));
            return d;
        });
    }

    /** Read every recipe of {@code typeHolder}, map it to a descriptor, and register a generic recipe. */
    private static <R> void emit(EmiRegistry registry, RecipeManager rm,
                                 DeferredHolder<RecipeType<?>, ?> typeHolder, EmiRecipeCategory category,
                                 Class<R> recipeClass, Function<R, MachineDescriptor> mapper) {
        for (RecipeHolder<?> holder : recipesFor(rm, typeHolder)) {
            Object value = holder.value();
            if (!recipeClass.isInstance(value)) continue;
            try {
                MachineDescriptor d = mapper.apply(recipeClass.cast(value));
                registry.addRecipe(new GenericEmiRecipe(category, holder.id(), d));
            } catch (Throwable ignored) {
                // one malformed recipe must not sink the whole category
            }
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static List<RecipeHolder<?>> recipesFor(RecipeManager rm, DeferredHolder<RecipeType<?>, ?> typeHolder) {
        RecipeType type = typeHolder.get();
        List raw = rm.getAllRecipesFor(type);
        return (List<RecipeHolder<?>>) raw;
    }

    private static void addWorkstation(EmiRegistry registry, EmiRecipeCategory category, String itemPath) {
        EmiStack icon = slagStack(itemPath);
        if (!icon.isEmpty()) {
            registry.addWorkstation(category, icon);
        }
    }

    private static EmiStack slagStack(String itemPath) {
        Item item = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("slag", itemPath));
        return EmiStack.of(item);
    }

    /** Build a non-consumed catalyst from a cast item-tag; null if the tag is missing/empty. */
    private static EmiIngredient castIngredient(TagKey<Item> tag) {
        if (tag == null) {
            return null;
        }
        var opt = BuiltInRegistries.ITEM.getTag(tag);
        if (opt.isEmpty() || opt.get().size() == 0) {
            return null;
        }
        return EmiIngredient.of(tag);
    }

    private static String formatSeconds(int ticks) {
        if (ticks <= 0) {
            return null;
        }
        double s = ticks / 20.0;
        return (s == Math.floor(s) ? String.valueOf((long) s) : String.format(Locale.ROOT, "%.1f", s)) + "s";
    }

    private static String formatNumber(float f) {
        return f == Math.floor(f) ? String.valueOf((long) f) : String.format(Locale.ROOT, "%.1f", f);
    }
}
