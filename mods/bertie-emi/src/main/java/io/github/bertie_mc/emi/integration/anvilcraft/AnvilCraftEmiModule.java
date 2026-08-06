package io.github.bertie_mc.emi.integration.anvilcraft;

import dev.anvilcraft.lib.recipe.component.BlockStatePredicate;
import dev.anvilcraft.lib.recipe.component.ChanceBlockState;
import dev.anvilcraft.lib.recipe.component.ChanceItemStack;
import dev.anvilcraft.lib.recipe.component.ItemIngredientPredicate;
import dev.dubhe.anvilcraft.recipe.ChargerChargingRecipe;
import dev.dubhe.anvilcraft.recipe.JewelCraftingRecipe;
import dev.dubhe.anvilcraft.recipe.anvil.MassInjectRecipe;
import dev.dubhe.anvilcraft.recipe.anvil.StampingUniqueItemsRecipe;
import dev.dubhe.anvilcraft.recipe.anvil.collision.AnvilCollisionCraftRecipe;
import dev.dubhe.anvilcraft.recipe.anvil.collision.BlockTransform;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.AbstractProcessRecipe;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.BlockCompressRecipe;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.BlockCrushRecipe;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.BlockSmearRecipe;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.BoilingRecipe;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.BulgingRecipe;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.CookingRecipe;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.ItemCompressRecipe;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.ItemCrushRecipe;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.ItemInjectRecipe;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.MeshRecipe;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.NeutronIrradiationRecipe;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.SqueezingRecipe;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.StampingRecipe;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.SuperHeatingRecipe;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.TimeWarpRecipe;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.UnpackRecipe;
import dev.dubhe.anvilcraft.recipe.component.HasCauldronSimple;
import dev.dubhe.anvilcraft.recipe.mineral.MineralFountainChanceRecipe;
import dev.dubhe.anvilcraft.recipe.mineral.MineralFountainRecipe;
import dev.dubhe.anvilcraft.recipe.multiblock.MultiblockConversionRecipe;
import dev.dubhe.anvilcraft.recipe.multiblock.MultiblockRecipe;
import dev.dubhe.anvilcraft.recipe.multiple.BaseMultipleToOneSmithingRecipe;
import dev.dubhe.anvilcraft.recipe.transform.MobTransformRecipe;
import dev.dubhe.anvilcraft.recipe.transform.MobTransformWithItemRecipe;
import dev.dubhe.anvilcraft.recipe.transform.TransformResult;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import io.github.bertie_mc.emi.framework.Categories;
import io.github.bertie_mc.emi.framework.GenericEmiRecipe;
import io.github.bertie_mc.emi.framework.MachineDescriptor;
import io.github.bertie_mc.emi.framework.Recipes;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

/**
 * AnvilCraft — recipes triggered by a falling anvil. The "process" types (item-based, block-based and
 * the two mixed ones) all share one mapper: inputs/outputs come from anvillib predicate/chance
 * components and cauldron fluids from resource ids. The remaining types each need their own mapper
 * because they carry something the generic shape has no slot for — a mass value, an entity, a
 * multiblock pattern.
 *
 * <p>Category names follow AnvilCraft's own {@code gui.anvilcraft.category.*} strings so the EMI tabs
 * read the same as the mod's JEI ones.
 *
 * <p>Still deferred (nothing to usefully render): {@code cooling} (a raw anvillib in-world recipe
 * with bespoke outcomes), and the dynamic {@code CanningFoodRecipe}/{@code PillRecipe}, which are
 * vanilla {@code CustomRecipe}s matching any food / any potion.
 *
 * <p>Most types have no machine block, so the workstation falls back to the Anvil.
 */
public final class AnvilCraftEmiModule {
    private AnvilCraftEmiModule() {}

    private static final String ANVIL = "minecraft:anvil";
    private static final String CAULDRON = "minecraft:cauldron";

    public static void register(EmiRegistry reg) {
        RecipeManager rm = reg.getRecipeManager();

        process(reg, rm, BulgingRecipe.class, "anvilcraft_bulging", CAULDRON, "Bulging");
        process(reg, rm, ItemCrushRecipe.class, "anvilcraft_item_crush", "anvilcraft:crushing_table", "Item Crushing");
        process(reg, rm, SuperHeatingRecipe.class, "anvilcraft_super_heating", CAULDRON, "Super Heating");
        process(reg, rm, TimeWarpRecipe.class, "anvilcraft_time_warp", ANVIL, "Time Warp");
        process(reg, rm, StampingRecipe.class, "anvilcraft_stamping", "anvilcraft:stamping_platform", "Stamping");
        process(reg, rm, UnpackRecipe.class, "anvilcraft_unpack", ANVIL, "Unpacking");
        process(reg, rm, MeshRecipe.class, "anvilcraft_mesh", ANVIL, "Mesh Sifting");
        process(reg, rm, ItemCompressRecipe.class, "anvilcraft_item_compress", ANVIL, "Item Compress");
        process(reg, rm, NeutronIrradiationRecipe.class, "anvilcraft_neutron", ANVIL, "Neutron Irradiation");
        process(reg, rm, CookingRecipe.class, "anvilcraft_cooking", ANVIL, "Anvil Cooking");
        process(reg, rm, BoilingRecipe.class, "anvilcraft_boiling", CAULDRON, "Boiling");

        process(reg, rm, BlockSmearRecipe.class, "anvilcraft_block_smear", ANVIL, "Block Smear");
        process(reg, rm, BlockCrushRecipe.class, "anvilcraft_block_crush", ANVIL, "Block Crushing");
        process(reg, rm, BlockCompressRecipe.class, "anvilcraft_block_compress", ANVIL, "Block Compress");

        // Mixed: these two carry item AND block sides at once, which is why the mapper is unified.
        process(reg, rm, ItemInjectRecipe.class, "anvilcraft_item_inject", ANVIL, "Item Inject");
        process(reg, rm, SqueezingRecipe.class, "anvilcraft_squeezing", CAULDRON, "Squeezing");

        EmiRecipeCategory jewel =
                Categories.machine(reg, "anvilcraft_jewel", "anvilcraft:jewelcrafting_table", "Jewel Crafting");
        Recipes.forEach(rm, JewelCraftingRecipe.class, (id, r) -> {
            MachineDescriptor d = new MachineDescriptor();
            for (Ingredient ing : r.getIngredients()) d.itemIn(EmiIngredient.of(ing));
            d.itemOut(EmiStack.of(r.getResult()));
            reg.addRecipe(new GenericEmiRecipe(jewel, id, d));
        });

        EmiRecipeCategory su = Categories.machine(
                reg, "anvilcraft_stamping_unique", "anvilcraft:stamping_platform", "Stamping (Unique)");
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

        massInject(reg, rm);
        anvilCollision(reg, rm);
        mineralFountain(reg, rm);
        mobTransform(reg, rm);
        multiblock(reg, rm);
        multipleToOneSmithing(reg, rm);
    }

    /**
     * The shared mapper for every {@link AbstractProcessRecipe}. A given type only fills some of the
     * four collections — item types leave the block lists empty and vice versa — so one pass covers
     * the item-only, block-only and mixed types alike.
     */
    private static <R extends AbstractProcessRecipe<?>> void process(
            EmiRegistry reg, RecipeManager rm, Class<R> cls, String key, String ws, String name) {
        EmiRecipeCategory cat = Categories.machine(reg, key, ws, name);
        Recipes.forEach(rm, cls, (id, r) -> {
            MachineDescriptor d = new MachineDescriptor();
            for (ItemIngredientPredicate p : r.getInputItems()) d.itemIn(predIn(p));
            for (BlockStatePredicate bp : r.getInputBlocks()) d.itemIn(blockIn(bp));
            HasCauldronSimple c = r.getHasCauldron();
            if (c != null) {
                d.fluidIn(fluid(c.fluid()));
                d.fluidOut(fluid(c.transform()));
            }
            for (ChanceItemStack o : r.getResultItems()) out(d, o);
            for (ChanceBlockState cb : r.getResultBlocks()) d.itemOut(blockOut(cb));
            reg.addRecipe(new GenericEmiRecipe(cat, id, d));
        });
    }

    /** Feed items to a falling anvil to bank mass; the machine emits its product once the total is met. */
    private static void massInject(EmiRegistry reg, RecipeManager rm) {
        EmiRecipeCategory cat =
                Categories.machine(reg, "anvilcraft_mass_inject", "anvilcraft:space_overcompressor", "Mass Inject");
        Recipes.forEach(rm, MassInjectRecipe.class, (id, r) -> {
            MachineDescriptor d = new MachineDescriptor();
            d.itemIn(EmiIngredient.of(r.getIngredient()));
            d.info(r.displayMassValue());
            reg.addRecipe(new GenericEmiRecipe(cat, id, d));
        });
    }

    /** An anvil falling fast enough onto a block: converts nearby blocks and/or drops items. */
    private static void anvilCollision(EmiRegistry reg, RecipeManager rm) {
        EmiRecipeCategory cat = Categories.machine(reg, "anvilcraft_anvil_collision", ANVIL, "Anvil Collision");
        Recipes.forEach(rm, AnvilCollisionCraftRecipe.class, (id, r) -> {
            MachineDescriptor d = new MachineDescriptor();
            EmiIngredient anvil = blockIn(r.anvil());
            // A consumed anvil is a real input; a surviving one is a tool, which is what catalysts are for.
            if (r.consume()) {
                d.itemIn(anvil);
            } else {
                d.catalyst(anvil);
            }
            d.itemIn(blockIn(r.hitBlock()));
            for (BlockTransform t : r.transformBlocks()) {
                d.itemIn(blockIn(t.inputBlock()));
                d.itemOut(blockOut(t.outputBlock()));
            }
            for (ChanceItemStack o : r.outputItems()) out(d, o);
            d.info(Component.literal("Need Speed: " + r.speed() + " m/tick"));
            reg.addRecipe(new GenericEmiRecipe(cat, id, d));
        });
    }

    /**
     * Mineral Fountain: the block below decides what the fountain turns stone into. The plain type
     * needs a specific block present; the chance type is gated on the dimension instead.
     */
    private static void mineralFountain(EmiRegistry reg, RecipeManager rm) {
        EmiRecipeCategory cat = Categories.machine(
                reg, "anvilcraft_mineral_fountain", "anvilcraft:mineral_fountain", "Mineral Fountain");
        Recipes.forEach(rm, MineralFountainRecipe.class, (id, r) -> {
            MachineDescriptor d = new MachineDescriptor();
            d.itemIn(blockIn(r.getFromBlock()));
            d.catalyst(blockIn(r.getNeedBlock()));
            d.itemOut(blockOut(r.getToBlock()));
            reg.addRecipe(new GenericEmiRecipe(cat, id, d));
        });
        Recipes.forEach(rm, MineralFountainChanceRecipe.class, (id, r) -> {
            MachineDescriptor d = new MachineDescriptor();
            d.itemIn(blockIn(r.getFromBlock()));
            d.itemOut(blockOut(r.getToBlock()));
            ResourceLocation dim = r.getDimension();
            if (dim != null) {
                d.info(Component.literal("Dimension: " + dim));
            }
            reg.addRecipe(new GenericEmiRecipe(cat, id, d));
        });
    }

    /**
     * Mobs caught in a Corrupted Beacon beam. Entities are not EMI stacks, so each one is shown as its
     * spawn egg with the names and odds spelled out in the info line — the line is what carries the
     * recipe when a mob has no spawn egg (Giant, for one).
     */
    private static void mobTransform(EmiRegistry reg, RecipeManager rm) {
        EmiRecipeCategory cat =
                Categories.machine(reg, "anvilcraft_mob_transform", "anvilcraft:corrupted_beacon", "Mob Transform");
        Recipes.forEach(rm, MobTransformRecipe.class, (id, r) -> {
            MachineDescriptor d = new MachineDescriptor();
            d.itemIn(spawnEgg(r.input()));
            MutableComponent line =
                    Component.empty().append(r.input().getDescription()).append(" -> ");
            boolean first = true;
            for (TransformResult res : r.results()) {
                d.itemOut(spawnEgg(res.resultEntityType()));
                if (!first) {
                    line.append(", ");
                }
                line.append(res.resultEntityType().getDescription()).append(" " + percent(res.probability()));
                first = false;
            }
            d.info(Component.literal("In a Corrupted Beacon beam"));
            d.info(line);
            reg.addRecipe(new GenericEmiRecipe(cat, id, d));
        });
        Recipes.forEach(rm, MobTransformWithItemRecipe.class, (id, r) -> {
            MachineDescriptor d = new MachineDescriptor();
            d.itemIn(spawnEgg(r.input()));
            for (ItemIngredientPredicate p : r.itemIngredients()) d.itemIn(predIn(p));
            TransformResult res = r.specialResult();
            MutableComponent line =
                    Component.empty().append(r.input().getDescription()).append(" -> ");
            if (res != null) {
                d.itemOut(spawnEgg(res.resultEntityType()));
                line.append(res.resultEntityType().getDescription());
            }
            d.itemOut(EmiStack.of(r.itemResult()));
            d.info(Component.literal("Chance Per Item: " + r.chancePercentPerItem() + "%"));
            d.info(line);
            reg.addRecipe(new GenericEmiRecipe(cat, id, d));
        });
    }

    /**
     * Multiblock structures. The pattern is a 3D layer grid; EMI gets its flattened block list rather
     * than the layer-by-layer viewer JEI draws, which keeps it inside the generic one-row layout.
     */
    private static void multiblock(EmiRegistry reg, RecipeManager rm) {
        EmiRecipeCategory craft = Categories.machine(reg, "anvilcraft_multiblock", ANVIL, "Multiblock Crafting");
        Recipes.forEach(rm, MultiblockRecipe.class, (id, r) -> {
            MachineDescriptor d = new MachineDescriptor();
            for (ItemStack s : r.getPattern().toIngredientList()) d.itemIn(EmiStack.of(s));
            d.itemOut(EmiStack.of(r.getResult()));
            reg.addRecipe(new GenericEmiRecipe(craft, id, d));
        });

        EmiRecipeCategory conv =
                Categories.machine(reg, "anvilcraft_multiblock_conversion", ANVIL, "Multiblock Conversion");
        Recipes.forEach(rm, MultiblockConversionRecipe.class, (id, r) -> {
            MachineDescriptor d = new MachineDescriptor();
            for (ItemStack s : r.getInputPattern().toIngredientList()) d.itemIn(EmiStack.of(s));
            d.itemOut(EmiStack.of(r.centerOutput()));
            d.info(Component.literal("Forms a " + r.getSize() + "-block structure"));
            reg.addRecipe(new GenericEmiRecipe(conv, id, d));
        });
    }

    /**
     * Two-, four- and eight-to-one smithing. All three share {@link BaseMultipleToOneSmithingRecipe},
     * so one enumeration by the base class covers them; the input count is what tells them apart.
     */
    private static void multipleToOneSmithing(EmiRegistry reg, RecipeManager rm) {
        EmiRecipeCategory cat = Categories.machine(
                reg, "anvilcraft_multi_smithing", "anvilcraft:royal_smithing_table", "Multiple To One Smithing");
        EmiStack ember = Categories.stack("anvilcraft:ember_smithing_table");
        if (!ember.isEmpty()) {
            reg.addWorkstation(cat, ember);
        }
        Recipes.forEach(rm, BaseMultipleToOneSmithingRecipe.class, (id, r) -> {
            MachineDescriptor d = new MachineDescriptor();
            d.itemIn(predIn(r.getTemplate()));
            d.itemIn(predIn(r.getMaterial()));
            for (ItemIngredientPredicate p : r.getInputs()) d.itemIn(predIn(p));
            d.itemOut(EmiStack.of(r.getResult().getResult()));
            reg.addRecipe(new GenericEmiRecipe(cat, id, d));
        });
    }

    private static EmiIngredient predIn(ItemIngredientPredicate p) {
        if (p == null) {
            return null;
        }
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
        if (bp == null) {
            return null;
        }
        List<EmiStack> stacks = new ArrayList<>();
        for (BlockState st : bp.constructStatesForRender()) {
            EmiStack s = EmiStack.of(st.getBlock());
            if (!s.isEmpty()) stacks.add(s);
        }
        if (stacks.isEmpty()) return null;
        return EmiIngredient.of(stacks);
    }

    private static EmiStack blockOut(ChanceBlockState cb) {
        return cb == null ? null : EmiStack.of(cb.state().getBlock());
    }

    private static void out(MachineDescriptor d, ChanceItemStack c) {
        ItemStack s = c.stack();
        if (s != null && !s.isEmpty()) d.itemOut(EmiStack.of(s));
    }

    /** An entity rendered as its spawn egg, or empty when the mob has none. */
    private static EmiStack spawnEgg(EntityType<?> type) {
        if (type == null) {
            return null;
        }
        SpawnEggItem egg = SpawnEggItem.byId(type);
        return egg == null ? null : EmiStack.of(egg);
    }

    private static String percent(double probability) {
        double pct = probability * 100.0;
        return (pct == Math.floor(pct) ? String.valueOf((long) pct) : String.format(Locale.ROOT, "%.1f", pct)) + "%";
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
