package io.github.bertie_mc.emi.integration.anvilcraft;

import static io.github.bertie_mc.emi.integration.anvilcraft.AnvilCraftGuideEmiModule.page;

import dev.emi.emi.api.EmiRegistry;
import java.util.List;

/**
 * Guide pages for AnvilCraft's tools and held items — the things whose recipe tells you nothing
 * about what they are for, which is most of them. A Crab Claw's recipe does not mention reach; a
 * Disk's does not mention that it copies machine settings.
 *
 * <p>Written from the 1.6 guide book, checked against 1.5.3. The Guide Book itself is skipped: 1.5.3
 * ships the item but none of the book behind it, so a page telling you to open it would be a lie.
 */
final class AnvilCraftToolGuide {
    private AnvilCraftToolGuide() {}

    static void register(EmiRegistry reg) {
        tools(reg);
        weapons(reg);
        held(reg);
        consumables(reg);
    }

    private static void tools(EmiRegistry reg) {
        page(
                reg,
                "anvil_hammer",
                List.of(
                        "anvilcraft:anvil_hammer",
                        "anvilcraft:royal_anvil_hammer",
                        "anvilcraft:ember_anvil_hammer",
                        "anvilcraft:transcendence_anvil_hammer"),
                "This mod's wrench, and the tool you will use most.",
                "Right-click rotates a block; sneak-right-click takes it apart. It dismantles most of the"
                        + " mod's blocks in one go.",
                "Left-clicking a block does to it exactly what a falling anvil would — the quickest way"
                        + " to test a setup without building the drop first.",
                "Right-click a cauldron with it to cut an outlet, so processed items export themselves.");
        page(
                reg,
                "amethyst_tools",
                List.of(
                        "anvilcraft:amethyst_pickaxe",
                        "anvilcraft:amethyst_axe",
                        "anvilcraft:amethyst_shovel",
                        "anvilcraft:amethyst_hoe",
                        "anvilcraft:amethyst_sword"),
                "Cheap, durable, and enchanted out of the box — the recommended first tools.",
                "The pickaxe carries Fortune III, which is a lot of extra ore early, but it only mines at"
                        + " stone level.",
                "The axe carries Timber, one of the mod's own enchantments.",
                "Combining two identical amethyst tools on an anvil pushes their enchantments higher.");
        page(
                reg,
                "dragon_rod",
                List.of(
                        "anvilcraft:dragon_rod",
                        "anvilcraft:royal_dragon_rod",
                        "anvilcraft:ember_dragon_rod",
                        "anvilcraft:transcendence_dragon_rod"),
                "Collapses the whole place-devourer, hit-it, pick-it-up routine into a single tool.",
                "Every tier behaves the same; they differ only in durability and stats.");
        page(
                reg,
                "multitool",
                List.of("anvilcraft:multitool"),
                "Built around Multiphase Matter, and stands in for eight things at once: shears, flint"
                        + " and steel, brush, spyglass, hand magnet, fishing rod, carrot on a stick and"
                        + " warped fungus on a stick.");
        page(
                reg,
                "resonator",
                List.of("anvilcraft:resonator_core", "anvilcraft:ember_metal_resonator"),
                "Mines any block at all, and holds a radial menu of five modes.",
                "It never truly breaks. Like an elytra it goes inert instead — no damage, no bonuses, and"
                        + " most enchantments stop working until it is repaired.");
        page(
                reg,
                "ionocraft_backpack",
                List.of("anvilcraft:ionocraft_backpack"),
                "Worn in the chest slot, or a curio slot if you have one, and grants creative-style"
                        + " flight while it has power.",
                "On a full charge and no resupply it is good for about twenty minutes in the air.");
    }

    private static void weapons(EmiRegistry reg) {
        page(
                reg,
                "heavy_halberd",
                List.of("anvilcraft:heavy_halberd_core", "anvilcraft:ember_metal_heavy_halberd"),
                "Sword, axe, heavy hammer and trident folded into one weapon, built around a Heavy" + " Halberd Core.",
                "It acts as any of the four it was made from, and accepts the enchantments of all of" + " them.");
        page(
                reg,
                "spectral_slingshot",
                List.of("anvilcraft:spectral_slingshot"),
                "Fires your weapons as projectiles. Hold the weapon in your offhand and the slingshot in"
                        + " your main hand, then right-click to load it.",
                "From there it behaves like a crossbow: the tooltip shows what is loaded, and right-click" + " fires.");
        page(
                reg,
                "energy_weapon_platform",
                List.of("anvilcraft:energy_weapon_platform"),
                "Holds 320 MJ. Open it and feed it materials to convert it into one of several energy" + " weapons.",
                "If the first item in the recipe is enchanted, the finished weapon keeps those" + " enchantments.");
    }

    private static void held(EmiRegistry reg) {
        page(
                reg,
                "crab_claw",
                List.of("anvilcraft:crab_claw"),
                "Three blocks of extra reach, which is worth more than it sounds when you are placing"
                        + " anvils overhead.",
                "Right-click to force a shulker open.");
        page(
                reg,
                "hand_magnet",
                List.of("anvilcraft:magnet"),
                "Sneak-right-click a block face to leave a magnetised node there, which pulls items in"
                        + " without you holding anything.",
                "Takes Mending, Unbreaking and Curse of Vanishing.");
        page(
                reg,
                "geode",
                List.of("anvilcraft:geode"),
                "From mining budding amethyst — Fortune does nothing — or from a Jeweler, or the bonus"
                        + " chest if you enabled one.",
                "Stamping one has a chance at each of the mod's three gems, so keep some spare rather"
                        + " than spending them all on locating.");
        page(
                reg,
                "disk",
                List.of("anvilcraft:disk"),
                "Right-click a machine to copy its configuration, then right-click another of the same"
                        + " kind to paste it. Sneak-right-click clears it.",
                "Facing is not part of the copy.");
        page(
                reg,
                "resin_block",
                List.of("anvilcraft:resin_block"),
                "The creature has to be under 1.5 blocks wide and 2 tall, and anything hostile or neutral"
                        + " must be under Weakness first.",
                "Right-click the ground to let it out again. This is how you move villagers, and how you"
                        + " hold a zombie still while it becomes a Giant Zombie.");
        page(
                reg,
                "ionocraft",
                List.of("anvilcraft:ionocraft"),
                "Right-click the ground to place it as an entity you can stand on.",
                "Inside a power grid it draws 16 kW and climbs quickly. Outside one it sinks slowly.");
    }

    private static void consumables(EmiRegistry reg) {
        page(
                reg,
                "amulet",
                List.of("anvilcraft:amulet_box"),
                "The first amulet is won, not crafted. Carry an Amulet Box with a Totem of Undying inside"
                        + " it and take the specific fatal damage that totem blocks.",
                "That gives a 20% chance at the matching amulet, and the odds climb each time it fails.",
                "The box itself comes from a master-level Jeweler, and nowhere else.");
        page(
                reg,
                "totem_of_recovery",
                List.of("anvilcraft:totem_of_recovery", "anvilcraft:recovery_pearl"),
                "Works like a Totem of Undying and, unlike one, also saves you from the void.",
                "It leaves you holding a Recovery Pearl, which teleports you back to where you died.",
                "Use the pearl within twelve blocks of that death point and it sends you to your spawn"
                        + " instead. Either way it costs four points of fall damage.");
        page(
                reg,
                "totem_of_rage",
                List.of("anvilcraft:totem_of_rage"),
                "Triggers on fatal damage like a Totem of Undying, but strips every effect from you"
                        + " first, then refills health and hunger.",
                "You get a minute of Strength 5, Speed 3 and Haste 3 alongside its own Rage effect.");
        page(
                reg,
                "can",
                List.of("anvilcraft:tin_can", "anvilcraft:canned_food"),
                "Craft a Tin Can with any food to seal it in. Eating it does not give the can back.",
                "Up to five of the same food go into one can, and packing more in each time is worth"
                        + " more than eating them loose.");
        page(
                reg,
                "pill",
                List.of("anvilcraft:pill", "anvilcraft:pill_box"),
                "Craft a blank pill together with a potion and it carries that potion's effect.",
                "Pills stack, unlike bottles, and right-click swallows one instantly.",
                "The Pill Box holds them: left-click to pick one up, left-click the box to put it away.");
        page(
                reg,
                "chocolate",
                List.of("anvilcraft:chocolate", "anvilcraft:chocolate_black", "anvilcraft:chocolate_white"),
                "All three can be eaten on a full stomach.",
                "Plain gives thirty seconds of very fast Speed. Dark trades some of that for Haste, white"
                        + " for Jump Boost.");
        page(
                reg,
                "utusan",
                List.of("anvilcraft:utusan"),
                "Clears every negative effect on you at once.",
                "Take it while perfectly healthy and it gives you thirty seconds of Poison 5 instead.");
    }
}
