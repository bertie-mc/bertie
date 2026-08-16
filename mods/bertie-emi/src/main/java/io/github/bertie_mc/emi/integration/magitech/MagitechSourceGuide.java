package io.github.bertie_mc.emi.integration.magitech;

import dev.emi.emi.api.EmiRegistry;
import io.github.bertie_mc.emi.framework.InfoPages;
import java.util.List;

/**
 * The two things about Magitech's items that nothing else in the pack can tell you.
 *
 * <p>Of its 169 registered items and blocks, 117 are made by a recipe and 27 more come from a loot
 * table, which Advanced Loot Info already gives a proper page. That leaves 25: the thirteen flasks,
 * the Weaver's spawn egg, the guide book, and ten that simply do not exist yet.
 *
 * <p>Counting that correctly needs care. Magitech's {@code tool_material} recipes take an item and
 * "produce" a tool material of the same name, so a naive scan of result fields reports Abyssite and
 * its three siblings as craftable when nothing makes them at all.
 */
final class MagitechSourceGuide {
    private MagitechSourceGuide() {}

    static void register(EmiRegistry reg) {
        wip(reg);
        flasks(reg);
    }

    /**
     * Registered, given a model and put in the creative tab, then referenced by no recipe, no loot
     * table and no code path. The four raw materials are as dead as the polished forms made from
     * them — the only thing naming them is the {@code tool_material} declaration that consumes them.
     */
    private static void wip(EmiRegistry reg) {
        InfoPages.page(
                reg,
                "magitech/source/wip",
                List.of(
                        "magitech:chromium_ingot",
                        "magitech:abyssite",
                        "magitech:frigidite",
                        "magitech:resonite",
                        "magitech:translucium",
                        "magitech:polished_abyssite",
                        "magitech:polished_frigidite",
                        "magitech:polished_redstone_crystal",
                        "magitech:polished_resonite",
                        "magitech:polished_translucium"),
                "WIP");
    }

    /** Filled through a fluid-handler capability, so there is neither a recipe nor a loot table. */
    private static void flasks(EmiRegistry reg) {
        InfoPages.page(
                reg,
                "magitech/source/flasks",
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
}
