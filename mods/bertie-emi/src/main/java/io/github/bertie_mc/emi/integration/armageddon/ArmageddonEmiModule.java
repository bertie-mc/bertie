package io.github.bertie_mc.emi.integration.armageddon;

import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiStack;
import io.github.bertie_mc.emi.framework.Categories;
import io.github.bertie_mc.emi.framework.GenericEmiRecipe;
import io.github.bertie_mc.emi.framework.MachineDescriptor;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/**
 * Armageddon's treasure bags, as a loot table rather than a sentence. Each boss drops a bag, and
 * right-clicking it spawns a fixed set of items — some always, some on a roll, some in a range. That
 * is a loot table in everything but form: the mod is built in MCreator, so it is a generated
 * procedure with no table or modifier behind it, and no viewer could read it.
 *
 * <p>One entry per bag, showing what it opens into with each item's odds and count, so a bag can be
 * read before it is opened and any item in one can be traced back to it. The kills that drop the
 * bags are on the bosses' own pages in Advanced Loot Info; see {@code ArmageddonDropsAliPlugin}.
 *
 * <p>The contents are transcribed from those procedures, which are fixed generated code with nothing
 * to read at runtime.
 */
public final class ArmageddonEmiModule {

    private static final String MOD = "armageddon_mod";

    private ArmageddonEmiModule() {}

    /** One item out of a bag: how many, and how often. */
    private record Loot(String item, int min, int max, float chance) {}

    /** A bag and everything it opens into. */
    private record Bag(String bag, List<Loot> loot) {}

    private static Loot always(String item) {
        return new Loot(item, 1, 1, 1.0F);
    }

    private static Loot amount(String item, int min, int max) {
        return new Loot(item, min, max, 1.0F);
    }

    private static Loot chance(String item, float chance) {
        return new Loot(item, 1, 1, chance);
    }

    private static final List<Bag> BAGS = List.of(
            new Bag(
                    "arion_treasurebag",
                    List.of(
                            chance("gemwoven_fragment", 0.35F),
                            chance("arion_heart", 0.25F),
                            chance("arion_helmet_helmet", 0.25F),
                            chance("the_emerald_wrath", 0.2F))),
            new Bag(
                    "the_bringer_of_doom_treasure_bag",
                    List.of(always("doom_powder"), always("sealed_bottle"), always("sigil_of_calamity"))),
            new Bag(
                    "eldorath_treasure_bag",
                    List.of(chance("the_worldshaper_axe", 0.25F), chance("ancient_builders_charm", 0.25F))),
            new Bag(
                    "elvenite_paladin_treasure_bag",
                    List.of(
                            always("elvenite_ore"),
                            always("raw_elvenite"),
                            always("elvenite_ingot"),
                            chance("the_titans_oath", 0.21F),
                            chance("elvenite_bell", 0.13F))),
            new Bag(
                    "goblin_lord_treasure_bag",
                    List.of(always("gilded_nugget"), always("gilded_plate"), always("gilded_ingot_smithing_template"))),
            new Bag(
                    "iron_colossus_treasure_bag",
                    List.of(chance("iron_colossus_arm", 0.25F), chance("colossal_iron_ingot", 0.25F))),
            new Bag(
                    "nyxaris_the_veil_of_oblivion_treasure_bag",
                    List.of(always("shadow_fragment"), chance("lance_of_the_veil", 0.2F))),
            new Bag(
                    "sanghor_lord_of_blood_treasure_bag",
                    List.of(
                            chance("vampiric_talisman", 0.5F),
                            chance("bloody_ingot", 0.5F),
                            chance("hemalith_catalyst", 0.5F),
                            chance("bloody_block", 0.09F))),
            new Bag(
                    "the_calamities_treasure_bag",
                    List.of(
                            always("spiritof_chaos"),
                            always("spiritof_dread"),
                            always("spirit_of_despair"),
                            chance("calamitous_core", 0.25F))),
            new Bag(
                    "vaedric_treasure_bag",
                    List.of(
                            always("voiderite_ore"),
                            always("voiderite"),
                            chance("endermen_totem", 0.25F),
                            chance("the_lost_voidblade", 0.2F))),
            new Bag(
                    "zoranth_treasure_bag",
                    List.of(
                            chance("scytheofthe_withered_shadow", 0.2F),
                            chance("zoranths_helmet_helmet", 0.2F),
                            chance("zoranths_hand", 0.16F))),
            new Bag(
                    "zoranth_newborn_of_the_zenith_treasure_bag",
                    List.of(
                            always("helionite_ingot"),
                            always("helionite_smithing_template"),
                            chance("obsidian_skull", 0.2F),
                            chance("the_eye_of_the_sun", 0.2F))),
            new Bag(
                    "elder_guardian_treasurebag",
                    List.of(always("elder_guardian_lore_scroll"), amount("ancient_prism", 6, 12))),
            new Bag(
                    "ender_dragon_treasurebag",
                    List.of(
                            always("rustyswordofthelegendaryhero"),
                            always("dragons_fang"),
                            amount("khyros_tear", 28, 40),
                            amount("ender_dragon_bone", 9, 15))));

    public static void register(EmiRegistry reg) {
        EmiRecipeCategory bags = Categories.machineNoStation(
                reg, "armageddon_treasure_bag", MOD + ":ender_dragon_treasurebag", "Treasure Bag");
        for (Bag bag : BAGS) {
            EmiStack sack = Categories.stack(MOD + ":" + bag.bag());
            if (sack.isEmpty()) {
                continue;
            }
            MachineDescriptor d = new MachineDescriptor();
            d.itemIn(sack);
            for (Loot loot : bag.loot()) {
                EmiStack stack = Categories.stack(MOD + ":" + loot.item());
                if (stack.isEmpty()) {
                    continue;
                }
                // The slot carries one number, so a range shows its floor and says the rest below.
                stack.setAmount(Math.max(1, loot.min()));
                if (loot.chance() < 1.0F) {
                    stack.setChance(loot.chance());
                }
                d.itemOut(stack);
                if (loot.max() != loot.min()) {
                    d.info(Component.literal(stack.getName().getString() + ": " + loot.min() + " to " + loot.max()));
                }
            }
            reg.addRecipe(new GenericEmiRecipe(
                    bags, ResourceLocation.fromNamespaceAndPath("bertieemi", MOD + "/bag/" + bag.bag()), d));
        }
    }
}
