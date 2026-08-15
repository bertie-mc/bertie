package io.github.bertie_mc.emi.integration.anvilcraft;

import dev.anvilcraft.lib.recipe.component.BlockStatePredicate;
import dev.anvilcraft.lib.recipe.component.ChanceBlockState;
import dev.anvilcraft.lib.recipe.component.ChanceItemStack;
import dev.anvilcraft.lib.recipe.component.ItemIngredientPredicate;
import dev.dubhe.anvilcraft.recipe.CanningFoodRecipe;
import dev.dubhe.anvilcraft.recipe.ChargerChargingRecipe;
import dev.dubhe.anvilcraft.recipe.JewelCraftingRecipe;
import dev.dubhe.anvilcraft.recipe.PillRecipe;
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
import dev.emi.emi.api.recipe.EmiCraftingRecipe;
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
import java.util.function.Supplier;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.loot.providers.number.BinomialDistributionGenerator;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.NumberProvider;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;
import net.neoforged.neoforge.common.Tags;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
 * <p>{@code CanningFoodRecipe} and {@code PillRecipe} are vanilla {@code CustomRecipe}s, so EMI's own
 * crafting handler cannot read them; they are enumerated here and added to EMI's Crafting category
 * where they belong, rather than being given a tab of their own. The conversions with no recipe file
 * at all live in {@link AnvilCraftBehaviorEmiModule}.
 *
 * <p>Still deferred (nothing to usefully render): {@code cooling}, a raw anvillib in-world recipe
 * with bespoke outcomes.
 *
 * <p>A process type either transforms blocks the recipe names, or runs on a machine the class
 * hardcodes — a Heater, a Crushing Table, a lit Corrupted Beacon. The machine is that category's
 * workstation and a catalyst, never an ingredient: it is what the recipe needs present, not what it
 * eats. Types with neither fall back to the Anvil.
 */
public final class AnvilCraftEmiModule {
    private AnvilCraftEmiModule() {}

    private static final Logger LOGGER = LoggerFactory.getLogger("bertieemi");

    private static final String ANVIL = "minecraft:anvil";
    private static final String CAULDRON = "minecraft:cauldron";
    /**
     * Every process recipe is triggered the same way, so the sentences share an opening. They are
     * kept to roughly one rendered line each: the note repeats on every recipe in the category, and
     * a three-line wrap would cost more room than the recipe itself.
     */
    private static final String DROP = "Anvil onto items ";

    public static void register(EmiRegistry reg) {
        RecipeManager rm = reg.getRecipeManager();

        process(
                reg,
                rm,
                () -> BulgingRecipe.class,
                "anvilcraft_bulging",
                "Bulging",
                new Setup(CAULDRON, false, DROP + "in a water-filled cauldron"));
        process(
                reg,
                rm,
                () -> SqueezingRecipe.class,
                "anvilcraft_squeezing",
                "Squeezing",
                new Setup(CAULDRON, false, "Anvil onto a block sitting above a cauldron"));
        process(
                reg,
                rm,
                () -> SuperHeatingRecipe.class,
                "anvilcraft_super_heating",
                "Super Heating",
                machine("anvilcraft:heater", DROP + "in a cauldron over a Heater"));
        process(
                reg,
                rm,
                () -> BoilingRecipe.class,
                "anvilcraft_boiling",
                "Boiling",
                machine("minecraft:campfire", DROP + "in a cauldron over a lit Campfire"));

        process(
                reg,
                rm,
                () -> ItemCrushRecipe.class,
                "anvilcraft_item_crush",
                "Item Crushing",
                machine("anvilcraft:crushing_table", DROP + "on a Crushing Table"));
        process(
                reg,
                rm,
                () -> StampingRecipe.class,
                "anvilcraft_stamping",
                "Stamping",
                machine("anvilcraft:stamping_platform", DROP + "on a Stamping Platform"));
        process(
                reg,
                rm,
                () -> MeshRecipe.class,
                "anvilcraft_mesh",
                "Mesh Sifting",
                machine("minecraft:scaffolding", DROP + "on Scaffolding"));
        process(
                reg,
                rm,
                () -> UnpackRecipe.class,
                "anvilcraft_unpack",
                "Unpacking",
                machine("minecraft:iron_trapdoor", DROP + "on a closed upper-half Iron Trapdoor"));
        process(
                reg,
                rm,
                () -> NeutronIrradiationRecipe.class,
                "anvilcraft_neutron",
                "Neutron Irradiation",
                machine("anvilcraft:neutron_irradiator", DROP + "in a cauldron on a Neutron Irradiator"));
        process(
                reg,
                rm,
                () -> CookingRecipe.class,
                "anvilcraft_cooking",
                "Anvil Cooking",
                machine("minecraft:campfire", DROP + "in a cauldron on a lit Campfire"));
        process(
                reg,
                rm,
                () -> TimeWarpRecipe.class,
                "anvilcraft_time_warp",
                "Time Warp",
                machine("anvilcraft:corrupted_beacon", DROP + "on a lit Corrupted Beacon"));
        process(
                reg,
                rm,
                () -> ItemCompressRecipe.class,
                "anvilcraft_item_compress",
                "Item Compress",
                new Setup(CAULDRON, false, DROP + "in a cauldron"));

        process(
                reg,
                rm,
                () -> BlockSmearRecipe.class,
                "anvilcraft_block_smear",
                "Block Smear",
                transforms("Anvil onto the block"));
        process(
                reg,
                rm,
                () -> BlockCrushRecipe.class,
                "anvilcraft_block_crush",
                "Block Crushing",
                transforms("Anvil onto the block"));
        process(
                reg,
                rm,
                () -> BlockCompressRecipe.class,
                "anvilcraft_block_compress",
                "Block Compress",
                transforms("Anvil onto the block"));

        // Mixed: this one carries item AND block sides at once, which is why the mapper is unified.
        process(
                reg,
                rm,
                () -> ItemInjectRecipe.class,
                "anvilcraft_item_inject",
                "Item Inject",
                transforms(DROP + "lying on the block"));

        safely("Jewel Crafting", () -> jewelCrafting(reg, rm));
        safely("Stamping (Unique)", () -> stampingUnique(reg, rm));
        safely("Charger Charging", () -> chargerCharging(reg, rm));
        safely("Mass Inject", () -> massInject(reg, rm));
        safely("Anvil Collision", () -> anvilCollision(reg, rm));
        safely("Mineral Fountain", () -> mineralFountain(reg, rm));
        safely("Mob Transform", () -> mobTransform(reg, rm));
        safely("Multiblock", () -> multiblock(reg, rm));
        safely("Multiple To One Smithing", () -> multipleToOneSmithing(reg, rm));
        safely("Canning Food", () -> canningFood(reg, rm));
        safely("Pill", () -> pills(reg, rm));
        AnvilCraftBehaviorEmiModule.register(reg);
        AnvilCraftGuideEmiModule.register(reg);
    }

    /**
     * AnvilCraft reshapes its recipe classes between releases — 1.6 folded bulging, boiling and
     * cooking into other types, and took anvillib from 1.x to 2.x with them. Every category is
     * registered through here so that a pack running a build other than the one this was compiled
     * against loses only the categories that actually moved, instead of losing the whole mod's tabs
     * to the first missing class. Class literals therefore have to stay inside the lambda, where the
     * JVM resolves them lazily.
     */
    static void safely(String category, Runnable body) {
        try {
            body.run();
        } catch (Throwable t) {
            LOGGER.warn("bertieemi: AnvilCraft '{}' is not in this build of the mod, skipping it", category, t);
        }
    }

    private static void jewelCrafting(EmiRegistry reg, RecipeManager rm) {
        EmiRecipeCategory jewel =
                Categories.machine(reg, "anvilcraft_jewel", "anvilcraft:jewelcrafting_table", "Jewel Crafting");
        Recipes.forEach(rm, JewelCraftingRecipe.class, (id, r) -> {
            MachineDescriptor d = new MachineDescriptor();
            for (Ingredient ing : r.getIngredients()) d.itemInMerged(EmiIngredient.of(ing));
            d.itemOut(EmiStack.of(r.getResult()));
            reg.addRecipe(new GenericEmiRecipe(jewel, id, d));
        });
    }

    private static void stampingUnique(EmiRegistry reg, RecipeManager rm) {
        EmiRecipeCategory su = Categories.machine(
                reg, "anvilcraft_stamping_unique", "anvilcraft:stamping_platform", "Stamping (Unique)");
        Recipes.forEach(rm, StampingUniqueItemsRecipe.class, (id, r) -> {
            MachineDescriptor d = new MachineDescriptor();
            for (Ingredient ing : r.getIngredients()) d.itemInMerged(EmiIngredient.of(ing));
            for (ChanceItemStack c : r.getResults()) out(d, c);
            reg.addRecipe(new GenericEmiRecipe(su, id, d));
        });
    }

    private static void chargerCharging(EmiRegistry reg, RecipeManager rm) {
        // No anvil involved here despite the old icon saying so: the Charger is its own machine.
        EmiRecipeCategory charger =
                Categories.machine(reg, "anvilcraft_charger", "anvilcraft:charger", "Charger Charging");
        Recipes.forEach(rm, ChargerChargingRecipe.class, (id, r) -> {
            MachineDescriptor d = new MachineDescriptor();
            d.itemIn(EmiIngredient.of(r.getIngredient()));
            d.itemOut(EmiStack.of(r.getResult()));
            if (r.getTime() > 0) {
                d.info(Component.literal("Takes " + Categories.seconds(r.getTime()) + " in a powered Charger"));
            }
            reg.addRecipe(new GenericEmiRecipe(charger, id, d));
        });
    }

    /**
     * A tin can plus any food. The recipe itself only carries the predicate, so the food slot is the
     * {@code c:foods} tag filtered by it — the same list AnvilCraft's JEI extension builds.
     */
    private static void canningFood(EmiRegistry reg, RecipeManager rm) {
        Recipes.forEach(rm, CanningFoodRecipe.class, (id, r) -> {
            List<EmiStack> foods = new ArrayList<>();
            BuiltInRegistries.ITEM.getTag(Tags.Items.FOODS).ifPresent(tag -> {
                for (Holder<Item> holder : tag) {
                    ItemStack stack = holder.value().getDefaultInstance();
                    if (!stack.isEmpty() && r.isFood(stack)) {
                        foods.add(EmiStack.of(stack));
                    }
                }
            });
            if (foods.isEmpty()) {
                return;
            }
            crafting(
                    reg,
                    id,
                    List.of(Categories.stack("anvilcraft:tin_can"), EmiIngredient.of(foods)),
                    "anvilcraft:canned_food");
        });
    }

    /**
     * A pill plus a potion of any kind, which the pill then carries. Water/mundane/thick/awkward are
     * left out because they hold no effect to transfer.
     */
    private static void pills(EmiRegistry reg, RecipeManager rm) {
        Recipes.forEach(rm, PillRecipe.class, (id, r) -> {
            List<EmiStack> potions = new ArrayList<>();
            for (Item bottle : List.of(Items.POTION, Items.SPLASH_POTION, Items.LINGERING_POTION)) {
                for (Holder.Reference<Potion> potion :
                        BuiltInRegistries.POTION.holders().toList()) {
                    if (potion.is(Potions.WATER)
                            || potion.is(Potions.MUNDANE)
                            || potion.is(Potions.THICK)
                            || potion.is(Potions.AWKWARD)) {
                        continue;
                    }
                    ItemStack stack = bottle.getDefaultInstance();
                    stack.set(DataComponents.POTION_CONTENTS, new PotionContents(potion));
                    potions.add(EmiStack.of(stack));
                }
            }
            if (potions.isEmpty()) {
                return;
            }
            crafting(
                    reg,
                    id,
                    List.of(EmiIngredient.of(potions), Categories.stack("anvilcraft:pill")),
                    "anvilcraft:pill");
        });
    }

    private static void crafting(EmiRegistry reg, ResourceLocation id, List<EmiIngredient> inputs, String result) {
        EmiStack out = Categories.stack(result);
        if (out.isEmpty()) {
            return;
        }
        reg.addRecipe(new EmiCraftingRecipe(inputs, out, id, true));
    }

    /**
     * The shared mapper for every {@link AbstractProcessRecipe}. A given type only fills some of the
     * four collections — item types leave the block lists empty and vice versa — so one pass covers
     * the item-only, block-only and mixed types alike.
     */
    private static <R extends AbstractProcessRecipe<?>> void process(
            EmiRegistry reg, RecipeManager rm, Supplier<Class<R>> cls, String key, String name, Setup setup) {
        safely(name, () -> {
            // Resolve the recipe class before declaring the category, so a type this build of
            // AnvilCraft no longer has leaves no empty tab behind.
            Class<R> type = cls.get();
            EmiRecipeCategory cat = Categories.machine(reg, key, setup.workstation(), name);
            Recipes.forEach(rm, type, (id, r) -> {
                MachineDescriptor d = new MachineDescriptor();
                for (ItemIngredientPredicate p : r.getInputItems()) d.itemInMerged(predIn(p));
                for (BlockStatePredicate bp : r.getInputBlocks()) {
                    // A structural block is the machine the recipe runs on, so it survives; only a
                    // block the recipe actually transforms belongs in an input slot.
                    if (setup.blocksAreMachine()) {
                        d.catalyst(blockIn(bp));
                    } else {
                        d.itemInMerged(blockIn(bp));
                    }
                }
                HasCauldronSimple c = r.getHasCauldron();
                if (c != null) {
                    d.fluidIn(fluid(c.fluid()));
                    d.fluidOut(fluid(c.transform()));
                }
                for (ChanceItemStack o : r.getResultItems()) out(d, o);
                for (ChanceBlockState cb : r.getResultBlocks()) d.itemOut(blockOut(cb));
                d.info(Component.literal(setup.how()));
                reg.addRecipe(new GenericEmiRecipe(cat, id, d));
            });
        });
    }

    /**
     * What a process category needs beyond its recipes: the block it runs on, whether that block is
     * machinery the recipe merely requires (rather than something it consumes), and the sentence
     * that says how to actually set the thing up. Every {@link AbstractProcessRecipe} shares one
     * trigger — {@code ON_ANVIL_FALL_ON} — so an anvil always has to land; what differs is where the
     * items go and what has to be underneath them.
     */
    private record Setup(String workstation, boolean blocksAreMachine, String how) {}

    /** The recipe supplies its own blocks and transforms them, so nothing here is scenery. */
    private static Setup transforms(String how) {
        return new Setup(ANVIL, false, how);
    }

    /** The block is hardcoded machinery the recipe runs on top of, and it survives the process. */
    private static Setup machine(String workstation, String how) {
        return new Setup(workstation, true, how);
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
            for (ItemIngredientPredicate p : r.itemIngredients()) d.itemInMerged(predIn(p));
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
            for (ItemStack s : r.getPattern().toIngredientList()) d.itemInMerged(EmiStack.of(s));
            d.itemOut(EmiStack.of(r.getResult()));
            reg.addRecipe(new GenericEmiRecipe(craft, id, d));
        });

        EmiRecipeCategory conv =
                Categories.machine(reg, "anvilcraft_multiblock_conversion", ANVIL, "Multiblock Conversion");
        Recipes.forEach(rm, MultiblockConversionRecipe.class, (id, r) -> {
            MachineDescriptor d = new MachineDescriptor();
            for (ItemStack s : r.getInputPattern().toIngredientList()) d.itemInMerged(EmiStack.of(s));
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
            for (ItemIngredientPredicate p : r.getInputs()) d.itemInMerged(predIn(p));
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

    /**
     * anvillib keeps the result stack at count 1 and carries the real amount in a loot-table
     * {@link NumberProvider} alongside it, so reading {@code stack()} on its own renders every
     * AnvilCraft output as a single item. That is what made the three Royal Steel recipes look
     * identical when they actually yield one, two and three ingots.
     *
     * <p>A constant is an exact count. A binomial is "n tries at p", which is EMI's own
     * amount-plus-chance. A uniform range has no EMI equivalent, so it shows its highest roll and
     * says the range in words.
     */
    private static void out(MachineDescriptor d, ChanceItemStack c) {
        if (c == null) {
            return;
        }
        ItemStack s = c.stack();
        if (s == null || s.isEmpty()) {
            return;
        }
        EmiStack out = EmiStack.of(s);
        switch (c.count()) {
            case ConstantValue v -> out.setAmount(amount(v.value()));
            case BinomialDistributionGenerator b -> {
                out.setAmount(amount(constant(b.n(), 1)));
                double chance = constant(b.p(), 1);
                if (chance < 1.0) {
                    out.setChance((float) chance);
                }
            }
            case UniformGenerator u -> {
                double low = constant(u.min(), 1);
                double high = constant(u.max(), low);
                out.setAmount(amount(high));
                if (high > low) {
                    d.info(Component.literal(s.getHoverName().getString() + ": " + amount(low) + "-" + amount(high)));
                }
            }
            default -> {}
        }
        d.itemOut(out);
    }

    /** A nested provider's fixed value, or {@code fallback} when it is not a plain constant. */
    private static double constant(NumberProvider provider, double fallback) {
        return provider instanceof ConstantValue v ? v.value() : fallback;
    }

    private static long amount(double value) {
        return Math.max(1L, Math.round(value));
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
