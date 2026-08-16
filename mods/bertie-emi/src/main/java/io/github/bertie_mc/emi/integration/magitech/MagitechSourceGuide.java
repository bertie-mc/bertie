package io.github.bertie_mc.emi.integration.magitech;

import dev.emi.emi.api.EmiRegistry;
import io.github.bertie_mc.emi.framework.InfoPages;
import java.util.List;

/**
 * The two things about Magitech's items that nothing else in the pack can tell you.
 *
 * <p>Of its 169 registered items and blocks, 48 are produced by no recipe. Most of those are mined,
 * harvested or dropped, and Advanced Loot Info already gives them a proper page from their loot
 * table — ores, clusters, trees, mana berries, the Weaver's drops. Nothing is added by repeating
 * that here, so this covers only the two cases a loot table cannot express.
 */
final class MagitechSourceGuide {
    private MagitechSourceGuide() {}

    private static final String UNUSED_1 =
            "Nothing in Magitech 1.1.3 produces this item. It is registered and shown in the creative"
                    + " tab, but no recipe, loot table or mechanic in the mod makes it, and nothing takes"
                    + " it as an ingredient.";
    private static final String UNUSED_2 = "Unfinished content rather than something to hunt for — creative mode only.";

    static void register(EmiRegistry reg) {
        unused(reg);
        flasks(reg);
    }

    /**
     * Registered, in the creative tab, and referenced by nothing else in the mod. Worth saying out
     * loud: an empty recipe tab looks the same whether an item is mined, dropped or simply never
     * finished, and only one of those is worth going to look for.
     */
    private static void unused(EmiRegistry reg) {
        page(reg, "chromium_ingot", List.of("magitech:chromium_ingot"), UNUSED_1, UNUSED_2);
        page(
                reg,
                "polished_materials",
                List.of(
                        "magitech:polished_abyssite",
                        "magitech:polished_frigidite",
                        "magitech:polished_redstone_crystal",
                        "magitech:polished_resonite",
                        "magitech:polished_translucium"),
                UNUSED_1,
                UNUSED_2,
                "Note that Polished Alchecrysite is a different thing entirely — that one is a real"
                        + " building block with an ordinary crafting recipe.");
    }

    /** Filled through a fluid-handler capability, so there is neither a recipe nor a loot table. */
    private static void flasks(EmiRegistry reg) {
        page(
                reg,
                "flasks",
                List.of(
                        "magitech:water_flask",
                        "magitech:lava_flask",
                        "magitech:sulfuric_acid_flask",
                        "magitech:mana_potion_flask",
                        "magitech:magic_potion_flask",
                        "magitech:healing_potion_flask",
                        "magitech:ember_potion_flask",
                        "magitech:glace_potion_flask",
                        "magitech:flow_potion_flask",
                        "magitech:hollow_potion_flask",
                        "magitech:phantom_potion_flask",
                        "magitech:surge_potion_flask",
                        "magitech:tremor_potion_flask"),
                "These are not crafted. Each one is an empty Alchemical Flask filled with the matching"
                        + " fluid, so they are made by filling rather than by a recipe.",
                "The potion fluids themselves come out of the Zardius Crucible — check that tab for the"
                        + " fluid you want, then put it in a flask.");
    }

    private static void page(EmiRegistry reg, String key, List<String> itemIds, String... lines) {
        InfoPages.page(reg, "magitech/source/" + key, itemIds, lines);
    }
}
