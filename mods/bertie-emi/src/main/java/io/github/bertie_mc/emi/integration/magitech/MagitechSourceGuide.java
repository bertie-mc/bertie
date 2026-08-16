package io.github.bertie_mc.emi.integration.magitech;

import dev.emi.emi.api.EmiRegistry;
import io.github.bertie_mc.emi.framework.InfoPages;
import java.util.List;

/**
 * Where Magitech items come from when no recipe makes them.
 *
 * <p>Of Magitech 1.1.3's 169 registered items and blocks, 48 are produced by no recipe anywhere in
 * this pack. Almost all of those are mined, harvested, dropped or filled, and an empty recipe tab
 * tells you none of that. These pages say which.
 *
 * <p>Six of them are produced by nothing at all: registered, given a model and placed in the
 * creative tab, then referenced by no recipe, no loot table and no code path in the mod. They get a
 * page saying so, because "no recipe" and "unfinished" look identical in EMI and only one of them is
 * worth spending time hunting for.
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
        mined(reg);
        dropped(reg);
        flasks(reg);
    }

    /** Registered, in the creative tab, and referenced by nothing else in the mod. */
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

    private static void mined(EmiRegistry reg) {
        page(
                reg,
                "redstone_crystal",
                List.of("magitech:redstone_crystal"),
                "Mined, not crafted. Break a Redstone Crystal Cluster to get them.",
                "They also generate in the barrels of Engineer Lodges.",
                "Redstone Crystals convert back into ordinary redstone, so a cluster is worth taking.");
        page(
                reg,
                "sulfur",
                List.of("magitech:sulfur"),
                "Mined, not crafted. Break a Sulfur Crystal Cluster to get it.",
                "Sulfur is what Gunpowder and Sulfuric Acid are made from here.");
        page(
                reg,
                "clusters",
                List.of(
                        "magitech:redstone_crystal_cluster",
                        "magitech:sulfur_crystal_cluster",
                        "magitech:fluorite_crystal_cluster"),
                "Generated in the world, like amethyst. Break one for its crystals; the cluster itself"
                        + " is not craftable.");
        page(
                reg,
                "ores",
                List.of(
                        "magitech:zinc_ore",
                        "magitech:deepslate_zinc_ore",
                        "magitech:fluorite_ore",
                        "magitech:deepslate_fluorite_ore",
                        "magitech:tourmaline_ore",
                        "magitech:deepslate_tourmaline_ore"),
                "Ordinary worldgen ore — dig for it. Smelting or blasting the ore gives the material,"
                        + " and Raw Zinc comes from the zinc ore directly.");
        page(
                reg,
                "trees",
                List.of(
                        "magitech:celifern_log",
                        "magitech:celifern_sapling",
                        "magitech:charcoal_birch_log",
                        "magitech:charcoal_birch_sapling"),
                "Magitech's trees grow in the world; chop the logs and replant the saplings.",
                "The stripped logs come from using an axe on a placed log, which is an interaction rather"
                        + " than a recipe, so they show nothing here either.");
        page(
                reg,
                "foraged",
                List.of("magitech:mana_berries", "magitech:mistalia_petals"),
                "Harvested from their own plants out in the world.",
                "Mana Berries also turn up in Engineer Lodge chests, and they are an ingredient in the"
                        + " Zardius Crucible's Mana Potion.");
    }

    private static void dropped(EmiRegistry reg) {
        page(
                reg,
                "aggregated",
                List.of("magitech:aggregated_fluxia", "magitech:aggregated_luminis", "magitech:aggregated_noctis"),
                "Dropped by the Weaver. There is no recipe for any of the three — you have to go and kill" + " one.");
    }

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
