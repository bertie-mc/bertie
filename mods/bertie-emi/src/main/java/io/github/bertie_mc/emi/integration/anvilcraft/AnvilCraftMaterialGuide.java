package io.github.bertie_mc.emi.integration.anvilcraft;

import static io.github.bertie_mc.emi.integration.anvilcraft.AnvilCraftGuideEmiModule.page;

import dev.emi.emi.api.EmiRegistry;
import java.util.List;

/**
 * Guide pages for AnvilCraft's materials — where each one comes from and what it unlocks. The recipe
 * tabs already say how a material is made; these say why you would want it, and which of them gate
 * the tiers above.
 *
 * <p>Written from the 1.6 guide book, checked against 1.5.3. The Experience Gem is a 1.6 addition and
 * has no page. Frost Metal keeps its page because the material and its template do exist in 1.5.3,
 * even though the Frost anvil, grindstone and smithing table do not.
 */
final class AnvilCraftMaterialGuide {
    private AnvilCraftMaterialGuide() {}

    static void register(EmiRegistry reg) {
        early(reg);
        metals(reg);
        tiers(reg);
        exotic(reg);
    }

    private static void early(EmiRegistry reg) {
        page(
                reg,
                "gems",
                List.of("anvilcraft:ruby", "anvilcraft:sapphire", "anvilcraft:topaz"),
                "Three gems on top of the Emerald: Ruby for fire, Sapphire for water, Topaz for"
                        + " lightning. The mod treats all four together as gems.",
                "The first ones come from stamping Geodes, which has a chance at each.",
                "Once you have a Corrupted Beacon you can mass-produce them.");
        page(
                reg,
                "magnet",
                List.of("anvilcraft:magnet_ingot", "anvilcraft:ferrite_core_magnet_block"),
                "The first magnet has to be struck by lightning: a Block of Iron hit by a bolt becomes a"
                        + " Hollow Magnet Block.",
                "Rather than wait for a storm, right-click a lightning rod with a Topaz to spend it and"
                        + " call the bolt down immediately.",
                "After that, magnetisation makes more without lightning — right-click a Hollow Magnet"
                        + " Block with an Iron Ingot.");
        page(
                reg,
                "resin",
                List.of("anvilcraft:resin", "anvilcraft:hardend_resin", "anvilcraft:circuit_board"),
                "Crush logs for Resin. It hardens into Hardened Resin, which becomes Circuit Boards.",
                "Circuit boards and processors are the common ingredient in most of the mod's machines,"
                        + " so this is worth automating early.",
                "Resin also makes Resin Blocks, which trap creatures — handy for a trading hall, and for"
                        + " holding a zombie still.");
        page(
                reg,
                "wood_fiber",
                List.of("anvilcraft:wood_fiber", "anvilcraft:plywood"),
                "Makes paper and charcoal, and burns as fuel.",
                "Plywood stands in for planks in most recipes and can be run through a stonecutter.");
        page(
                reg,
                "lime_powder",
                List.of("anvilcraft:lime_powder"),
                "Super Heat calcite or dripstone to get it — four powder from a Block of Calcite or"
                        + " Dripstone, one from a Pointed Dripstone.");
        page(
                reg,
                "cement",
                List.of("anvilcraft:gray_cement_bucket"),
                "Comes out grey. Throw a dye into the cauldron and crush it to change the colour.",
                "Cement is what Concrete is made from here.");
        page(
                reg,
                "heavy_iron_block",
                List.of("anvilcraft:heavy_iron_block", "anvilcraft:heavy_iron_door", "anvilcraft:heavy_iron_trapdoor"),
                "Enormous blast resistance, which is what you want around anything that explodes.",
                "Shift-right-click with an Anvil Hammer to take it apart quickly.",
                "The door and trapdoor cannot be operated by hand at all — right-click them with an Anvil"
                        + " Hammer instead.");
        page(
                reg,
                "levitation_powder",
                List.of("anvilcraft:levitation_powder", "anvilcraft:levitation_powder_block"),
                "Falls upward instead of down, in both item and block form.",
                "It is what Controllable Sand is made from.");
        page(
                reg,
                "amber",
                List.of("anvilcraft:amber", "anvilcraft:amber_block"),
                "Sells well to a Jeweler, and it is a component in crafting spawners.");
    }

    private static void metals(EmiRegistry reg) {
        page(
                reg,
                "common_nugget",
                List.of(
                        "anvilcraft:tungsten_ingot",
                        "anvilcraft:titanium_ingot",
                        "anvilcraft:zinc_ingot",
                        "anvilcraft:tin_ingot",
                        "anvilcraft:lead_ingot",
                        "anvilcraft:silver_ingot"),
                "Tungsten, titanium, zinc, tin, lead and silver are not mined here — they are sifted.",
                "Run dusts through Mesh Sifting on scaffolding and the nuggets come out of it.");
        page(
                reg,
                "capacitor",
                List.of("anvilcraft:capacitor", "anvilcraft:supercapacitor"),
                "Stores and releases power, filled and drained at a Charger.",
                "The Supercapacitor wants a Resin Block with a creeper inside it. If you use a charged"
                        + " one, it is an even bet between a full capacitor and a very large explosion.");
        page(
                reg,
                "chromatic_stone",
                List.of("anvilcraft:melt_gem_bucket", "anvilcraft:chromatic_stone"),
                "Super Heat any gem block in a cauldron and it melts into molten gem.",
                "Where the source touches water it sets into Chromatic Stone; where the flow touches"
                        + " water you get granite, diorite or andesite instead.");
        page(
                reg,
                "cursed_gold",
                List.of("anvilcraft:cursed_gold_ingot", "anvilcraft:cursed_gold_block"),
                "Made at a Royal Grindstone: strip a curse or a work penalty from an item using gold, and"
                        + " the gold comes out cursed.",
                "It is what the Corrupted Beacon is built and activated with, and you need a great deal" + " of it.",
                "Piglins that pick it up are zombified and drop extra cursed gold when killed, which is"
                        + " the basis of automating production.");
    }

    private static void tiers(EmiRegistry reg) {
        page(
                reg,
                "royal_steel",
                List.of("anvilcraft:royal_steel_ingot", "anvilcraft:royal_steel_block"),
                "Amethyst boosts the yield — that is what the two bonus Super Heating recipes are.",
                "Royal Preference: each world picks one gem other than Emerald at random from its seed,"
                        + " and using that gem doubles the yield. Nothing displays which one it is; you"
                        + " find it by trying them.",
                "Spend the first of it on the Royal Smithing Table.");
        page(
                reg,
                "royal_template",
                List.of("anvilcraft:royal_steel_upgrade_smithing_template"),
                "Found in village weaponsmith chests, or bought from a journeyman Jeweler.",
                "Upgrades items and blocks to the Royal Steel tier.");
        page(
                reg,
                "ember_metal",
                List.of("anvilcraft:ember_metal_ingot", "anvilcraft:ember_metal_block"),
                "Netherite-grade durability and mining level, and it carries the Reforging property.",
                "Ember tools take no damage from fire or lava.",
                "Built from Earth Core Shards.");
        page(
                reg,
                "ember_template",
                List.of("anvilcraft:ember_metal_upgrade_smithing_template"),
                "Upgrades items and blocks to the Ember tier.");
        page(
                reg,
                "frost_metal",
                List.of("anvilcraft:frost_metal_ingot", "anvilcraft:frost_metal_block"),
                "Netherite-grade durability and mining level, carrying the Ruthless property.",
                "Note that the Frost anvil, grindstone and smithing table are a later addition than this"
                        + " pack's AnvilCraft, so the metal is here but its own tier blocks are not.");
        page(
                reg,
                "frost_template",
                List.of("anvilcraft:frost_metal_upgrade_smithing_template"),
                "Upgrades items and blocks to the Frost tier.");
        page(
                reg,
                "transcendium",
                List.of("anvilcraft:transcendium_ingot", "anvilcraft:transcendium_block"),
                "Press a Charged Neutronium Ingot into an Overheated Ember Metal Block with an anvil.",
                "How many enchantments are on that ingot decides what you get: none gives four ingots,"
                        + " one to fourteen adds nuggets and a chance of the neutronium back, and fifteen"
                        + " or more leaves a Block of Transcendium behind in place of the ember block.");
        page(
                reg,
                "transcendium_template",
                List.of("anvilcraft:transcendium_upgrade_smithing_template"),
                "Comes from dissociating an eight-in-one smithing template.",
                "Upgrades items and blocks to the Transcendium tier.");
    }

    private static void exotic(EmiRegistry reg) {
        page(
                reg,
                "earth_core_shard",
                List.of("anvilcraft:earth_core_shard", "anvilcraft:earth_core_shard_block"),
                "Produced by a Mineral Fountain.",
                "It makes Ember Metal, duplicates armour trim templates, and smelts metal blocks at" + " triple yield.",
                "Fireproof, and as a block it needs the same tools as obsidian.");
        page(
                reg,
                "void_matter",
                List.of("anvilcraft:void_matter", "anvilcraft:void_matter_block", "anvilcraft:void_stone"),
                "Produced by a Mineral Fountain. It rises in the void rather than falling.",
                "It builds the Void Energy Collector, and the block form is what Void Decay works on.");
        page(
                reg,
                "neutronium",
                List.of(
                        "anvilcraft:neutronium_ingot",
                        "anvilcraft:stable_neutronium_ingot",
                        "anvilcraft:charged_neutronium_ingot"),
                "Made by banking mass in a Space Overcompressor: put metal on top, strike with an anvil,"
                        + " and the ingot drops out once enough has accumulated.",
                "The charged form is the input to Transcendium.");
        page(
                reg,
                "uranium",
                List.of("anvilcraft:uranium_ingot", "anvilcraft:uranium_block", "anvilcraft:raw_uranium"),
                "First obtained by Anvil Collision crafting, then mass-produced at a Mineral Fountain.",
                "Each Block of Uranium feeds a Heat Collector 2 kW.",
                "Time Warp a Block of Uranium and it dumps millennia of decay at once, holding the blocks"
                        + " beside it incandescent for five minutes.",
                "Never put one in a Neutron Irradiator.");
        page(
                reg,
                "plutonium",
                List.of("anvilcraft:plutonium_ingot", "anvilcraft:plutonium_block"),
                "Comes out of the Neutron Irradiator. It is too reactive to exist as ore, so a Mineral"
                        + " Fountain cannot make it.",
                "Each Block of Plutonium feeds a Heat Collector 8 kW, four times what uranium gives.",
                "Time Warping it releases the same sudden flood of heat that uranium does.");
        page(
                reg,
                "negative_matter",
                List.of("anvilcraft:negative_matter", "anvilcraft:negative_matter_block"),
                "Comes from Anvil Collision crafting.",
                "A Block of Negative Matter is thick with void energy and drives a Void Energy Collector"
                        + " far harder — each one cancels the matter around the collector, and a surplus"
                        + " pushes the output up sharply.");
        page(
                reg,
                "multiphase_matter",
                List.of("anvilcraft:multiphase_matter", "anvilcraft:multiphase_matter_block"),
                "Comes from Anvil Collision crafting — the extremely cold and the extremely hot pressed" + " together.",
                "It builds the Ember Metal Resonator, the Ember Metal Heavy Halberd and the Multitool.");
        page(
                reg,
                "oil",
                List.of("anvilcraft:oil_bucket"),
                "Crude oil never generates naturally here; it is rendered from meat.",
                "One layer, 250 mB, comes from a single zombie or piglin head, sixteen pieces of beef,"
                        + " pork, mutton or rabbit, or sixty-four fish, chicken, rotten flesh or spider"
                        + " eyes.");
    }
}
