package com.berlord.emi.anvilcraft;

import com.berlord.emi.framework.Categories;
import com.berlord.emi.framework.GenericEmiRecipe;
import com.berlord.emi.framework.MachineDescriptor;
import com.berlord.emi.framework.Recipes;
import dev.anvilcraft.lib.recipe.component.BlockStatePredicate;
import dev.anvilcraft.lib.recipe.component.ChanceBlockState;
import dev.anvilcraft.lib.recipe.component.ChanceItemStack;
import dev.anvilcraft.lib.recipe.component.ItemIngredientPredicate;
import dev.dubhe.anvilcraft.recipe.component.HasCauldronSimple;
import dev.dubhe.anvilcraft.recipe.ChargerChargingRecipe;
import dev.dubhe.anvilcraft.recipe.JewelCraftingRecipe;
import dev.dubhe.anvilcraft.recipe.anvil.StampingUniqueItemsRecipe;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.AbstractProcessRecipe;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.BlockCompressRecipe;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.BlockCrushRecipe;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.BlockSmearRecipe;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.BoilingRecipe;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.BulgingRecipe;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.CookingRecipe;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.ItemCompressRecipe;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.ItemCrushRecipe;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.MeshRecipe;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.NeutronIrradiationRecipe;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.StampingRecipe;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.SuperHeatingRecipe;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.TimeWarpRecipe;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.UnpackRecipe;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

import java.util.ArrayList;
import java.util.List;

/**
 * AnvilCraft — recipes triggered by a falling anvil. Item-based and block-based "process" types share
 * mappers (inputs/outputs via anvillib predicate/chance components; cauldron fluids via resource ids).
 * Deferred (bespoke): mob_transform(_with_item), mineral_fountain(_chance), multiblock(_conversion),
 * mass_inject (no item output), anvil_collision, item_inject (mixed), squeezing, two/four_to_one_smithing.
 * Most have no machine block, so the workstation falls back to the Anvil.
 */
public final class AnvilCraftEmiModule {
    private AnvilCraftEmiModule() {
    }

    private static final String ANVIL = "minecraft:anvil";
    private static final String CAULDRON = "minecraft:cauldron";

    public static void register(EmiRegistry reg) {
        RecipeManager rm = reg.getRecipeManager();

        itemProcess(reg, rm, BulgingRecipe.class, "anvilcraft_bulging", CAULDRON, "Bulging");
        itemProcess(reg, rm, ItemCrushRecipe.class, "anvilcraft_item_crush", "anvilcraft:crushing_table", "Item Crushing");
        itemProcess(reg, rm, SuperHeatingRecipe.class, "anvilcraft_super_heating", CAULDRON, "Super Heating");
        itemProcess(reg, rm, TimeWarpRecipe.class, "anvilcraft_time_warp", ANVIL, "Time Warp");
        itemProcess(reg, rm, StampingRecipe.class, "anvilcraft_stamping", "anvilcraft:stamping_platform", "Stamping");
        itemProcess(reg, rm, UnpackRecipe.class, "anvilcraft_unpack", ANVIL, "Unpacking");
        itemProcess(reg, rm, MeshRecipe.class, "anvilcraft_mesh", ANVIL, "Mesh Sifting");
        itemProcess(reg, rm, ItemCompressRecipe.class, "anvilcraft_item_compress", ANVIL, "Item Compress");
        itemProcess(reg, rm, NeutronIrradiationRecipe.class, "anvilcraft_neutron", ANVIL, "Neutron Irradiation");
        itemProcess(reg, rm, CookingRecipe.class, "anvilcraft_cooking", ANVIL, "Anvil Cooking");
        itemProcess(reg, rm, BoilingRecipe.class, "anvilcraft_boiling", CAULDRON, "Boiling");

        blockProcess(reg, rm, BlockSmearRecipe.class, "anvilcraft_block_smear", ANVIL, "Block Smear");
        blockProcess(reg, rm, BlockCrushRecipe.class, "anvilcraft_block_crush", ANVIL, "Block Crushing");
        blockProcess(reg, rm, BlockCompressRecipe.class, "anvilcraft_block_compress", ANVIL, "Block Compress");

        EmiRecipeCategory jewel = Categories.machine(reg, "anvilcraft_jewel", "anvilcraft:jewelcrafting_table", "Jewel Crafting");
        Recipes.forEach(rm, JewelCraftingRecipe.class, (id, r) -> {
            MachineDescriptor d = new MachineDescriptor();
            for (Ingredient ing : r.getIngredients()) d.itemIn(EmiIngredient.of(ing));
            d.itemOut(EmiStack.of(r.getResult()));
            reg.addRecipe(new GenericEmiRecipe(jewel, id, d));
        });

        EmiRecipeCategory su = Categories.machine(reg, "anvilcraft_stamping_unique", "anvilcraft:stamping_platform", "Stamping (Unique)");
        Recipes.forEach(rm, StampingUniqueItemsRecipe.class, (id, r) -> {
            MachineDescriptor d = new MachineDescriptor();
            for (Ingredient ing : r.getIngredients()) d.itemIn(EmiIngredient.of(ing));
            for (ChanceItemStack c : r.getResults()) out(d, c);
            reg.addRecipe(new GenericEmiRecipe(su, id, d));
        });

        EmiRecipeCategory charger = Categories.machine(reg, "anvilcraft_charger", ANVIL, "Charger Charging");
        Recipes.forEach(rm, ChargerChargingRecipe.class, (id, r) -> {
            MachineDescriptor d = new MachineDescriptor();
            d.itemIn(EmiIngredient.of(r.getIngredient()));
            d.itemOut(EmiStack.of(r.getResult()));
            if (r.getTime() > 0) d.info(Component.literal(Categories.seconds(r.getTime())));
            reg.addRecipe(new GenericEmiRecipe(charger, id, d));
        });
    }

    private static <R extends AbstractProcessRecipe<?>> void itemProcess(
            EmiRegistry reg, RecipeManager rm, Class<R> cls, String key, String ws, String name) {
        EmiRecipeCategory cat = Categories.machine(reg, key, ws, name);
        Recipes.forEach(rm, cls, (id, r) -> {
            MachineDescriptor d = new MachineDescriptor();
            for (ItemIngredientPredicate p : r.getInputItems()) {
                EmiIngredient in = predIn(p);
                if (in != null) d.itemIn(in);
            }
            HasCauldronSimple c = r.getHasCauldron();
            if (c != null) {
                EmiStack fin = fluid(c.fluid());
                if (fin != null) d.fluidIn(fin);
                EmiStack fout = fluid(c.transform());
                if (fout != null) d.fluidOut(fout);
            }
            for (ChanceItemStack out : r.getResultItems()) out(d, out);
            reg.addRecipe(new GenericEmiRecipe(cat, id, d));
        });
    }

    private static <R extends AbstractProcessRecipe<?>> void blockProcess(
            EmiRegistry reg, RecipeManager rm, Class<R> cls, String key, String ws, String name) {
        EmiRecipeCategory cat = Categories.machine(reg, key, ws, name);
        Recipes.forEach(rm, cls, (id, r) -> {
            MachineDescriptor d = new MachineDescriptor();
            for (BlockStatePredicate bp : r.getInputBlocks()) {
                EmiIngredient in = blockIn(bp);
                if (in != null) d.itemIn(in);
            }
            for (ChanceBlockState cb : r.getResultBlocks()) {
                EmiStack s = EmiStack.of(cb.state().getBlock());
                if (!s.isEmpty()) d.itemOut(s);
            }
            reg.addRecipe(new GenericEmiRecipe(cat, id, d));
        });
    }

    private static EmiIngredient predIn(ItemIngredientPredicate p) {
        List<EmiStack> stacks = new ArrayList<>();
        ItemStack[] items = p.getItems();
        if (items != null) {
            for (ItemStack s : items) {
                if (s != null && !s.isEmpty()) stacks.add(EmiStack.of(s));
            }
        }
        if (stacks.isEmpty()) return null;
        return EmiIngredient.of(stacks).setAmount(Math.max(1, p.count()));
    }

    private static EmiIngredient blockIn(BlockStatePredicate bp) {
        List<EmiStack> stacks = new ArrayList<>();
        for (BlockState st : bp.constructStatesForRender()) {
            EmiStack s = EmiStack.of(st.getBlock());
            if (!s.isEmpty()) stacks.add(s);
        }
        if (stacks.isEmpty()) return null;
        return EmiIngredient.of(stacks);
    }

    private static void out(MachineDescriptor d, ChanceItemStack c) {
        ItemStack s = c.stack();
        if (s != null && !s.isEmpty()) d.itemOut(EmiStack.of(s));
    }

    private static EmiStack fluid(ResourceLocation rl) {
        if (rl == null) {
            return null;
        }
        Fluid f = BuiltInRegistries.FLUID.get(rl);
        if (f == null || f == Fluids.EMPTY) {
            return null;
        }
        return EmiStack.of(f);
    }
}
