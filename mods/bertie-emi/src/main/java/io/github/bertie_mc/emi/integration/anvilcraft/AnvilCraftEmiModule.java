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
 * <p>Every type triggered by a falling anvil is drawn by {@link AnvilDropEmiRecipe} as the structure
 * that performs it rather than as a row of slots, because where each block goes is the recipe. The
 * machine a type runs on is also that category's workstation, and is marked as a catalyst: it is
 * what the recipe needs present, not what it eats. Types with no machine of their own fall back to
 * the Anvil.
 */
public final class AnvilCraftEmiModule {
    private AnvilCraftEmiModule() {}

    private static final Logger LOGGER = LoggerFactory.getLogger("bertieemi");

    private static final String ANVIL = "minecraft:anvil";
    private static final String CAULDRON = "minecraft:cauldron";

    public static void register(EmiRegistry reg) {
        safely("crab_trap", () -> AnvilCraftCrabTrapEmiModule.register(reg));
        RecipeManager rm = reg.getRecipeManager();

        process(reg, rm, () -> BulgingRecipe.class, "anvilcraft_bulging", "Bulging", at(CAULDRON));
        process(reg, rm, () -> SqueezingRecipe.class, "anvilcraft_squeezing", "Squeezing", at(CAULDRON));
        process(
                reg,
                rm,
                () -> SuperHeatingRecipe.class,
                "anvilcraft_super_heating",
                "Super Heating",
                at("anvilcraft:heater"));
        process(
                reg,
                rm,
                () -> BoilingRecipe.class,
                "anvilcraft_boiling",
                "Boiling",
                at("minecraft:campfire", "The Campfire has to be lit"));

        process(
                reg,
                rm,
                () -> ItemCrushRecipe.class,
                "anvilcraft_item_crush",
                "Item Crushing",
                at("anvilcraft:crushing_table"));
        process(
                reg,
                rm,
                () -> StampingRecipe.class,
                "anvilcraft_stamping",
                "Stamping",
                at("anvilcraft:stamping_platform", "The result is ejected from the front of the platform"));
        process(reg, rm, () -> MeshRecipe.class, "anvilcraft_mesh", "Mesh Sifting", at("minecraft:scaffolding"));
        process(
                reg,
                rm,
                () -> UnpackRecipe.class,
                "anvilcraft_unpack",
                "Unpacking",
                at("minecraft:iron_trapdoor", "The trapdoor has to be closed and set to its upper half"));
        process(
                reg,
                rm,
                () -> NeutronIrradiationRecipe.class,
                "anvilcraft_neutron",
                "Neutron Irradiation",
                at("anvilcraft:neutron_irradiator"));
        process(
                reg,
                rm,
                () -> CookingRecipe.class,
                "anvilcraft_cooking",
                "Anvil Cooking",
                at("minecraft:campfire", "The Campfire has to be lit"));
        process(
                reg,
                rm,
                () -> TimeWarpRecipe.class,
                "anvilcraft_time_warp",
                "Time Warp",
                at("anvilcraft:corrupted_beacon", "The Corrupted Beacon has to be lit and active"));
        process(
                reg,
                rm,
                () -> ItemCompressRecipe.class,
                "anvilcraft_item_compress",
                "Item Compress",
                at(CAULDRON, "The cauldron has to be empty"));

        process(reg, rm, () -> BlockSmearRecipe.class, "anvilcraft_block_smear", "Block Smear", at(ANVIL));
        process(reg, rm, () -> BlockCrushRecipe.class, "anvilcraft_block_crush", "Block Crushing", at(ANVIL));
        process(reg, rm, () -> BlockCompressRecipe.class, "anvilcraft_block_compress", "Block Compress", at(ANVIL));

        // Mixed: this one carries item AND block sides at once, which is why the mapper is unified.
        process(reg, rm, () -> ItemInjectRecipe.class, "anvilcraft_item_inject", "Item Inject", at(ANVIL));

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
        safely("Template Dissociation", () -> AnvilCraftDissociationEmiModule.register(reg));
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
     * The shared mapper for every {@link AbstractProcessRecipe}, which is also where the picture is
     * assembled. Every one of these types shares the {@code ON_ANVIL_FALL_ON} trigger, so an anvil
     * always has to land; what differs is which decks sit under it, and the recipe says that itself.
     *
     * <p>Two facts do the sorting, so no category has to declare its own shape. A recipe's input
     * block is machinery it runs on unless the recipe also declares result blocks, in which case it
     * is the thing being converted. And the block sits above the cauldron or below it according to
     * the offsets the recipe carries for rendering — above for Squeezing, where a block is pressed
     * over a cauldron; below for Super Heating and the rest, where a Heater or a lit Campfire is
     * standing under one.
     */
    private static <R extends AbstractProcessRecipe<?>> void process(
            EmiRegistry reg, RecipeManager rm, Supplier<Class<R>> cls, String key, String name, Setup setup) {
        safely(name, () -> {
            // Resolve the recipe class before declaring the category, so a type this build of
            // AnvilCraft no longer has leaves no empty tab behind.
            Class<R> type = cls.get();
            EmiRecipeCategory cat = Categories.machine(reg, key, setup.workstation(), name);
            Recipes.forEach(rm, type, (id, r) -> {
                AnvilDrop drop = new AnvilDrop();
                for (ItemIngredientPredicate p : r.getInputItems()) drop.itemMerged(predIn(p));

                HasCauldronSimple cauldron = r.getHasCauldron();
                boolean converted = !r.getResultBlocks().isEmpty();
                boolean blockUnderCauldron = cauldron != null
                        && r.getProperty().getBlockInputOffset().y
                                < r.getProperty().getCauldronOffset().y;

                boolean first = true;
                for (BlockStatePredicate bp : r.getInputBlocks()) {
                    EmiIngredient block = blockIn(bp);
                    // Every 1.5.3 type declares exactly one; a second would have nowhere of its own
                    // to stand, so it joins the payload rather than going unshown.
                    if (!first) {
                        drop.itemMerged(block);
                    } else if (blockUnderCauldron) {
                        drop.over(block);
                    } else if (cauldron != null) {
                        drop.itemMerged(block);
                    } else if (converted) {
                        drop.onto(block);
                    } else {
                        drop.on(block);
                    }
                    first = false;
                }
                if (cauldron != null) {
                    // A filled cauldron has no item form in vanilla or in AnvilCraft, so the
                    // plain one carries the vessel and the fluid tank beside it says what is in it.
                    drop.on(Categories.stack(CAULDRON));
                    drop.fluidIn(fluid(cauldron.fluid()));
                    drop.fluidOut(fluid(cauldron.transform()));
                }

                for (ChanceItemStack o : r.getResultItems()) out(drop, o);
                for (ChanceBlockState cb : r.getResultBlocks()) drop.out(blockOut(cb));
                if (setup.note() != null) {
                    drop.note(Component.literal(setup.note()));
                }
                reg.addRecipe(new AnvilDropEmiRecipe(cat, id, drop));
            });
        });
    }

    /**
     * What a process category needs beyond its recipes: the block whose tab it appears under, and a
     * note for the one thing the picture cannot draw — a block state, such as a Campfire that has to
     * be lit or a trapdoor that has to be closed.
     */
    private record Setup(String workstation, String note) {}

    private static Setup at(String workstation) {
        return new Setup(workstation, null);
    }

    private static Setup at(String workstation, String note) {
        return new Setup(workstation, note);
    }

    /** Feed items to a falling anvil to bank mass; the machine emits its product once the total is met. */
    private static void massInject(EmiRegistry reg, RecipeManager rm) {
        EmiRecipeCategory cat =
                Categories.machine(reg, "anvilcraft_mass_inject", "anvilcraft:space_overcompressor", "Mass Inject");
        Recipes.forEach(rm, MassInjectRecipe.class, (id, r) -> {
            AnvilDrop drop = new AnvilDrop();
            drop.item(EmiIngredient.of(r.getIngredient()));
            drop.on(Categories.stack("anvilcraft:space_overcompressor"));
            drop.note(r.displayMassValue());
            reg.addRecipe(new AnvilDropEmiRecipe(cat, id, drop));
        });
    }

    /** An anvil falling fast enough onto a block: converts nearby blocks and/or drops items. */
    private static void anvilCollision(EmiRegistry reg, RecipeManager rm) {
        EmiRecipeCategory cat = Categories.machine(reg, "anvilcraft_anvil_collision", ANVIL, "Anvil Collision");
        Recipes.forEach(rm, AnvilCollisionCraftRecipe.class, (id, r) -> {
            AnvilDrop drop = new AnvilDrop();
            EmiIngredient anvil = blockIn(r.anvil());
            // A consumed anvil is a real cost; a surviving one is equipment, which the catalyst
            // marking on the scene already says.
            if (r.consume()) {
                drop.anvilConsumed(anvil);
            } else {
                drop.anvil(anvil);
            }
            // The blocks it converts lie beside the one it lands on, so they share the payload deck.
            for (BlockTransform t : r.transformBlocks()) {
                drop.itemMerged(blockIn(t.inputBlock()));
                drop.out(blockOut(t.outputBlock()));
            }
            drop.onto(blockIn(r.hitBlock()));
            for (ChanceItemStack o : r.outputItems()) out(drop, o);
            drop.note(Component.literal("Needs the anvil falling at " + r.speed() + " m/tick"));
            reg.addRecipe(new AnvilDropEmiRecipe(cat, id, drop));
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
        d.itemOut(chanceStack(c, d.info));
    }

    private static void out(AnvilDrop d, ChanceItemStack c) {
        d.out(chanceStack(c, d.info));
    }

    /** The result stack itself; a range it cannot show becomes a line appended to {@code notes}. */
    private static EmiStack chanceStack(ChanceItemStack c, List<Component> notes) {
        if (c == null) {
            return null;
        }
        ItemStack s = c.stack();
        if (s == null || s.isEmpty()) {
            return null;
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
                    notes.add(
                            Component.literal(s.getHoverName().getString() + ": " + amount(low) + "-" + amount(high)));
                }
            }
            default -> {}
        }
        return out;
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
