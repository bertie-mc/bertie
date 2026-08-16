package io.github.bertie_mc.emi.integration.anvilcraft;

import static io.github.bertie_mc.emi.integration.anvilcraft.AnvilCraftGuideEmiModule.page;

import dev.emi.emi.api.EmiRegistry;
import java.util.List;

/**
 * Guide pages for AnvilCraft's blocks and machines.
 *
 * <p>AnvilCraft gives 141 of its items a hover tooltip, and several of those are more precise than
 * anything worth writing here — the Block Placer, the Block Devourer and the Induction Light all
 * document themselves completely. Those have no page: an Information tab that repeats the tooltip
 * two inches above it is worse than an empty one, because it costs a click to find out it was
 * nothing. What survives is what the tooltip does not say.
 *
 * <p>Written from the 1.6 guide book and filtered against 1.5.3: the Fish Tank, Trading Station,
 * Burning Heater, Batch Cutter, Smart Block Placer, Structure Scanner, the fluid tanks, the
 * Experience Collector, the Large Laser and the whole Frost tier are 1.6 additions and are absent
 * here.
 */
final class AnvilCraftBlockGuide {
    private AnvilCraftBlockGuide() {}

    static void register(EmiRegistry reg) {
        logistics(reg);
        anvilTiers(reg);
        smithing(reg);
        power(reg);
        oddities(reg);
    }

    private static void logistics(EmiRegistry reg) {
        page(
                reg,
                "chute",
                List.of("anvilcraft:chute", "anvilcraft:magnetic_chute"),
                "A hopper with nine slots that moves a full stack at a time, into containers or out onto"
                        + " the floor.",
                "Chain chutes together and the one being fed becomes a Simple Chute: one stack, no"
                        + " pulling, no filters. That catches people out when a line silently stops"
                        + " buffering.");
        page(
                reg,
                "sliding_rail",
                List.of("anvilcraft:sliding_rail", "anvilcraft:sliding_rail_stop"),
                "Rails for items and blocks rather than minecarts: anything pushed onto them keeps" + " sliding.",
                "A block shoved on by a piston slides, and anything stuck to it — slime blocks and their"
                        + " cargo — slides along as one piece.");
        page(
                reg,
                "controllable_sand",
                List.of("anvilcraft:controllable_sand"),
                "Ignores gravity until a redstone signal reaches it, then moves up or down at random.",
                "Block one direction and it must go the other way, which is how you steer it.",
                "It carries players along with it.");
    }

    private static void anvilTiers(EmiRegistry reg) {
        page(
                reg,
                "royal_anvil",
                List.of("anvilcraft:royal_anvil"),
                "Beyond being unbreakable: it never refuses work as Too Expensive.",
                "Special characters in a name tag will style an item's name.");
        page(
                reg,
                "ember_anvil",
                List.of("anvilcraft:ember_anvil"),
                "Does everything the Royal Anvil does, and will force enchantments onto an item that"
                        + " normally could not carry them together, in survival.");
        page(
                reg,
                "transcendence_anvil",
                List.of("anvilcraft:transcendence_anvil"),
                "Does everything the Ember Anvil does, ignores enchantment level caps when combining, and"
                        + " the work penalty climbs by one each time instead of doubling.");
        page(
                reg,
                "giant_anvil",
                List.of("anvilcraft:giant_anvil"),
                "Getting the first one is a fight. Right-click a zombie while holding an anvil to hand it"
                        + " over, then have a Corrupted Beacon irradiate that zombie.",
                "Each anvil it holds gives a 5% chance it becomes a Giant Zombie carrying the Giant"
                        + " Anvil. Kill it to take the anvil.",
                "The Giant Zombie has been given real AI and is genuinely dangerous — wall it in first. A"
                        + " Resin Block is a good way to hold the zombie still.");
        page(
                reg,
                "spectral_anvil",
                List.of("anvilcraft:spectral_anvil"),
                "Made by dropping an anvil into an end portal, and the better the anvil the better the"
                        + " odds: 1% for a damaged anvil, 2% chipped, 3% plain, 50% Royal, and certain for"
                        + " Ember or Transcendence.");
    }

    private static void smithing(EmiRegistry reg) {
        page(
                reg,
                "royal_grindstone",
                List.of("anvilcraft:royal_grindstone"),
                "The gold you spend removing a curse comes out the other side as a Cursed Gold Ingot.",
                "That is the only source of cursed gold, and the Corrupted Beacon needs a great deal of"
                        + " it — so grinding curses off is worth doing even when you do not care about the"
                        + " curse.");
        page(
                reg,
                "ember_smithing_table",
                List.of(
                        "anvilcraft:ember_smithing_table",
                        "anvilcraft:two_to_one_smithing_template",
                        "anvilcraft:four_to_one_smithing_template",
                        "anvilcraft:eight_to_one_smithing_template"),
                "Consumes no templates, but only handles the many-to-one recipes.",
                "Those need a multi-to-one template, which you make by stamping that number of different"
                        + " smithing templates on a Stamping Platform. Upgrade and armour trim templates"
                        + " both count.");
        page(
                reg,
                "ember_grindstone",
                List.of("anvilcraft:ember_grindstone"),
                "Spends experience to pull an enchantment off an item and keep it.",
                "It never says Too Expensive, but the item still picks up a work penalty.");
    }

    private static void power(EmiRegistry reg) {
        page(
                reg,
                "heliostats",
                List.of("anvilcraft:heliostats"),
                "Grouping them multiplies the heat. Sixty-four together is worth about 512 kW once the"
                        + " heat is collected and converted.");
        page(
                reg,
                "laser",
                List.of("anvilcraft:ruby_laser", "anvilcraft:ruby_prism", "anvilcraft:laser_receiver"),
                "Straight beams, stopped by any opaque block. Each has a level, and the level decides"
                        + " what it can do — damage is the level minus four, capped at sixteen.",
                "Lasers heat heatable blocks, and they bore ore straight out of stone: the ore is ejected"
                        + " from the back of the prism that fired, or into a container behind it.",
                "Getting a system running takes a lot of ruby, so set up production first.");
        page(
                reg,
                "tesla_tower",
                List.of("anvilcraft:tesla_tower"),
                "Fires every four seconds for ten damage, at the nearest valid target.",
                "The screen holds a whitelist, so it can be pointed at mobs without hitting your own"
                        + " lightning rods.");
        page(
                reg,
                "large_electromagnet",
                List.of("anvilcraft:acceleration_ring", "anvilcraft:deflection_ring"),
                "Both come from multi-block conversion. An acceleration ring draws 256 kW.",
                "Facing up and powered, it pulls a Giant Anvil to it from twelve blocks away, provided"
                        + " nothing sits in between.",
                "Two of them facing the same way form an acceleration interval, which speeds anvils,"
                        + " projectiles and players along the line between them. Giant and spectral anvils"
                        + " are the exceptions.");
    }

    private static void oddities(EmiRegistry reg) {
        page(
                reg,
                "crab_trap",
                List.of("anvilcraft:crab_trap"),
                "It only works if at least three of the four blocks around it are water sources or" + " waterlogged.",
                "Right-click it, or drop an anvil on it, to make it cough up what it has caught.");
        page(
                reg,
                "jewelcrafting_table",
                List.of("anvilcraft:jewelcrafting_table"),
                "Copies items that normally cannot be copied — banner patterns, music discs, pottery"
                        + " sherds and armour trim templates.",
                "Put the original in with the right materials and press space to fill the rest in. The"
                        + " copy carries the Curse of Vanishing.",
                "It also works as a villager workstation, giving you the Jeweler.");
        page(
                reg,
                "space_overcompressor",
                List.of("anvilcraft:space_overcompressor"),
                "Put any metal block, ingot or nugget on top and strike it with an anvil to bank its"
                        + " mass. The ingot drops out of the bottom once enough has accumulated.",
                "So it is fed by anvil strikes, not by hoppers — the metal has to sit on top of it.");
        page(
                reg,
                "menger_sponge",
                List.of("anvilcraft:menger_sponge"),
                "Built by multi-block crafting, so mass-produce ordinary sponges first.",
                "It also enlarges tanks: a fluid tank at the centre of a 3x3x3 of these holds far more"
                        + " than it otherwise would.");
        page(
                reg,
                "flint_block",
                List.of("anvilcraft:flint_block"),
                "Rub a Block of Iron against one with pistons and the shove lights everything flammable"
                        + " in a 3x3x3 around the pushed block — oil cauldrons and campfires first.",
                "With nothing to light, it simply starts a fire.");
        page(
                reg,
                "gunpowder_block",
                List.of("anvilcraft:gunpowder_block"),
                "Detonates when an anvil lands on it, or when lit. A Giant Anvil will not set it off.",
                "The blast only knocks things back — it shoves the anvil that hit it back where it came"
                        + " from, which makes it a way to reset a drop without a magnet.");
        page(
                reg,
                "sugar_block",
                List.of("anvilcraft:sugar_block"),
                "An anvil landing on one generates a single charge of power, which is the cheapest way"
                        + " into the power system.",
                "Each strike has a 5% chance to crack it, and the fourth crack breaks it into nine" + " sugar.");
    }
}
