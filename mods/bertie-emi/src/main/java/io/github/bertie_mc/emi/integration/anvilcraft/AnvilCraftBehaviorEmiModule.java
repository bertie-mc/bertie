package io.github.bertie_mc.emi.integration.anvilcraft;

import dev.dubhe.anvilcraft.util.SpectralAnvilConversionUtil;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import io.github.bertie_mc.emi.framework.Categories;
import io.github.bertie_mc.emi.framework.GenericEmiRecipe;
import io.github.bertie_mc.emi.framework.MachineDescriptor;
import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

/**
 * AnvilCraft's hardcoded in-world conversions — the ones with no recipe file behind them, which
 * {@link AnvilCraftEmiModule} therefore cannot find by walking the recipe manager. AnvilCraft's own
 * JEI plugin synthesises a recipe object per case; this rebuilds the same set from the item ids,
 * block tags and behaviour constants instead, so nothing here depends on JEI being installed.
 *
 * <p>Referring to blocks and items by id rather than through {@code ModBlocks}/{@code ModItems}
 * keeps the module off Registrate, which AnvilCraft bundles but does not expose to us.
 *
 * <p>Still deferred: {@code colored_concrete}, which JEI splits out for a nicer picture but which is
 * only the subset of Bulging whose result is reinforced concrete — in the generic one-row layout it
 * would be a duplicate of recipes the Bulging category already shows.
 */
final class AnvilCraftBehaviorEmiModule {
    private AnvilCraftBehaviorEmiModule() {}

    private static final String CAULDRON = "minecraft:cauldron";
    private static final String CURSED_GOLD_BLOCK = "anvilcraft:cursed_gold_block";
    private static final String VOID_MATTER_BLOCK = "anvilcraft:void_matter_block";
    private static final String OVERHEATED_EMBER_METAL_BLOCK = "anvilcraft:overheated_ember_metal_block";
    private static final String TRANSCENDIUM_NUGGET = "anvilcraft:transcendium_nugget";
    private static final String TRANSCENDIUM_INGOT = "anvilcraft:transcendium_ingot";
    private static final String NEUTRONIUM_INGOT = "anvilcraft:neutronium_ingot";

    private static final TagKey<Block> VOID_DECAY_PRODUCTS =
            TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath("anvilcraft", "void_decay_products"));

    static void register(EmiRegistry reg, EmiRecipeCategory itemInject) {
        AnvilCraftEmiModule.safely("Beacon Conversion", () -> beaconConversion(reg));
        AnvilCraftEmiModule.safely("Void Decay", () -> voidDecay(reg));
        if (itemInject != null) {
            AnvilCraftEmiModule.safely("Transcendium Recipe", () -> transcendium(reg, itemInject));
        }
        AnvilCraftEmiModule.safely("Cement Staining", () -> cementStaining(reg));
        // The only one reading an AnvilCraft class rather than ids and tags, so the only one here a
        // version bump can take away.
        AnvilCraftEmiModule.safely("Block Falls Into End Portal", () -> endPortalConversion(reg));
    }

    /**
     * A beacon on a pyramid of Blocks of Cursed Gold, activated with a Cursed Gold Ingot, turns into
     * a Corrupted Beacon. Deeper pyramids are likelier; four layers always works.
     */
    private static void beaconConversion(EmiRegistry reg) {
        EmiRecipeCategory cat =
                Categories.machine(reg, "anvilcraft_beacon_conversion", CURSED_GOLD_BLOCK, "Beacon Conversion");
        float[] chances = {0.02f, 0.05f, 0.2f, 1.0f};
        for (int layers = 1; layers <= chances.length; layers++) {
            float chance = chances[layers - 1];
            MachineDescriptor d = new MachineDescriptor();
            d.itemIn(Categories.stack("anvilcraft:cursed_gold_ingot"));
            d.itemIn(Categories.stack("minecraft:beacon"));
            d.catalyst(Categories.stack(CURSED_GOLD_BLOCK).setAmount(pyramidBlocks(layers)));
            d.itemOut(Categories.stack("anvilcraft:corrupted_beacon").setChance(chance));
            if (chance < 1.0f) {
                d.itemOut(Categories.stack("minecraft:beacon").setChance(1.0f - chance));
            }
            d.info(Component.literal(
                    "Beacon base: " + layers + (layers == 1 ? " layer" : " layers") + " of Blocks of Cursed Gold"));
            d.info(Component.literal("Activate the beacon with a Cursed Gold Ingot"));
            reg.addRecipe(new GenericEmiRecipe(cat, id("beacon_conversion/" + layers), d));
        }
    }

    /** A beacon pyramid of n layers: 3x3, then 5x5, and so on. */
    private static int pyramidBlocks(int layers) {
        int count = 0;
        for (int i = 0; i < layers; i++) {
            int side = 2 * i + 3;
            count += side * side;
        }
        return count;
    }

    /**
     * A block dropped through an end portal comes back as a Spectral Anvil, or as End Dust when it
     * does not. One entry per convertible block, so the odds per anvil tier stay readable.
     */
    private static void endPortalConversion(EmiRegistry reg) {
        EmiRecipeCategory cat = Categories.machineNoStation(
                reg, "anvilcraft_end_portal_conversion", "minecraft:end_portal_frame", "Block Falls Into End Portal");
        Object2DoubleMap<Block> chances = SpectralAnvilConversionUtil.SPECTRAL_ANVIL_CONVERSION_CHANCE;
        for (Object2DoubleMap.Entry<Block> entry : chances.object2DoubleEntrySet()) {
            Block block = entry.getKey();
            float chance = (float) entry.getDoubleValue();
            EmiStack in = EmiStack.of(block);
            if (in.isEmpty()) {
                continue;
            }
            MachineDescriptor d = new MachineDescriptor();
            d.itemIn(in);
            if (chance > 0.0f) {
                d.itemOut(Categories.stack("anvilcraft:spectral_anvil").setChance(chance));
            }
            if (chance < 1.0f) {
                d.itemOut(Categories.stack("anvilcraft:end_dust").setChance(1.0f - chance));
            }
            d.info(Component.literal("Converted when it falls through an end portal"));
            ResourceLocation key = BuiltInRegistries.BLOCK.getKey(block);
            reg.addRecipe(new GenericEmiRecipe(
                    cat, id("end_portal_conversion/" + key.getNamespace() + "/" + key.getPath()), d));
        }
    }

    /**
     * A Block of Void Matter ringed by five more of them rots, on a random tick, into one of the
     * blocks in {@code anvilcraft:void_decay_products}. One entry per product so each is findable
     * from its own item.
     */
    private static void voidDecay(EmiRegistry reg) {
        EmiRecipeCategory cat = Categories.machine(reg, "anvilcraft_void_decay", VOID_MATTER_BLOCK, "Void Decay");
        BuiltInRegistries.BLOCK.getTag(VOID_DECAY_PRODUCTS).ifPresent(products -> {
            for (Holder<Block> holder : products) {
                EmiStack out = EmiStack.of(holder.value());
                if (out.isEmpty()) {
                    continue;
                }
                MachineDescriptor d = new MachineDescriptor();
                d.itemIn(Categories.stack(VOID_MATTER_BLOCK));
                d.catalyst(Categories.stack(VOID_MATTER_BLOCK).setAmount(5));
                d.itemOut(out);
                d.info(Component.literal("The centre block converts on a random tick"));
                d.info(Component.literal("The five surrounding blocks are not consumed"));
                ResourceLocation key = BuiltInRegistries.BLOCK.getKey(holder.value());
                reg.addRecipe(
                        new GenericEmiRecipe(cat, id("void_decay/" + key.getNamespace() + "/" + key.getPath()), d));
            }
        });
    }

    /**
     * An anvil landing on an Overheated Ember Metal Block with a Charged Neutronium Ingot on top.
     * That is an item injected into a block, so the entries go in AnvilCraft's own Item Inject
     * category rather than getting a tab of their own — the mod implements it as a hardcoded
     * behaviour rather than an {@code item_inject} recipe, but a player has no way of telling and no
     * reason to care.
     *
     * <p>What comes out is decided by how many enchantments the ingot carries, which is why the five
     * entries share one picture — and why the counts that scale with the enchantment total are
     * spelled out in text rather than baked into a stack size.
     */
    private static void transcendium(EmiRegistry reg, EmiRecipeCategory cat) {
        transcendiumEntry(reg, cat, "0", "The ingot carries no enchantments", 4, false, 0, false);
        transcendiumEntry(reg, cat, "1_10", "The ingot carries 1-10 enchantments", 4, true, 3, false);
        transcendiumEntry(reg, cat, "11_14", "The ingot carries 11-14 enchantments", 4, true, 3, false);
        transcendiumEntry(reg, cat, "15", "The ingot carries 15 enchantments", 0, true, 0, true);
        transcendiumEntry(reg, cat, "16_plus", "The ingot carries 16 or more enchantments", 0, true, 1, true);
    }

    private static void transcendiumEntry(
            EmiRegistry reg,
            EmiRecipeCategory cat,
            String key,
            String enchantmentLine,
            int ingots,
            boolean neutronium,
            int nuggetsPerEnchantment,
            boolean leavesBlock) {
        AnvilDrop drop = new AnvilDrop();
        drop.item(Categories.stack("anvilcraft:charged_neutronium_ingot"));
        drop.onto(Categories.stack(OVERHEATED_EMBER_METAL_BLOCK));
        if (neutronium) {
            drop.out(Categories.stack(NEUTRONIUM_INGOT));
        }
        if (ingots > 0) {
            drop.out(Categories.stack(TRANSCENDIUM_INGOT).setAmount(ingots));
        }
        if (nuggetsPerEnchantment > 0) {
            drop.out(Categories.stack(TRANSCENDIUM_NUGGET));
        }
        if (leavesBlock) {
            drop.out(Categories.stack("anvilcraft:transcendium_block"));
        }
        drop.note(Component.literal(enchantmentLine));
        if (nuggetsPerEnchantment > 0) {
            drop.note(Component.literal(
                    "Transcendium Nuggets: " + nuggetsPerEnchantment + " per enchantment on the ingot"));
        }
        // Only the 1-10 band rolls for the Neutronium Ingot; above it the ingot is guaranteed.
        if (neutronium && "1_10".equals(key)) {
            drop.note(Component.literal("Neutronium Ingot chance: 10% per enchantment on the ingot"));
        }
        reg.addRecipe(new AnvilDropEmiRecipe(cat, id("transcendium/" + key), drop));
    }

    /**
     * A dye lying on a Cement Cauldron when an anvil lands on it restains the cauldron. The cauldrons
     * themselves have no item form, so the cement is shown as the fluid it is: any colour in, the
     * dye's colour out.
     */
    private static void cementStaining(EmiRegistry reg) {
        EmiRecipeCategory cat = Categories.machine(reg, "anvilcraft_cement_staining", CAULDRON, "Cement Staining");
        List<EmiStack> cements = new ArrayList<>();
        for (DyeColor colour : DyeColor.values()) {
            EmiStack cement = cement(colour);
            if (!cement.isEmpty()) {
                cements.add(cement);
            }
        }
        if (cements.isEmpty()) {
            return;
        }
        // Any colour of cement takes any dye, so the input cycles through the lot rather than
        // claiming one particular starting colour.
        EmiIngredient anyCement = EmiIngredient.of(cements);
        for (DyeColor colour : DyeColor.values()) {
            EmiStack result = cement(colour);
            if (result.isEmpty()) {
                continue;
            }
            AnvilDrop drop = new AnvilDrop();
            drop.item(EmiStack.of(DyeItem.byColor(colour)));
            drop.on(Categories.stack(CAULDRON));
            drop.fluidIn(anyCement);
            drop.fluidOut(result);
            reg.addRecipe(new AnvilDropEmiRecipe(cat, id("cement_staining/" + colour.getName()), drop));
        }
    }

    private static EmiStack cement(DyeColor colour) {
        Fluid fluid = BuiltInRegistries.FLUID.get(
                ResourceLocation.fromNamespaceAndPath("anvilcraft", colour.getName() + "_cement"));
        return fluid == null || fluid == Fluids.EMPTY ? EmiStack.EMPTY : EmiStack.of(fluid);
    }

    /** These conversions have no recipe file, so their EMI ids are ours to mint. */
    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("bertieemi", "anvilcraft/" + path);
    }
}
