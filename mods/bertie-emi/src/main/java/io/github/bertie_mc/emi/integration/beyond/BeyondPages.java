package io.github.bertie_mc.emi.integration.beyond;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** In-world arrangements; positions are in recipe pixels and block properties describe placed states. */
final class BeyondPages {
    static final int WIDTH = 176;
    static final int HEIGHT = 126;

    enum Kind {
        ITEM,
        BLOCK,
        ENTITY,
        TAG,
        ALTERNATIVES
    }

    enum Role {
        INPUT,
        CATALYST,
        OUTPUT,
        DISPLAY
    }

    enum Group {
        METAL("Metalworking", "brittle_metal"),
        BONFIRE("Bonfires", "bonfire"),
        GROWTH("Cultivation", "obiroot"),
        CREATURES("Living Creatures", "ocarina"),
        DEVICES("Devices", "prismograph");

        final String title;
        final String icon;

        Group(String title, String icon) {
            this.title = title;
            this.icon = icon;
        }
    }

    record Node(
            Kind kind,
            String id,
            int amount,
            float chance,
            Role role,
            int x,
            int y,
            String label,
            String hint,
            Map<String, String> properties) {}

    record Link(int x1, int y1, int x2, int y2) {}

    record Caption(String text, int x, int y, String hint) {}

    record Page(String id, Group group, String title, List<Node> nodes, List<Link> links, List<Caption> captions) {}

    record Cast(String tool, String pattern) {}

    static final List<Cast> CASTS = List.of(
            new Cast("sword", "010010010"),
            new Cast("shovel", "010000000"),
            new Cast("pickaxe", "111010010"),
            new Cast("axe", "011011010"),
            new Cast("hoe", "011010010"));

    private BeyondPages() {}

    static List<Page> all() {
        List<Page> pages = new ArrayList<>();
        for (Cast cast : CASTS) pages.add(casting(cast));

        pages.add(page("quenching", Group.METAL, "Quench molten metal")
                .block("molten_metal", Role.INPUT, 36, 47, "Molten", "The molten block is removed.")
                .choice(
                        "minecraft:ice|minecraft:packed_ice|minecraft:blue_ice|minecraft:water_bucket",
                        Role.CATALYST,
                        16,
                        47,
                        "Ice / water",
                        "Place ice or water against any face of molten metal. The bucket itself is not consumed.")
                .arrow(60, 56, 108, 56)
                .item("prismuth", Role.OUTPUT, 118, 35, "1-2", "Always drops 1-2 Prismuth.")
                .output(
                        "brittle_metal_sheet",
                        0.5f,
                        118,
                        75,
                        "50%: 1-3",
                        "50% chance to also drop 1-3 Brittle Metal Sheets.")
                .build());
        pages.add(page("metal_cycle", Group.METAL, "Shape a regenerating metal bed")
                .block(
                        "brittle_metal",
                        Role.CATALYST,
                        12,
                        48,
                        "Solid",
                        "Punch an intact block, or stand on it, to crack it.")
                .arrow(34, 57, 70, 57)
                .block(
                        "brittle_metal",
                        Role.DISPLAY,
                        74,
                        48,
                        "Cracked",
                        "Cracked metal counts as an empty cell in the casting pattern.",
                        "powered",
                        "true")
                .arrow(96, 57, 136, 57)
                .block(
                        "molten_metal",
                        Role.DISPLAY,
                        140,
                        48,
                        "Molten",
                        "Standing on cracked metal can melt it. Molten metal also counts as an empty cell.")
                .caption(
                        "Random ticks restore the bed",
                        12,
                        100,
                        "Molten metal reforms into cracked metal; cracked metal reforms into solid metal. Cast again after reshaping the bed.")
                .build());

        pages.add(page("light_bonfire", Group.BONFIRE, "Light a bonfire")
                .choice(
                        "the_beyond:live_flame|the_beyond:livid_flame",
                        35,
                        25,
                        "Either flame",
                        "Use Live Flame or Livid Flame on an unlit Bonfire. One flame is consumed.")
                .arrow(44, 47, 44, 61)
                .block("bonfire", Role.CATALYST, 35, 68, "Unlit", "Right-click with either flame.", "lit", "false")
                .arrow(61, 77, 115, 77)
                .block(
                        "bonfire",
                        Role.DISPLAY,
                        122,
                        68,
                        "Lit",
                        "Lighting a Bonfire activates its ritual.",
                        "lit",
                        "true")
                .caption(
                        "Carry in the selected hotbar slot",
                        4,
                        109,
                        "Flames lose durability over time. Unselected flames burn out four times faster, sprinting doubles the loss, and throwing a flame destroys it.")
                .build());
        pages.add(page("bonfire_lanterns", Group.BONFIRE, "Kindle a Lantern gathering")
                .choice(
                        "the_beyond:live_flame|the_beyond:livid_flame",
                        28,
                        24,
                        "Light fire",
                        "Lighting an unlit Bonfire starts its activation ritual for the activating player.")
                .arrow(37, 46, 37, 62)
                .block(
                        "bonfire",
                        Role.CATALYST,
                        28,
                        69,
                        "Bonfire",
                        "Stay nearby while the Bonfire calls and splits Lanterns.",
                        "lit",
                        "true")
                .arrow(54, 78, 115, 78)
                .entity(
                        "lantern",
                        128,
                        69,
                        "Lanterns",
                        "The ritual attempts to spawn Lanterns while fewer than ten are within 20 blocks. Lanterns can split near the end of the ritual or during thunder.")
                .build());
        for (boolean rain : List.of(false, true)) {
            pages.add(page(
                            rain ? "livid_flame" : "live_flame",
                            Group.BONFIRE,
                            rain ? "Collect flame during rain" : "Collect flame in clear weather")
                    .item(
                            "ectoplasm",
                            Role.INPUT,
                            34,
                            24,
                            "Use on fire",
                            "Right-click a lit Bonfire with Ectoplasm; one Ectoplasm is consumed.")
                    .arrow(43, 46, 43, 59)
                    .block("bonfire", Role.CATALYST, 34, 66, "Lit", "The Bonfire remains lit.", "lit", "true")
                    .arrow(59, 75, 115, 75)
                    .item(
                            rain ? "livid_flame" : "live_flame",
                            Role.OUTPUT,
                            126,
                            66,
                            "",
                            "The current rain state determines which flame is produced.")
                    .build());
        }
        pages.add(page("bonfire_search", Group.BONFIRE, "Find the next bonfire")
                .block(
                        "bonfire",
                        Role.CATALYST,
                        20,
                        49,
                        "Empty hand",
                        "Right-click a Bonfire with an empty hand.",
                        "lit",
                        "true")
                .arrow(47, 58, 124, 58)
                .block(
                        "bonfire",
                        Role.DISPLAY,
                        135,
                        49,
                        "Opposite",
                        "Finds the nearest Bonfire with the opposite lit state within 200 blocks. Otherwise it searches for a Bonfire structure.",
                        "lit",
                        "false")
                .caption(
                        "Follow the particle trail",
                        18,
                        99,
                        "A trail points to a nearby opposite-state Bonfire. Structure-search particles only indicate that one is nearby.")
                .build());

        pages.add(page("obiroot_sprout", Group.GROWTH, "Grow hanging Obiroot")
                .block(
                        "obiroot",
                        Role.CATALYST,
                        43,
                        25,
                        "Obiroot",
                        "Rain can grow an Obiroot Sprout into the air directly beneath this block.")
                .arrow(52, 47, 52, 61)
                .block(
                        "obiroot_sprout",
                        Role.OUTPUT,
                        43,
                        68,
                        "Air below",
                        "Leave the space below Obiroot empty. Growth is random, with a higher chance during thunder.")
                .caption(
                        "Rain / thunder",
                        104,
                        49,
                        "Per random tick: 2% during rain, 5% during thunder, and no growth in clear weather.")
                .build());
        for (String base : List.of("nacre", "rich_nacre", "pale_nacre")) {
            String raked = base.equals("nacre") ? "raked_nacre" : base.replace("_nacre", "_raked_nacre");
            pages.add(page("rake_" + base, Group.GROWTH, "Rake Nacre")
                    .tag(
                            "minecraft:hoes",
                            Role.CATALYST,
                            32,
                            24,
                            "Right-click",
                            "Use any hoe. Its durability decreases by one.")
                    .arrow(41, 46, 41, 61)
                    .block(base, Role.INPUT, 32, 68, "", "Aim at a different edge to change the raked pattern.")
                    .arrow(57, 77, 116, 77)
                    .block(
                            raked,
                            Role.OUTPUT,
                            128,
                            68,
                            "",
                            "The hit position and facing direction determine the raked design.")
                    .build());
        }
        pages.add(page("bedazzled_end_stone", Group.GROWTH, "Collect loose Pearl Beads")
                .item(
                        "minecraft:iron_pickaxe",
                        Role.CATALYST,
                        25,
                        24,
                        "No Silk Touch",
                        "Breaking without Silk Touch releases 1-4 Pearl Bead projectiles. Pick them up by interacting with them.")
                .arrow(34, 46, 34, 61)
                .block(
                        "bedazzled_end_stone",
                        Role.INPUT,
                        25,
                        68,
                        "Break",
                        "Silk Touch suppresses the extra loose pearls.")
                .arrow(51, 77, 118, 77)
                .item("pearl_bead", Role.OUTPUT, 128, 68, "1-4 loose", "Right-click a loose Pearl Bead to collect it.")
                .build());

        pages.add(page("nomad_blessing", Group.CREATURES, "Give a Nomad a Remembrance")
                .tag(
                        "the_beyond:remembrances",
                        Role.INPUT,
                        22,
                        27,
                        "Give one",
                        "Use a Remembrance on an Abyssal Nomad that is not sitting.")
                .arrow(31, 50, 31, 62)
                .entity("abyssal_nomad", 22, 69, "Nomad", "The offered Remembrance is consumed.")
                .arrow(48, 78, 111, 78)
                .caption("5:00 blessing", 96, 65, "Grants Nomad's Blessing for 6,000 ticks (five minutes).")
                .build());
        pages.add(page("memor_faucet", Group.CREATURES, "Offer Remembrances beneath a faucet")
                .block(
                        "memor_faucet",
                        Role.CATALYST,
                        42,
                        27,
                        "Faucet",
                        "A nearby Nomad, or a player holding a Remembrance within five blocks, opens the faucet.")
                .arrow(51, 49, 51, 65)
                .tag(
                        "the_beyond:remembrances",
                        Role.INPUT,
                        42,
                        72,
                        "Drop below",
                        "Drop Remembrances below the faucet. It consumes them one at a time and advances through five charge stages.")
                .arrow(68, 81, 116, 81)
                .entity(
                        "abyssal_nomad",
                        128,
                        72,
                        "Summon",
                        "The first offering attempts to summon Nomads near Auroracite; later offerings empower nearby Nomads.")
                .caption(
                        "Five offerings: fully charged",
                        4,
                        110,
                        "Nearby Nomads can also supply Cloth Remembrances to an uncharged faucet. The faucet is not an inventory crafting recipe.")
                .build());
        pages.add(page("enadrake_growth", Group.CREATURES, "Grow an Enadrake into Obiroot")
                .item(
                        "minecraft:bone_meal",
                        Role.INPUT,
                        25,
                        25,
                        "Feed",
                        "Use Bone Meal on an Enadrake. Each attempt consumes one Bone Meal.")
                .arrow(34, 47, 34, 62)
                .entity(
                        "enadrake",
                        25,
                        69,
                        "Enadrake",
                        "Each feeding has a one-in-three chance to attempt Obiroot growth; the feature also needs room to generate.")
                .arrow(51, 78, 117, 78)
                .block(
                        "obiroot",
                        Role.OUTPUT,
                        130,
                        69,
                        "Obiroot",
                        "Successful growth replaces the Enadrake with an Obiroot feature.")
                .build());
        pages.add(page("trinket_care", Group.CREATURES, "Wax and colour a Trinket")
                .item(
                        "minecraft:honeycomb",
                        Role.INPUT,
                        12,
                        29,
                        "Wax",
                        "Right-click with Honeycomb to wax a Trinket; one Honeycomb is consumed.")
                .tag(
                        "minecraft:axes",
                        Role.CATALYST,
                        78,
                        29,
                        "Unwax",
                        "Right-click with an axe to remove wax; one durability is used.")
                .item(
                        "minecraft:red_dye",
                        Role.INPUT,
                        142,
                        29,
                        "Dye",
                        "Use a dye to change the Trinket's colour. One dye is consumed when the colour changes.")
                .arrow(21, 51, 80, 78)
                .arrow(87, 51, 87, 70)
                .arrow(151, 51, 99, 78)
                .entity(
                        "trinket",
                        78,
                        82,
                        "Trinket",
                        "These are independent interactions; the three items are not a combined recipe.")
                .build());
        pages.add(page("ocarina", Group.CREATURES, "Command living Trinkets")
                .item(
                        "ocarina",
                        Role.CATALYST,
                        20,
                        49,
                        "Tap / hold",
                        "Tap use to perform the current command. Hold use to cycle modes every 12 ticks. Crouch and tap to deselect.")
                .arrow(46, 58, 119, 58)
                .entity(
                        "trinket",
                        130,
                        49,
                        "Within 8",
                        "Select mode links nearby unowned or owned Trinkets. The other modes guide them to a target, follow the player, or scatter them.")
                .caption(
                        "Select > Guide > Follow > Scatter",
                        1,
                        103,
                        "The selected mode is displayed by the Ocarina's overlay. The maximum number of linked Trinkets follows the mod configuration.")
                .build());
        pages.add(page("auroracite", Group.CREATURES, "Walk on Auroracite")
                .item(
                        "pathfinder_boots",
                        Role.CATALYST,
                        23,
                        24,
                        "Wear",
                        "Equip Pathfinder Boots, or obtain Nomad's Blessing, to stand on Auroracite.")
                .arrow(32, 46, 32, 62)
                .block(
                        "auroracite",
                        Role.CATALYST,
                        23,
                        69,
                        "Walk",
                        "Approach Auroracite from above while wearing the boots or carrying Nomad's Blessing.")
                .arrow(49, 78, 116, 78)
                .caption(
                        "Solid footing",
                        101,
                        64,
                        "Holding Ectoplasm or Auroracite allows targeting this block; walking on it separately requires the boots or blessing.")
                .build());

        pages.add(page("prismograph", Group.DEVICES, "Take a Snapshot")
                .tag(
                        "the_beyond:prismograph_film",
                        Role.INPUT,
                        33,
                        24,
                        "Load film",
                        "Right-click the Prismograph with film; the default film is Paper. Each photograph consumes one film.")
                .arrow(42, 46, 42, 61)
                .block(
                        "prismograph",
                        Role.CATALYST,
                        33,
                        68,
                        "Use / pulse",
                        "Use an empty hand or a rising redstone signal to capture. A player must be within 160 blocks to render the image.")
                .arrow(59, 77, 117, 77)
                .item("snapshot", Role.OUTPUT, 130, 68, "Snapshot", "The image is stored on the resulting Snapshot.")
                .build());
        pages.add(page("projector", Group.DEVICES, "Illuminate a Projector from behind")
                .block(
                        "minecraft:glowstone",
                        Role.CATALYST,
                        12,
                        62,
                        "Light",
                        "Put a light-emitting block immediately behind the Projector.")
                .block(
                        "projector",
                        Role.CATALYST,
                        37,
                        62,
                        "Projector",
                        "The Projector faces right in this side view. Open its menu to load images and choose the mode.")
                .arrow(62, 71, 137, 71)
                .item(
                        "snapshot",
                        Role.CATALYST,
                        37,
                        24,
                        "Load image",
                        "Snapshots and accepted photographs go in the Projector's image slots.")
                .caption(
                        "Image",
                        139,
                        67,
                        "The beam projects forward. A rising redstone pulse advances carousel mode; crouch-right-click with an empty hand also advances it.")
                .caption(
                        "Side view",
                        62,
                        103,
                        "The light source must be behind the Projector, opposite its facing direction.")
                .build());
        pages.add(page("bellow", Group.DEVICES, "Push entities with a Bellow")
                .item(
                        "minecraft:lever",
                        Role.CATALYST,
                        12,
                        28,
                        "Signal 1-15",
                        "Power the Bellow directly, or power the Copper Bulb supporting it.")
                .arrow(21, 50, 21, 64)
                .block(
                        "minecraft:copper_bulb",
                        Role.CATALYST,
                        12,
                        71,
                        "Support",
                        "Attach the Bellow to a support. A Copper Bulb can relay its redstone power to the Bellow.")
                .block(
                        "bellow",
                        Role.CATALYST,
                        37,
                        71,
                        "Bellow",
                        "Point its nozzle toward the entities to move. Keep the jet's path clear.",
                        "facing",
                        "east")
                .arrow(62, 80, 139, 80)
                .caption(
                        "0-5 blocks",
                        103,
                        45,
                        "Reach is signal strength divided by three, up to five blocks, shortened by obstructions.")
                .build());
        pages.add(page("polar_signal", Group.DEVICES, "Trigger a Polar Bulb")
                .block(
                        "polar_antenna",
                        Role.CATALYST,
                        24,
                        69,
                        "Touch",
                        "Touch a Polar Antenna to send magnetic pulses to nearby receivers. Propagation chooses random horizontal directions and can stop.")
                .arrow(50, 78, 88, 78)
                .block(
                        "polar_pillar",
                        Role.CATALYST,
                        96,
                        69,
                        "Pillar",
                        "An adjacent receiver can be at the same height or one block above or below. Pillars carry the charge upward.")
                .block(
                        "polar_bulb",
                        Role.CATALYST,
                        96,
                        49,
                        "",
                        "A charged bulb above the pillar releases an Enderglop, then slowly recharges through random ticks.")
                .arrow(121, 58, 144, 36)
                .entity(
                        "enderglop",
                        149,
                        23,
                        "Glop",
                        "Rain produces a larger Enderglop. Wait for the bulb to refill before triggering again.")
                .caption(
                        "Side view",
                        8,
                        108,
                        "Place a Polar Bulb on top of a Polar Pillar, with the Antenna beside the pillar.")
                .build());
        Builder vent = page("gauss_vent", Group.DEVICES, "Build a stronger Gauss Vent");
        for (int[] offset : new int[][] {{0, 0}, {1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
            vent.block(
                    "gauss_vent",
                    Role.CATALYST,
                    40 + offset[0] * 20,
                    49 + offset[1] * 20,
                    "",
                    "A centre vent with vents on all four horizontal sides forms the strong configuration. Leave the space above the centre clear.");
        }
        pages.add(vent.arrow(88, 58, 145, 58)
                .caption(
                        "13.3",
                        146,
                        54,
                        "The strong upward jet reaches 40/3 blocks and grants Weightless. An isolated vent reaches five blocks.")
                .caption(
                        "Top view: five vents",
                        13,
                        103,
                        "Jets activate randomly; this arrangement strengthens the centre vent.")
                .build());
        pages.add(page("mirror", Group.DEVICES, "Expose a Mirror face")
                .tag(
                        "minecraft:axes",
                        Role.CATALYST,
                        26,
                        25,
                        "Use on face",
                        "Use an axe on a Mirror face to toggle that face's reflective surface. One durability is used.")
                .arrow(35, 47, 35, 62)
                .block("mirror", Role.CATALYST, 26, 69, "Mirror", "Every face can be toggled independently.")
                .arrow(52, 78, 118, 78)
                .block(
                        "mirror",
                        Role.DISPLAY,
                        131,
                        69,
                        "Reflective",
                        "Expose the desired faces; creatures in the no-image tag do not appear in reflections.",
                        "east",
                        "true")
                .build());
        pages.add(page("magnet", Group.DEVICES, "Slingshot toward metal")
                .item(
                        "magnet",
                        Role.CATALYST,
                        20,
                        49,
                        "Use",
                        "Aim at a metal-tagged block within 32 blocks and use the Magnet to launch toward it.")
                .arrow(46, 58, 123, 58)
                .block(
                        "minecraft:iron_block",
                        Role.CATALYST,
                        136,
                        49,
                        "Metal",
                        "Valid targets follow the_beyond:metal_blocks, including the mod's metals and common metal storage blocks and ores.")
                .caption(
                        "Hotbar / offhand: attracts items",
                        1,
                        104,
                        "Keeping the Magnet in the hotbar or offhand attracts dropped items within its eight-block-wide detection area.")
                .build());
        pages.add(page("coiled_stalk", Group.DEVICES, "Extend a Coiled Stalk")
                .item(
                        "coiled_stalk",
                        Role.INPUT,
                        20,
                        25,
                        "Aim + release",
                        "Aim at a block face within 64 blocks, hold use, and release. Auroracite cannot be targeted.")
                .arrow(29, 47, 29, 62)
                .block(
                        "minecraft:end_stone",
                        Role.CATALYST,
                        20,
                        69,
                        "Anchor",
                        "The stalk grows outward from the targeted face until blocked or its length limit is reached.")
                .block(
                        "coil_vertebrae",
                        Role.DISPLAY,
                        40,
                        69,
                        "",
                        "Horizontal stalks form thin walkways; vertical stalks can be climbed.",
                        "axis",
                        "x")
                .block(
                        "coil_vertebrae",
                        Role.DISPLAY,
                        60,
                        69,
                        "",
                        "Breaking a segment causes connected segments on the same axis to break in succession.",
                        "axis",
                        "x")
                .arrow(86, 78, 151, 78)
                .caption(
                        "Up to 31 segments",
                        62,
                        104,
                        "Segments appear every three ticks. Growth stops at a non-replaceable obstacle.")
                .build());
        pages.add(page("refuge", Group.DEVICES, "Let Enadrakes complete a Refuge")
                .item(
                        "minecraft:cobblestone",
                        Role.INPUT,
                        22,
                        25,
                        "Drop items",
                        "Enadrakes pick up dropped items. A nearby calm Enadrake holding a non-Epic item can spend it to grow one step of the Refuge's arms.")
                .arrow(31, 47, 31, 62)
                .entity(
                        "enadrake",
                        22,
                        69,
                        "Enadrake",
                        "Keep supplying items near the incomplete Refuge. Its generated arm pattern fills incrementally.")
                .arrow(48, 78, 116, 78)
                .block(
                        "refuge",
                        Role.CATALYST,
                        129,
                        69,
                        "Refuge",
                        "When all arms are complete the Refuge activates. Use an empty hand to claim an unowned active Refuge and open its menu.")
                .build());
        return List.copyOf(pages);
    }

    static Page casting(Cast cast) {
        Builder b = page("casting_" + cast.tool(), Group.METAL, "Cast a Brittle " + cast.tool());
        for (int i = 0; i < 9; i++) {
            boolean solid = cast.pattern().charAt(i) == '1';
            b.block(
                    "brittle_metal",
                    Role.CATALYST,
                    12 + i % 3 * 20,
                    32 + i / 3 * 20,
                    "",
                    i == 4
                            ? "Right-click this centre block with an empty hand. Keep solid metal in the tool outline; punch the other cells to crack them."
                            : solid
                                    ? "Leave this cell solid. Casting melts the solid tool outline."
                                    : "Crack this cell by punching it. Molten metal also works here. Do not leave air in the 3x3 bed.",
                    "powered",
                    Boolean.toString(!solid));
        }
        return b.arrow(80, 61, 121, 61)
                .item(
                        "brittle_" + cast.tool(),
                        Role.OUTPUT,
                        137,
                        52,
                        "",
                        "Rotated outlines also work; axes and hoes can also be mirrored.")
                .caption(
                        "Top view",
                        12,
                        18,
                        "All nine positions must contain Brittle Metal or Molten Metal, at the same height.")
                .caption(
                        "Empty-hand use: centre",
                        12,
                        105,
                        "The centre is outlined in gold. The metal bed regenerates through random ticks and can be used again.")
                .build();
    }

    private static Builder page(String id, Group group, String title) {
        return new Builder(id, group, title);
    }

    private static String qualify(String id) {
        return id.contains(":") ? id : "the_beyond:" + id;
    }

    private static final class Builder {
        private final String id;
        private final Group group;
        private final String title;
        private final List<Node> nodes = new ArrayList<>();
        private final List<Link> links = new ArrayList<>();
        private final List<Caption> captions = new ArrayList<>();

        Builder(String id, Group group, String title) {
            this.id = id;
            this.group = group;
            this.title = title;
        }

        Builder item(String id, Role role, int x, int y, String label, String hint) {
            return node(Kind.ITEM, id, role, x, y, label, hint, Map.of());
        }

        Builder output(String id, float chance, int x, int y, String label, String hint) {
            nodes.add(new Node(Kind.ITEM, qualify(id), 1, chance, Role.OUTPUT, x, y, label, hint, Map.of()));
            return this;
        }

        Builder choice(String ids, int x, int y, String label, String hint) {
            return choice(ids, Role.INPUT, x, y, label, hint);
        }

        Builder choice(String ids, Role role, int x, int y, String label, String hint) {
            return node(Kind.ALTERNATIVES, ids, role, x, y, label, hint, Map.of());
        }

        Builder block(String id, Role role, int x, int y, String label, String hint, String... state) {
            return node(
                    Kind.BLOCK, id, role, x, y, label, hint, state.length == 0 ? Map.of() : Map.of(state[0], state[1]));
        }

        Builder entity(String id, int x, int y, String label, String hint) {
            return node(Kind.ENTITY, id, Role.CATALYST, x, y, label, hint, Map.of());
        }

        Builder tag(String id, Role role, int x, int y, String label, String hint) {
            return node(Kind.TAG, id, role, x, y, label, hint, Map.of());
        }

        private Builder node(
                Kind kind,
                String id,
                Role role,
                int x,
                int y,
                String label,
                String hint,
                Map<String, String> properties) {
            nodes.add(new Node(kind, qualify(id), 1, 1, role, x, y, label, hint, properties));
            return this;
        }

        Builder arrow(int x1, int y1, int x2, int y2) {
            links.add(new Link(x1, y1, x2, y2));
            return this;
        }

        Builder caption(String text, int x, int y, String hint) {
            captions.add(new Caption(text, x, y, hint));
            return this;
        }

        Page build() {
            return new Page(id, group, title, List.copyOf(nodes), List.copyOf(links), List.copyOf(captions));
        }
    }
}
