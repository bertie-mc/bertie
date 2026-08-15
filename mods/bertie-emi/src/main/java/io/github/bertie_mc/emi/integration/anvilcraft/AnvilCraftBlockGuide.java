package io.github.bertie_mc.emi.integration.anvilcraft;

import static io.github.bertie_mc.emi.integration.anvilcraft.AnvilCraftGuideEmiModule.page;

import dev.emi.emi.api.EmiRegistry;
import java.util.List;

/**
 * Guide pages for AnvilCraft's blocks and machines — the half of the mod that is neither a recipe
 * nor a material, and which a recipe row therefore says nothing about at all.
 *
 * <p>Written from the 1.6 guide book and then filtered against 1.5.3: the Fish Tank, Trading
 * Station, Burning Heater, Batch Cutter, Smart Block Placer, Structure Scanner, the fluid tanks, the
 * Experience Collector, the Large Laser and the entire Frost tier are 1.6 additions and have no page
 * here. {@link AnvilCraftGuideEmiModule#page} drops any item the pack does not have, so a page whose
 * subjects are all missing never registers.
 */
final class AnvilCraftBlockGuide {
    private AnvilCraftBlockGuide() {}

    static void register(EmiRegistry reg) {
        logistics(reg);
        redstone(reg);
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
                "Its screen sets the output direction and per-slot limits; scroll on a slot to cap it. A"
                        + " Filter gives finer control, and a redstone signal locks it.",
                "Chain chutes together and the one being fed becomes a Simple Chute: one stack, no"
                        + " pulling, no filters.");
        page(
                reg,
                "item_collector",
                List.of("anvilcraft:item_collector"),
                "Vacuums up dropped items over a wide area, with filters.",
                "Range and cooldown are yours to set, and both drive its power draw — the screen shows"
                        + " the current figure. It stops when power runs short.");
        page(
                reg,
                "block_placer",
                List.of("anvilcraft:block_placer"),
                "Places blocks taken from whatever is behind it — a chest, a chute, even a chest boat.",
                "On a redstone signal it places one block in front.",
                "Struck by an anvil instead, it places blocks spaced by the anvil's fall height, which is"
                        + " what makes it useful for laying anvil tracks.",
                "Pistons can push it.");
        page(
                reg,
                "block_devourer",
                List.of("anvilcraft:block_devourer"),
                "Destroys the blocks in front of it and pushes the drops into whatever sits behind.",
                "A redstone signal clears 3x3. An anvil landing on it clears 5x5, 7x7 or 9x9 depending on"
                        + " whether the anvil fell one, two or three blocks.",
                "Drops that cannot fit behind it fall on the floor.");
        page(
                reg,
                "batch_crafter",
                List.of("anvilcraft:batch_crafter"),
                "On a redstone signal it performs up to sixty-four crafts at once.",
                "Idles at 4 kW. Filters and per-slot limits are set in its screen.",
                "Shift-right-click it with a crafter or a stonecutter to switch which of the two it does.");
        page(
                reg,
                "sliding_rail",
                List.of(
                        "anvilcraft:sliding_rail",
                        "anvilcraft:sliding_rail_stop",
                        "anvilcraft:powered_sliding_rail",
                        "anvilcraft:activator_sliding_rail",
                        "anvilcraft:detector_sliding_rail"),
                "Rails for items and blocks rather than minecarts: anything pushed onto them keeps" + " sliding.",
                "A block shoved on by a piston slides, and anything stuck to it — slime blocks and their"
                        + " cargo — slides along as one piece.",
                "The Stop version has enormous friction: items halt at its centre, blocks halt on top of" + " it.");
    }

    private static void redstone(EmiRegistry reg) {
        page(
                reg,
                "block_comparator",
                List.of("anvilcraft:block_comparator"),
                "Emits a signal forward while the blocks on either side of it match.",
                "Normal mode compares the block. Right-click for precision mode, which compares the full"
                        + " block state as well.");
        page(
                reg,
                "pulse_generator",
                List.of("anvilcraft:pulse_generator"),
                "Turns the signal behind it into a pulse of your chosen shape.",
                "The screen sets the trigger condition, the delay and the pulse length — the three things"
                        + " you need to time an anvil drop.");
        page(
                reg,
                "item_detector",
                List.of("anvilcraft:item_detector"),
                "Watches for items and emits redstone when it finds what it is looking for. Filters are"
                        + " set in its screen.");
        page(
                reg,
                "controllable_sand",
                List.of("anvilcraft:controllable_sand"),
                "Ignores gravity until a redstone signal reaches it, then moves up or down at random.",
                "Block one direction and it must go the other way, which is how you steer it.",
                "It carries players along with it.");
        page(
                reg,
                "active_silencer",
                List.of("anvilcraft:active_silencer"),
                "Mutes the sounds you pick within thirty-one blocks of itself — worth having next to an"
                        + " anvil farm.",
                "Search for the sounds in its screen; a Disk copies the settings to another one.");
    }

    private static void anvilTiers(EmiRegistry reg) {
        page(
                reg,
                "royal_anvil",
                List.of("anvilcraft:royal_anvil"),
                "Never takes damage, and never refuses work as Too Expensive.",
                "Special characters in a name tag will style an item's name.");
        page(
                reg,
                "ember_anvil",
                List.of("anvilcraft:ember_anvil"),
                "Never takes damage, and does everything the Royal Anvil does.",
                "It will also force enchantments onto an item that normally could not carry them"
                        + " together, in survival.");
        page(
                reg,
                "transcendence_anvil",
                List.of("anvilcraft:transcendence_anvil"),
                "Never takes damage, and does everything the Ember Anvil does.",
                "It ignores enchantment level caps when combining, and the work penalty climbs by one"
                        + " each time instead of doubling.");
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
                "Made by dropping an anvil into an end portal. The better the anvil, the better the"
                        + " odds: 1% for a damaged anvil, 2% chipped, 3% plain, 50% Royal, and certain for"
                        + " Ember or Transcendence.",
                "It ignores gravity, takes no damage, and pistons can push it.",
                "When the magnet above it lets go it drops a phantom image downward, and that image"
                        + " always strikes as though it fell two blocks, however far it actually travels.");
    }

    private static void smithing(EmiRegistry reg) {
        page(
                reg,
                "royal_smithing_table",
                List.of("anvilcraft:royal_smithing_table"),
                "Smithing here consumes no template at all.",
                "It is the thing to build first with Royal Steel — every template you own becomes"
                        + " reusable from that moment on.");
        page(
                reg,
                "royal_grindstone",
                List.of("anvilcraft:royal_grindstone"),
                "Spend a Gold Ingot here to strip an item's curses and its accumulated work penalty while"
                        + " leaving the ordinary enchantments alone.",
                "The gold comes out the other side as a Cursed Gold Ingot, which is where that material"
                        + " comes from.");
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
                "Right-click a block with one in hand to bind it, then place it. It uses sunlight to heat"
                        + " the bound block and the one above it, so a single mirror heats two things.",
                "Group them for more heat. Sixty-four of them is worth about 512 kW once converted.");
        page(
                reg,
                "laser",
                List.of("anvilcraft:ruby_laser", "anvilcraft:ruby_prism", "anvilcraft:laser_receiver"),
                "Straight beams, stopped by any opaque block. Each has a level, and the level decides what"
                        + " it can do.",
                "Damage is the level minus four, capped at sixteen.",
                "Lasers heat heatable blocks, and they bore ore straight out of stone: the ore is ejected"
                        + " from the back of the prism that fired, or into a container behind it.",
                "Getting a system running takes a lot of ruby, so set up production first.");
        page(
                reg,
                "tesla_tower",
                List.of("anvilcraft:tesla_tower"),
                "Every four seconds it strikes the nearest mob or lightning rod within eight blocks for"
                        + " ten damage. The screen holds a whitelist.",
                "Draws 128 kW while working and stops when power is short. Redstone switches it off.");
        page(
                reg,
                "propel_piston",
                List.of("anvilcraft:propel_piston"),
                "Walks forward under redstone or a right-click, shoving blocks as it goes. The more it"
                        + " pushes the more it costs.",
                "It stops when you right-click it again, when it runs dry, or when it meets something it"
                        + " cannot move.",
                "Charge it from a capacitor, or aim a laser at its back — that gives fifteen kilowatts"
                        + " per level of laser.");
        page(
                reg,
                "large_electromagnet",
                List.of("anvilcraft:acceleration_ring", "anvilcraft:deflection_ring"),
                "Both come from multi-block conversion. An acceleration ring draws 256 kW and is switched"
                        + " off by redstone.",
                "Facing up and powered, it pulls a Giant Anvil to it from twelve blocks away, provided"
                        + " nothing sits in between.",
                "Two of them facing the same way form an acceleration interval, which speeds anvils,"
                        + " projectiles and players along the line between them. Giant and spectral anvils"
                        + " are the exceptions.");
        page(
                reg,
                "induction_light",
                List.of("anvilcraft:induction_light"),
                "A lamp that takes on a job depending on what you right-click it with, drawing 1 kW" + " while idle.",
                "Feed it redstone, for instance, and it accelerates crops in a 5x5x5 area — provided the"
                        + " light level there is at least ten.");
    }

    private static void oddities(EmiRegistry reg) {
        page(
                reg,
                "crab_trap",
                List.of("anvilcraft:crab_trap"),
                "Floats on water and fishes for you. At least three of the four blocks around it must be"
                        + " water sources or waterlogged.",
                "The catch varies a little by biome, but every biome yields Crab Claws.",
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
                List.of(
                        "anvilcraft:space_overcompressor",
                        "anvilcraft:nesting_shulker_box",
                        "anvilcraft:over_nesting_shulker_box",
                        "anvilcraft:supercritical_nesting_shulker_box"),
                "This is where Neutronium comes from. Put any metal block, ingot or nugget on top and"
                        + " strike it with an anvil to bank its mass.",
                "Once enough mass has accumulated it drops a Neutronium Ingot out of the bottom.",
                "The nesting shulker boxes behave like ordinary ones and are destroyed by pistons, so"
                        + " keep them away from your machinery.");
        page(
                reg,
                "menger_sponge",
                List.of("anvilcraft:menger_sponge"),
                "Built by multi-block crafting, so mass-produce ordinary sponges first.",
                "Placed, it annihilates any fluid within six blocks and never saturates. Held, it empties"
                        + " a cauldron, and a dispenser can do that for you.",
                "It also enlarges tanks: a fluid tank at the centre of a 3x3x3 of these holds far more"
                        + " than it otherwise would.");
        page(
                reg,
                "flint_block",
                List.of("anvilcraft:flint_block"),
                "Rub a Block of Iron against one with pistons and the shove lights everything flammable"
                        + " in a 3x3x3 around the pushed block — oil cauldrons and campfires first.",
                "With nothing to light, it simply starts a fire.",
                "A stonecutter turns it into building blocks.");
        page(
                reg,
                "gunpowder_block",
                List.of("anvilcraft:gunpowder_block"),
                "Detonates when an anvil lands on it, or when lit by flint and steel or a fire charge. A"
                        + " Giant Anvil will not set it off.",
                "The blast only knocks things back — it shoves the anvil that hit it back where it came"
                        + " from, which makes it a way to reset a drop without a magnet.");
        page(
                reg,
                "sugar_block",
                List.of("anvilcraft:sugar_block"),
                "An anvil landing on one generates a single charge of power.",
                "Each strike has a 5% chance to crack it, and the fourth crack breaks it into nine" + " sugar.");
        page(
                reg,
                "rotten_flesh_block",
                List.of("anvilcraft:rotten_flesh_block"),
                "Breaks a fall like a hay bale, at the cost of thirty seconds of nausea for whoever lands" + " on it.",
                "It can be smelted.");
    }
}
