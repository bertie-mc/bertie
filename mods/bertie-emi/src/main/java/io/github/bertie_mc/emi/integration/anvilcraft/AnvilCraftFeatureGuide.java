package io.github.bertie_mc.emi.integration.anvilcraft;

import static io.github.bertie_mc.emi.integration.anvilcraft.AnvilCraftGuideEmiModule.page;

import dev.emi.emi.api.EmiRegistry;
import java.util.List;

/**
 * Guide pages for the parts of AnvilCraft that are neither a recipe nor an item: what an anvil does
 * to mobs and to vanilla blocks, the heat and power systems, and the multiblocks.
 *
 * <p>Most of these have no item of their own in the book, so each is attached to the vanilla or mod
 * item you would actually be holding when you needed to know — Anvil Looting to the anvil, the EMP
 * to a Block of Redstone, spawner forcing to the spawner itself.
 *
 * <p>Written from the 1.6 guide book, checked against 1.5.3.
 */
final class AnvilCraftFeatureGuide {
    private AnvilCraftFeatureGuide() {}

    static void register(EmiRegistry reg) {
        anvilEffects(reg);
        heat(reg);
        electricity(reg);
        structures(reg);
    }

    private static void anvilEffects(EmiRegistry reg) {
        page(
                reg,
                "anvil_looting",
                List.of("minecraft:anvil"),
                "Anvil Looting: a mob crushed hard enough in one hit drops from its own loot table," + " multiplied.",
                "Take 40% of its health in a single blow for one lot of drops, 60% for two, 80% for" + " three.",
                "Mobs that heal themselves — witches, and iron golems you keep repairing — can be farmed"
                        + " on this indefinitely without ever killing them.");
        page(
                reg,
                "anvil_mining",
                List.of("minecraft:stonecutter"),
                "Anvil Mining is the mod's word for destroying blocks with a falling anvil, and a"
                        + " stonecutter under the target is the simplest form of it.",
                "It breaks things ordinary TNT cannot, obsidian included, but a plain anvil always loses"
                        + " a durability step doing it.",
                "Time the magnet to catch the anvil, or it lands on the stonecutter and pops into an" + " item.",
                "The Block Devourer and the Giant Anvil are the same idea over an area.");
        page(
                reg,
                "anvil_repair",
                List.of("minecraft:damaged_anvil", "minecraft:chipped_anvil"),
                "Hold a Block of Iron, sneak, and right-click a damaged anvil to repair it.",
                "Early on, deliberately crafting damaged anvils and repairing them later is the cheaper"
                        + " way to keep anvils in supply.");
        page(
                reg,
                "redstone_emp",
                List.of("minecraft:redstone_block"),
                "An anvil landing on a Block of Redstone snuffs out every redstone torch on its level for"
                        + " a single tick.",
                "Reach is six times the anvil's fall height, up to twenty-four blocks.",
                "An Iron Trapdoor flush against the redstone block shields that direction.");
        page(
                reg,
                "spawner",
                List.of("minecraft:spawner"),
                "An anvil landing on a spawner forces it to try spawning immediately.",
                "The chance is one minus one over the fall height, so higher drops succeed more often.",
                "No player needs to be nearby — the chunk only has to be loaded. The spawner's own"
                        + " conditions still apply.");
        page(
                reg,
                "villager",
                List.of("minecraft:villager_spawn_egg", "minecraft:wandering_trader_spawn_egg"),
                "An anvil that hits a villager without killing it resets that villager's trades, with a"
                        + " 20% chance of turning it into a nitwit.",
                "The same blow turns a Wandering Trader into an ordinary villager, with a 15% chance of a"
                        + " nitwit.");
        page(
                reg,
                "dispenser",
                List.of("minecraft:dispenser"),
                "Dispensers here can work a cauldron with bottles and buckets, filling and emptying it"
                        + " without a player.",
                "They also repair iron golems with iron ingots, milk cows, take stew from mooshrooms and"
                        + " cure zombie villagers with a golden apple — the pieces of an automatic iron"
                        + " farm.");
        page(
                reg,
                "enchantment",
                List.of("minecraft:enchanted_book"),
                "Disintegration: broken blocks drop nothing but a point of experience, and anything that"
                        + " would have dropped experience gives four times as much.",
                "Smelting: whatever the block drops comes out already smelted.");
    }

    private static void heat(EmiRegistry reg) {
        page(
                reg,
                "heated_block",
                List.of("minecraft:netherite_block", "anvilcraft:tungsten_block"),
                "Blocks of Netherite and Tungsten are heatable: they hold a temperature level and a"
                        + " duration, and cool down when nothing keeps heating them.",
                "There are five levels — normal, heated, red-hot, glowing and incandescent — and the"
                        + " block's appearance changes with each.",
                "Heat them with a Heater, a laser or heliostats. A Heat Collector turns the stored heat"
                        + " back into power.");
        page(
                reg,
                "overheated_block",
                List.of("anvilcraft:overheated_ember_metal_block"),
                "A Block of Ember Metal is heatable too, but with only two states: normal and" + " overheated.",
                "Ordinary heating will not get it there — it needs the methods from the thermal chapter,"
                        + " and Anvil Collision crafting before that.",
                "The overheated block is what a Charged Neutronium Ingot is pressed into to make" + " Transcendium.");
        page(
                reg,
                "void_decay",
                List.of("anvilcraft:void_matter_block"),
                "A Block of Void Matter with at least five of its faces against other Void Matter rots on"
                        + " a random tick.",
                "What it becomes is drawn at random from the ordinary stone, sandstone, dirt and ore"
                        + " blocks of all three dimensions — which is what makes it a generator.");
    }

    private static void electricity(EmiRegistry reg) {
        page(
                reg,
                "charge_collector",
                List.of("anvilcraft:charge_collector", "anvilcraft:piezoelectric_crystal"),
                "The Charge Collector is the heart of any power setup: it watches a 5x5x5 around itself"
                        + " and turns the charges produced there into power.",
                "Charges counted in one cycle become the next cycle's output in kilowatts, and a cycle is"
                        + " two seconds by default. Ceiling is 128 kW.",
                "Charges come from anvils hitting things — a Piezoelectric Crystal is the usual source.");
        page(
                reg,
                "transmission_pole",
                List.of("anvilcraft:transmission_pole", "anvilcraft:remote_transmission_pole"),
                "Everything generating or consuming inside a pole's range joins the same grid, and two"
                        + " poles join their grids wherever their ranges overlap.",
                "A Transmission Pole reaches 8 blocks; the Remote version reaches 16.");
        page(
                reg,
                "power_converter",
                List.of(
                        "anvilcraft:power_converter_small",
                        "anvilcraft:power_converter_middle",
                        "anvilcraft:power_converter_big",
                        "anvilcraft:fe_collector"),
                "AnvilCraft's power is not FE, so it needs converting before anything else in the pack"
                        + " can drink it.",
                "Conversion runs one way only, into FE, and loses some along the way. The three sizes"
                        + " differ in throughput.");
        page(
                reg,
                "heat_collector",
                List.of("anvilcraft:heat_collector"),
                "Turns the heat stored in nearby blocks back into power, watching a 5x5x5 around itself.",
                "Output depends entirely on what is in range, and it tops out at 4096 kW — far more than"
                        + " a charge collector.",
                "Blocks of Uranium give it 2 kW each; Plutonium gives 8 kW.");
        page(
                reg,
                "void_energy_collector",
                List.of("anvilcraft:void_energy_collector"),
                "Draws power from the absence of matter in a 5x5x5 around itself, so solid blocks nearby"
                        + " cut the output.",
                "Two of them must never share space — overlapping detection ranges stop both.",
                "Negative Matter cancels the matter around it and, in quantity, pushes the output far" + " higher.");
    }

    private static void structures(EmiRegistry reg) {
        page(
                reg,
                "mineral_fountain",
                List.of("anvilcraft:impact_pile", "anvilcraft:mineral_fountain"),
                "The mod's ores do not generate in the world; this is where the rarer ones come from.",
                "Craft an Impact Pile and place it on bedrock or deepslate, no more than eight blocks"
                        + " above the bottom of the world.",
                "Drop an undamaged anvil onto it from at least twenty blocks up. Both are consumed and a"
                        + " fountain structure forms in their place.");
        page(
                reg,
                "overseer",
                List.of("anvilcraft:overseer"),
                "Keeps chunks loaded so your machines keep running while you are away.",
                "It needs a multiblock under it: up to three 3x3 layers of Royal Steel or Frost Metal"
                        + " blocks. More layers reach further; with none it holds only its own chunk.");
        page(
                reg,
                "mineral_ores",
                List.of(
                        "anvilcraft:deepslate_zinc_ore",
                        "anvilcraft:deepslate_tin_ore",
                        "anvilcraft:deepslate_lead_ore",
                        "anvilcraft:deepslate_silver_ore",
                        "anvilcraft:deepslate_tungsten_ore",
                        "anvilcraft:deepslate_titanium_ore"),
                "These do not spawn during worldgen. Early on the only way to the metals is sifting dusts"
                        + " on scaffolding for nuggets.",
                "The ore blocks themselves come later, from a Mineral Fountain.");
    }
}
