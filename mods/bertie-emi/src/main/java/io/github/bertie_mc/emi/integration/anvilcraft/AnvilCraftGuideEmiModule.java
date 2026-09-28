package io.github.bertie_mc.emi.integration.anvilcraft;

import dev.emi.emi.api.EmiRegistry;
import io.github.bertie_mc.emi.framework.InfoPages;
import java.util.List;

/**
 * What is left over once the recipes have been drawn: an EMI Information page for the few AnvilCraft
 * mechanics that are not a recipe at all.
 *
 * <p>Each machine used to carry a page describing the structure to build on it. Those are gone —
 * {@link AnvilDropEmiRecipe} draws the structure on every recipe in the category instead, which is
 * both shorter and impossible to get out of step with the recipe. What remains is the handling that
 * no entry can show: how to make an anvil fall more than once, and the two blocks that work without
 * one.
 *
 * <p>Claims here are checked against what 1.5.3 actually ships rather than against the mod's guide
 * book, which is a 1.6 addition. The wording is ours; the book's own text is AnvilCraft's to
 * distribute, not ours.
 */
final class AnvilCraftGuideEmiModule {
    private AnvilCraftGuideEmiModule() {}

    static void register(EmiRegistry reg) {
        AnvilCraftEmiModule.safely("guide pages", () -> {
            page(
                    reg,
                    "anvil",
                    List.of("minecraft:anvil"),
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
                    "charger",
                    List.of("anvilcraft:charger", "anvilcraft:discharger"),
                    "No anvil involved here, unlike most of the mod.",
                    "The Charger pushes energy into an item and the Discharger pulls it back out, which"
                            + " is how capacitors are filled and emptied.");
            AnvilCraftBlockGuide.register(reg);
            AnvilCraftMaterialGuide.register(reg);
            AnvilCraftFeatureGuide.register(reg);
            AnvilCraftToolGuide.register(reg);
        });
    }

    /** One Information page, attached to every one of the given items that this pack actually has. */
    static void page(EmiRegistry reg, String key, List<String> itemIds, String... lines) {
        InfoPages.page(reg, "anvilcraft/guide/" + key, itemIds, lines);
    }
}
