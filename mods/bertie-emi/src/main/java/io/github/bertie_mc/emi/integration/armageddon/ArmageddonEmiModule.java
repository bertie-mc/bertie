package io.github.bertie_mc.emi.integration.armageddon;

import dev.emi.emi.api.EmiRegistry;
import io.github.bertie_mc.emi.framework.InfoPages;
import java.util.List;

/**
 * Armageddon's boss economy. The mod is built in MCreator, so none of it is recipes or loot tables:
 * a boss death runs a procedure that spawns its relic, its lore scroll and its treasure bag, and
 * right-clicking the bag runs another that spawns the contents. Nothing in the pack could point at
 * any of it, which left well over a hundred of its items looking unobtainable.
 *
 * <p>Both halves are written out from those procedures. They are generated code with no data to
 * read at runtime, and the drops are fixed, so a page per item saying which boss it comes from — or
 * which bag — is the whole of the answer.
 */
public final class ArmageddonEmiModule {

    private static final String MOD = "armageddon_mod";

    private ArmageddonEmiModule() {}

    /** A boss, the items its death spawns, and the bag those drops include. */
    private record Boss(String name, String bag, List<String> drops, List<String> bagContents) {}

    private static final List<Boss> BOSSES = List.of(
            new Boss(
                    "Arion, Tyrant of the Emerald Wrath",
                    "arion_treasurebag",
                    List.of("arion_relic", "arion_lore_scroll"),
                    List.of("the_emerald_wrath", "arion_heart", "arion_helmet_helmet", "gemwoven_fragment")),
            new Boss(
                    "The Bringer of Doom",
                    "the_bringer_of_doom_treasure_bag",
                    List.of("bringer_of_doom_relic", "bringer_of_doom_lore_scroll"),
                    List.of("doom_powder", "sealed_bottle", "sigil_of_calamity")),
            new Boss(
                    "Eldorath, the Ancient Builder",
                    "eldorath_treasure_bag",
                    List.of("eldorath_relic", "eldorath_lore_scroll"),
                    List.of("the_worldshaper_axe", "ancient_builders_charm")),
            new Boss(
                    "the Elvenite Paladin",
                    "elvenite_paladin_treasure_bag",
                    List.of("elvenite_paladin_relic", "elvenite_paladin_lore_scroll"),
                    List.of("elvenite_ore", "raw_elvenite", "elvenite_ingot", "the_titans_oath", "elvenite_bell")),
            new Boss(
                    "the Goblin Lord",
                    "goblin_lord_treasure_bag",
                    List.of("goblin_lord_relic", "gobelin_lord_lore_scroll"),
                    List.of("gilded_nugget", "gilded_plate", "gilded_ingot_smithing_template")),
            new Boss(
                    "the Iron Colossus",
                    "iron_colossus_treasure_bag",
                    List.of("iron_colossus_relic", "iron_colossus_lore_scroll"),
                    List.of("iron_colossus_arm", "colossal_iron_ingot")),
            new Boss(
                    "Nyxaris, the Veil of Oblivion",
                    "nyxaris_the_veil_of_oblivion_treasure_bag",
                    List.of("nyxaris_the_veil_of_oblivion_relic", "nyxaris_lore_scroll"),
                    List.of("shadow_fragment", "lance_of_the_veil")),
            new Boss(
                    "Sanghor, Lord of Blood",
                    "sanghor_lord_of_blood_treasure_bag",
                    List.of("sanghor_lord_of_the_blood_relic", "sanghor_lord_of_blood_lore_scroll"),
                    List.of("vampiric_talisman", "bloody_ingot", "hemalith_catalyst", "bloody_block")),
            new Boss(
                    "the Discord",
                    "the_calamities_treasure_bag",
                    List.of(
                            "the_calamities_relic_1",
                            "the_discord_lore_scroll",
                            "the_famine_lore_scroll",
                            "the_chaos_lore_scroll"),
                    List.of("spiritof_chaos", "spiritof_dread", "spirit_of_despair", "calamitous_core")),
            new Boss(
                    "Vaedric, the Fallen Wanderer",
                    "vaedric_treasure_bag",
                    List.of("vaedric_the_fallen_wanderer_relic", "vaedric_lore_scroll"),
                    List.of("the_lost_voidblade", "endermen_totem", "voiderite_ore", "voiderite")),
            new Boss(
                    "Zoranth, the Forgotten One",
                    "zoranth_treasure_bag",
                    List.of("zoranth_relic", "zoranth_lore_scroll"),
                    List.of("zoranths_hand", "scytheofthe_withered_shadow", "zoranths_helmet_helmet")),
            new Boss(
                    "Zoranth, Newborn of the Zenith",
                    "zoranth_newborn_of_the_zenith_treasure_bag",
                    List.of("zoranth_newborn_of_the_zenith_relic", "zoranth_newborn_of_the_zenith_lore_scroll"),
                    List.of("helionite_ingot", "helionite_smithing_template", "obsidian_skull", "the_eye_of_the_sun")),
            // The two bags Armageddon hangs off vanilla bosses; neither drops a relic or a scroll of
            // its own, so the scroll below comes out of the bag rather than the kill.
            new Boss(
                    "the Elder Guardian",
                    "elder_guardian_treasurebag",
                    List.of(),
                    List.of("elder_guardian_lore_scroll", "ancient_prism")),
            new Boss(
                    "the Ender Dragon",
                    "ender_dragon_treasurebag",
                    List.of(),
                    List.of("rustyswordofthelegendaryhero", "dragons_fang", "khyros_tear", "ender_dragon_bone")));

    public static void register(EmiRegistry reg) {
        for (Boss boss : BOSSES) {
            String killed = "Dropped when " + boss.name() + " is killed.";
            for (String drop : boss.drops()) {
                InfoPages.page(reg, MOD + "/" + drop, List.of(MOD + ":" + drop), killed);
            }
            InfoPages.page(
                    reg, MOD + "/" + boss.bag(), List.of(MOD + ":" + boss.bag()), killed, "Right-click it to open.");
            String fromBag = "From the treasure bag " + boss.name() + " drops.";
            for (String content : boss.bagContents()) {
                InfoPages.page(reg, MOD + "/" + content, List.of(MOD + ":" + content), fromBag);
            }
        }
        InfoPages.page(
                reg,
                MOD + "/gilded_nugget_goblin",
                List.of(MOD + ":gilded_nugget"),
                "Also dropped by the Little Sword Goblin.");
    }
}
