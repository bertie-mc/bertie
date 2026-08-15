package io.github.bertie_mc.emi.integration.magitech;

import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.neoforge.NeoForgeEmiIngredient;
import dev.emi.emi.api.neoforge.NeoForgeEmiStack;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import io.github.bertie_mc.emi.framework.Categories;
import io.github.bertie_mc.emi.framework.GenericEmiRecipe;
import io.github.bertie_mc.emi.framework.MachineDescriptor;
import io.github.bertie_mc.emi.framework.Recipes;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeManager;
import net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient;
import net.stln.magitech.item.component.ComponentInit;
import net.stln.magitech.item.component.MaterialComponent;
import net.stln.magitech.item.tool.material.ToolMaterial;
import net.stln.magitech.recipe.AthanorPillarInfusionRecipe;
import net.stln.magitech.recipe.PartCuttingRecipe;
import net.stln.magitech.recipe.SpellConversionRecipe;
import net.stln.magitech.recipe.ToolAssemblyRecipe;
import net.stln.magitech.recipe.ToolMaterialRecipe;
import net.stln.magitech.recipe.ZardiusCrucibleRecipe;
import net.stln.magitech.util.ComponentHelper;

/**
 * Magitech — the tool-part chain (Part Cutting, Tool Assembly) and the three magic devices (Zardius
 * Alchemy, Athanor Infusion, Spell Conversion).
 *
 * <p>Parts and assembled tools carry the material they were made from as a data component rather
 * than as separate items, so one {@code part_cutting} recipe stands for one part in every material.
 * JEI renders that as a single entry whose slots cycle; EMI is a lookup tool first, so Part Cutting
 * is expanded into the full part x material cross product instead — that is what makes
 * right-clicking a Frigidite Plate land on the recipe that actually produces it. Tool Assembly is
 * not expanded (it would be materials^parts), so its part slots cycle and the result is the blank
 * tool.
 *
 * <p>The material set comes from the {@code tool_material} recipes, which is exactly what the
 * workbenches accept. They are sorted by id so the cycling order is stable between launches.
 *
 * <p>{@code tool_material} itself gets no category: it declares "this item counts as this material"
 * and has no result to show.
 */
public final class MagitechEmiModule {
    private MagitechEmiModule() {}

    private static final String ENGINEERING_WORKBENCH = "magitech:engineering_workbench";
    private static final String ASSEMBLY_WORKBENCH = "magitech:assembly_workbench";
    private static final String THREAD_PAGE = "magitech:thread_page";

    public static void register(EmiRegistry reg) {
        RecipeManager rm = reg.getRecipeManager();
        List<ToolMaterialRecipe> materials = new ArrayList<>();
        Recipes.forEach(rm, ToolMaterialRecipe.class, (id, r) -> materials.add(r));
        materials.sort(Comparator.comparing(r -> r.getToolMaterial().getId()));

        partCutting(reg, rm, materials);
        toolAssembly(reg, rm, materials);
        zardiusAlchemy(reg, rm);
        athanorInfusion(reg, rm);
        spellConversion(reg, rm);
    }

    /** One entry per part x material: the cut part keeps the material it was cut from. */
    private static void partCutting(EmiRegistry reg, RecipeManager rm, List<ToolMaterialRecipe> materials) {
        EmiRecipeCategory cat = Categories.machine(reg, "magitech_part_cutting", ENGINEERING_WORKBENCH, "Part Cutting");
        Recipes.forEach(rm, PartCuttingRecipe.class, (id, r) -> {
            for (ToolMaterialRecipe mat : materials) {
                Ingredient source = first(mat);
                if (source == null) {
                    continue;
                }
                MachineDescriptor d = new MachineDescriptor();
                d.itemIn(EmiIngredient.of(source).setAmount(Math.max(1, r.inputCount())));
                d.itemOut(EmiStack.of(stamped(r.result(), mat.getToolMaterial())));
                reg.addRecipe(new GenericEmiRecipe(cat, id.withSuffix(suffix(mat)), d));
            }
        });
    }

    /**
     * One entry per tool. Each part slot cycles through that part in every material; the result is
     * the plain tool, because its stats are whatever combination of materials went in.
     */
    private static void toolAssembly(EmiRegistry reg, RecipeManager rm, List<ToolMaterialRecipe> materials) {
        EmiRecipeCategory cat = Categories.machine(reg, "magitech_tool_assembly", ASSEMBLY_WORKBENCH, "Tool Assembly");
        Recipes.forEach(rm, ToolAssemblyRecipe.class, (id, r) -> {
            MachineDescriptor d = new MachineDescriptor();
            for (Ingredient ing : r.getIngredients()) {
                d.itemIn(inEveryMaterial(ing, materials));
            }
            d.itemOut(EmiStack.of(r.getResultItem(RegistryAccess.EMPTY)));
            d.info(Component.literal("The finished tool inherits the materials of the parts it was built from"));
            reg.addRecipe(new GenericEmiRecipe(cat, id, d));
        });
    }

    /** Items plus a fluid in the crucible; either side of the result is optional. */
    private static void zardiusAlchemy(EmiRegistry reg, RecipeManager rm) {
        EmiRecipeCategory cat =
                Categories.machine(reg, "magitech_zardius_alchemy", "magitech:zardius_crucible", "Zardius Alchemy");
        Recipes.forEach(rm, ZardiusCrucibleRecipe.class, (id, r) -> {
            MachineDescriptor d = new MachineDescriptor();
            for (Ingredient ing : r.ingredients()) {
                itemIn(d, ing);
            }
            SizedFluidIngredient fluid = r.fluidIngredient();
            if (fluid != null) {
                d.fluidIn(NeoForgeEmiIngredient.of(fluid));
            }
            r.result().ifPresent(s -> d.itemOut(EmiStack.of(s)));
            r.resultFluid().ifPresent(f -> d.fluidOut(NeoForgeEmiStack.of(f)));
            reg.addRecipe(new GenericEmiRecipe(cat, id, d));
        });
    }

    /**
     * A base item on the pillar plus up to twelve pedestals around it. The pedestal ring is a 2D
     * layout in JEI; here it flattens to the ordinary input row, with the mana cost as an info line.
     */
    private static void athanorInfusion(EmiRegistry reg, RecipeManager rm) {
        EmiRecipeCategory cat =
                Categories.machine(reg, "magitech_athanor_infusion", "magitech:athanor_pillar", "Athanor Infusion");
        Recipes.forEach(rm, AthanorPillarInfusionRecipe.class, (id, r) -> {
            MachineDescriptor d = new MachineDescriptor();
            d.itemIn(EmiStack.of(r.getBase()));
            for (Ingredient ing : r.getIngredients()) {
                itemIn(d, ing);
            }
            d.itemOut(EmiStack.of(r.getResultItem(RegistryAccess.EMPTY)));
            if (r.getMana() > 0) {
                d.info(Component.literal("Required Mana: " + r.getMana()));
            }
            reg.addRecipe(new GenericEmiRecipe(cat, id, d));
        });
    }

    /** An item plus the Thread Page carrying the spell; the wand does the converting. */
    private static void spellConversion(EmiRegistry reg, RecipeManager rm) {
        EmiRecipeCategory cat =
                Categories.machine(reg, "magitech_spell_conversion", "magitech:wand", "Spell Conversion");
        Item page = item(THREAD_PAGE);
        Recipes.forEach(rm, SpellConversionRecipe.class, (id, r) -> {
            MachineDescriptor d = new MachineDescriptor();
            itemIn(d, r.ingredient());
            if (page != null) {
                ItemStack stack = new ItemStack(page);
                ComponentHelper.setThreadPage(stack, r.spell());
                d.itemIn(EmiStack.of(stack));
            }
            d.itemOut(EmiStack.of(r.result()));
            reg.addRecipe(new GenericEmiRecipe(cat, id, d));
        });
    }

    /** The ingredient's first item, once per material — the same set of variants JEI cycles through. */
    private static EmiIngredient inEveryMaterial(Ingredient ing, List<ToolMaterialRecipe> materials) {
        ItemStack[] items = ing.getItems();
        if (items.length == 0 || items[0].isEmpty()) {
            return null;
        }
        List<EmiStack> variants = new ArrayList<>();
        for (ToolMaterialRecipe mat : materials) {
            variants.add(EmiStack.of(stamped(items[0], mat.getToolMaterial())));
        }
        return variants.isEmpty() ? EmiIngredient.of(ing) : EmiIngredient.of(variants);
    }

    private static ItemStack stamped(ItemStack template, ToolMaterial material) {
        ItemStack stack = template.copy();
        stack.set(ComponentInit.MATERIAL_COMPONENT.get(), new MaterialComponent(material));
        return stack;
    }

    /**
     * The Athanor pads its pedestal list out with air, so an ingredient that resolves to nothing has
     * to be dropped rather than turned into an empty slot.
     */
    private static void itemIn(MachineDescriptor d, Ingredient ing) {
        if (ing == null || ing.isEmpty()) {
            return;
        }
        for (ItemStack s : ing.getItems()) {
            if (!s.isEmpty()) {
                d.itemIn(EmiIngredient.of(ing));
                return;
            }
        }
    }

    private static Ingredient first(ToolMaterialRecipe mat) {
        List<Ingredient> ings = mat.getIngredients();
        return ings.isEmpty() ? null : ings.get(0);
    }

    /** Material id as a path segment, so every part x material pair gets its own EMI recipe id. */
    private static String suffix(ToolMaterialRecipe mat) {
        ResourceLocation id = mat.getToolMaterial().getId();
        return "/" + id.getNamespace() + "/" + id.getPath();
    }

    private static Item item(String id) {
        Item item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(id));
        return item == Items.AIR ? null : item;
    }
}
