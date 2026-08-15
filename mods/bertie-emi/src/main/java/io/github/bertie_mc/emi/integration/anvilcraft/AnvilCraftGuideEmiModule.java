package io.github.bertie_mc.emi.integration.anvilcraft;

import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.EmiInfoRecipe;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import io.github.bertie_mc.emi.framework.Categories;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/**
 * The "how does this actually work" layer for AnvilCraft: an EMI Information page on each machine
 * and each block the mod processes on top of.
 *
 * <p>AnvilCraft's recipes are in-world structures, not machine GUIs, and a recipe row can only ever
 * say what goes in and what comes out. The mod answers that with a guide book — but the book only
 * exists from 1.6 onwards, and the pack is pinned to 1.5.3. These pages are written from reading
 * that book and then checking every claim against what 1.5.3 actually ships: the recipe classes'
 * hardcoded block predicates, their cauldron offsets, and the recipe JSON. Anything the book
 * describes that 1.5.3 has no code for is left out.
 *
 * <p>The wording is ours. The book's own text is AnvilCraft's to distribute, not ours.
 */
final class AnvilCraftGuideEmiModule {
    private AnvilCraftGuideEmiModule() {}

    static void register(EmiRegistry reg) {
        AnvilCraftEmiModule.safely("guide pages", () -> {
            page(
                    reg,
                    "anvil",
                    List.of("minecraft:anvil"),
                    "Nearly everything in AnvilCraft happens by dropping an anvil. What you get depends"
                            + " entirely on what is underneath it.",
                    "A block under the anvil is transformed. Items lying on that block, or floating in it"
                            + " if it is a cauldron, are processed instead.",
                    "An anvil that falls two blocks or more can chip, so a short drop is the cheap one.",
                    "Hitting a note block next to a held anvil is the simplest way to make it fall once.");
            page(
                    reg,
                    "magnet",
                    List.of("anvilcraft:magnet_block", "anvilcraft:hollow_magnet_block"),
                    "Holds an anvil up so it can be dropped again. Cut the redstone signal and the anvil"
                            + " falls; restore it and the anvil is pulled back up.",
                    "This is what turns a one-off anvil drop into a machine.",
                    "Time Warp needs the Hollow Magnet Block, so the Corrupted Beacon's beam is not" + " blocked.");
            page(
                    reg,
                    "cauldron",
                    List.of("minecraft:cauldron"),
                    "Throw items into a cauldron and land an anvil on it. Which process runs depends on"
                            + " what sits under the cauldron.",
                    "Nothing underneath, water inside: Bulging. It spends one layer of water.",
                    "Nothing underneath, no water: Item Compress, which runs that item's own 2x2 or 3x3"
                            + " crafting recipe, preferring the 3x3 where both exist.",
                    "A lit Campfire underneath: Cooking, covering every smoker and campfire recipe.",
                    "A Heater underneath: Super Heating.",
                    "A block above the cauldron rather than items inside it: Squeezing, which converts the"
                            + " block and leaves its fluid in the cauldron.");
            page(
                    reg,
                    "heater",
                    // The fuel-burning Heater is a 1.6 addition; 1.5.3 ships only the electric one.
                    List.of("anvilcraft:heater"),
                    "Heats whatever sits on top of it. With a cauldron up there, that is Super Heating.",
                    "Super Heating runs furnace and blast furnace recipes a batch at a time, adds recipes"
                            + " of its own, and doubles what ore smelting gives you.",
                    "It will not cook food. Put a lit Campfire under the cauldron instead.",
                    "Draws 16 kW without pause and stops the moment power runs short.");
            page(
                    reg,
                    "crushing_table",
                    List.of("anvilcraft:crushing_table"),
                    "Items on top, anvil down, results drop out underneath.",
                    "It breaks tools, weapons and armour back down into the materials they were made"
                            + " from, and returns far more of it than smelting would.",
                    "It also runs every Block Crushing recipe, at a 20% loss for the convenience.");
            page(
                    reg,
                    "stamping_platform",
                    List.of("anvilcraft:stamping_platform"),
                    "Items on top, anvil down. The result is ejected from the front of the platform.",
                    "It presses things flat: ingots into pressure plates, snowballs into snowflakes.");
            page(
                    reg,
                    "mesh",
                    List.of("minecraft:scaffolding"),
                    "Items on the scaffolding, anvil down, results fall through it.",
                    "Sifting hands roughly half the material back as a byproduct, so it can be run again.");
            page(
                    reg,
                    "unpack",
                    List.of("minecraft:iron_trapdoor"),
                    "The trapdoor has to be closed and set to its upper half. Items on top, anvil down,"
                            + " results appear below.",
                    "This is the undo for packing: an ingot back into nine nuggets, a melon into slices,"
                            + " blocks of quartz and amethyst back into their gems.");
            page(
                    reg,
                    "corrupted_beacon",
                    List.of("anvilcraft:corrupted_beacon"),
                    "Time Warp only runs while the beacon is lit and active.",
                    "Metal blocks wind back into the raw ore they came from; other things are pushed"
                            + " forward instead.",
                    "To get one: build a beacon on a base made entirely of Blocks of Cursed Gold and"
                            + " activate it with a Cursed Gold Ingot. A deeper base converts more often —"
                            + " 2% at one layer, 5% at two, 20% at three, certain at four.",
                    "Its beam withers what it touches, and turns some mobs into nastier ones outright.");
            page(
                    reg,
                    "neutron_irradiator",
                    List.of("anvilcraft:neutron_irradiator"),
                    "Sits under a cauldron. Items in that cauldron are irradiated when an anvil lands.",
                    "Do not feed it a Block of Uranium. It detonates.");
            page(
                    reg,
                    "charger",
                    List.of("anvilcraft:charger", "anvilcraft:discharger"),
                    "No anvil involved here, unlike most of the mod.",
                    "The Charger pushes energy into an item and the Discharger pulls it back out, which"
                            + " is how capacitors are filled and emptied.",
                    "Both also run a few recipes that nothing but raw energy can perform.");
            AnvilCraftBlockGuide.register(reg);
            AnvilCraftMaterialGuide.register(reg);
            AnvilCraftFeatureGuide.register(reg);
        });
    }

    /** One Information page, attached to every one of the given items that this pack actually has. */
    static void page(EmiRegistry reg, String key, List<String> itemIds, String... lines) {
        List<EmiIngredient> stacks = new ArrayList<>();
        for (String id : itemIds) {
            EmiStack stack = Categories.stack(id);
            if (!stack.isEmpty()) {
                stacks.add(stack);
            }
        }
        if (stacks.isEmpty()) {
            return;
        }
        List<Component> text = Arrays.stream(lines)
                .map(Component::literal)
                .map(Component.class::cast)
                .toList();
        reg.addRecipe(new EmiInfoRecipe(
                stacks, text, ResourceLocation.fromNamespaceAndPath("bertieemi", "anvilcraft/guide/" + key)));
    }
}
