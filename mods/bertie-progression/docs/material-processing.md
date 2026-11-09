# Material processing

Material merges preserve distinct processing methods while removing duplicate item forms.
The generator restores reviewed machine routes with retained outputs, including mixed-output
recipes whose other results must survive. Magitech tool-material identifiers remain in their
own registry. Retained item tags are scoped to individual ingredient families, so replacing
sulfur with sulfur dust does not make that dust usable in every gem recipe. Bundled dependency
JARs participate in tag resolution.

Equivalent compatibility routes and authored progression replacements remain excluded.
Optional recipes with no available material are gated instead of displaying empty ingredients.
Generation fails on an unresolved active item ingredient. The regression cases run with:

```text
python -m unittest discover -s mods/bertie-progression/texture-work -p "test_recipe_*.py"
```

Zinc uses four nuggets per ingot in both directions. Oritech's dry, water and acid zinc
centrifuge recipes return 4, 8 and 12 nuggets. Antarchy titanium and uranium ores drop full
retained raw metals; obsolete scraps are merged away. Small copper, iron, gold, nickel and
platinum dust byproducts become their nuggets. Uranium and plutonium small dusts remain.
Magitech's fluorite block packs and unpacks mana-charged fluorite; its tool-material rule
accepts Mekanism fluorite. Mercury uranium ore has furnace and blast-furnace routes.

## AnvilCraft

Stamping converts brass, copper, iron and gold ingots into Create sheets, and Soulstained
Steel and Malignant Pewter ingots into their Malum plating. Pressure plates remain craftable
but are excluded from sheet/plate ingredient tags and stamping outputs. Equipment and horse
armour crushing are disabled. Chutes use iron sheets and return one item.

Anvil Hammer uses a basic or higher Pigment Pedestal for five seconds, consuming six each
Citrine, Amethyst and Topaz Dust. Its vertical anvil/lightning-rod/ingot pattern uses
Ironwood as the bottom ingot. Hardened Resin consumes four Resin per item; stamping one
Wood Fiber returns one Paper. Magnetoelectric Core uses Large Copper Slates and Mana
Insulating Glass. Lightning converts Nickel Blocks to Hollow Magnet Blocks through the
`bertieprogression:lightning_magnet_inputs` block tag, retaining the native strike area
and Topaz-triggered lightning.

Mechanical Drill is exclusive to a five-by-five Mechanical Crafter wall, using Tungsten
Nuggets, Malignant Pewter Nuggets, Andesite Alloy and Andesite Casings.

Royal Steel has one superheating alloy recipe: two Obsidiansteel Ingots, one Refined
Soulstone, one Emerald and one Refined Brilliance produce one Royal Steel Ingot. The
bonus and direct-block alloy recipes are disabled; ingot/block/nugget storage conversions
remain available.

Both Heliostat crafting patterns use four Large Silver Slates and return eight Heliostats.
The biological pattern uses a Sunflower; the electrical pattern uses a Circuit Board and
Daylight Detector. Each retains its Iron Block base.

Beef Mushroom Stew uses a Farmer's Delight Cooking Pot with beef, both mushrooms, and a bowl
in the container slot. It consumes no water. Spider-eye fermentation by bulging is disabled.

Sifting results roll independently, one item on each successful roll. Output order follows
descending probability; equal-probability outputs retain their specified order.

| Input | Changed outputs |
| --- | --- |
| Soul Sand | Nether Wart 1% |
| Gravel | Flint 20%, Iron Nugget 10%, Copper Nugget 2% |
| Quartz Sand | Quartz 15% |
| Nether Dust | Netherite Scrap 0.6%, Tungsten Nugget 0.2%; scrap precedes tungsten |
| Cinerite | Cinerite 50%, Gunpowder 20%, Zinc Nugget 15%, Tin Nugget 15%, Silver Nugget 5%, Lead Nugget 3%; no lapis |

Other existing sifting returns remain unchanged.

## Ores

Solar Core Ore uses the blackstone variant; its netherrack variant is withdrawn. Six-block
connected veins seed beneath lava pools in Nether Basalt Deltas above Y=32. The seed touches
lava above it; connected ore may extend into buried basalt or blackstone. Sixteen column
searches per chunk cover pool floors throughout the dimension's height. Every placed block
remains inside Basalt Deltas, and lava and bedrock are never replaced.

Tungsten generates only in Overworld deepslate between Y=-64 and Y=8. Its per-layer weight is
proportional to `Y + 96`, decreasing toward the floor while remaining positive there. Four
attempts per chunk use a mixture of two- and three-block veins, twice the native Ancient
Debris attempt rate. Bedrock is ineligible. Ore changes affect newly generated chunks.

## Nether materials

The Null Blaze Cube's five-by-five Nether crafting recipe uses Blazing Quartz, Stratine
Gems, Blaze Rods, Glowstone and Ancient Debris. Stratine Gems use Solar Core in the centre
and consume six Citrine Dust alongside their existing Onyx Dust.
Creative Blaze Cake uses a Tier-2 Hephaestus Forge: one Blaze Cake, four Blaze Cubes,
two Ignitium Ingots and two Fire Infused Netherite Ingots; 15000 Blood, 50 Souls and 1350 Ink.

Malignant Pewter includes two Tungsten Ingots as its second secondary ingredient and keeps
Onyx Dust last. It uses 20 Earthen and 16 Eldritch Spirits. The Seashelf of Aquatic Filtration
uses Large Prismarine Slates in place of prismarine blocks.

Lava Bottle and Bone Serpent Tooth brew three-minute Lava Vision. Redstone extends it to
eight minutes, and gunpowder makes either duration splashable.

Scale Ingot uses a 5×3 Nether Crafting Table recipe: twelve Ice and Fire dragon
scales around a middle row of three Eezo Ingots. Scale types and colours may mix.
One craft produces one ingot. All three Dragonsteel forge recipes consume Scale
Ingot instead of Iron Ingot and retain their blood, duration and output.
Its scales smoothly exchange their eight color palettes over a 24-second animation.

## Sheets and technology entry

Iron, Copper, Golden and Brass Sheets use Bertie Progression items instead of
Create's material sheets. Recipes, plate tags and quest objectives use the new
items. Royal Steel, Osmium, Deep Alloy, Obsidiansteel, Steel and Nickel also have Sheets.
Electrum provides Rod and Wire forms; its Sheet and Slates are withdrawn.
Sturdy Sheet and its incomplete assembly item retain their native items and art;
both Obsidian Dust deployment steps in its assembly now use Cognitive Flux.

Each small material slate contains four ingots. Create pressing, AnvilCraft
stamping and the Mekanism More Machines Stamper produce one Sheet from a small
slate. The Stamper uses a matching Sheet as its reusable mould. Copper Rod is a
dedicated item: one Copper Ingot makes one on the Lathe. Steel Rod has the same
Lathe yield. Electrum Rod uses the same 1:1 Lathe route. Three matching ingots
diagonally craft one Rod.

Copper and Electrum Wire use one matching Rod in a single Create sequence: press, press, saw, saw,
press. It produces one guaranteed Wire plus either another Wire (30%) or a
matching Ingot (70%). The Lathe's 1:1 yield prevents recycling that Ingot return
into surplus material. Rolling Mill consumes one matching Rod for four Wires.
EMI keeps the five-step assembly and shows its guaranteed output above the
secondary choice. Basic Rod recipes run bottom-left to top-right in the viewer.

Machine Casing retains `mekanism:steel_casing` as its ID. Its 5×5 Nether-table
recipe uses Royal Steel and Osmium Sheets, Runic Glass, Large Redstone Slates and
a central Bound Soul Ingot. The Metallurgic Infuser and Enrichment Chamber also
use 5×5 Nether-table recipes with Deep Alloy and Osmium Sheets, Cognitive Alloy
and the relevant circuits, alloys and machine components.
Both use Motor at row 4, column 2 and AnvilCraft Circuit Board at row 2, column 4.
Circuit Board, Oritech Magnetic Coil and Oritech Motor use 5×5 Nether-table
recipes. Bound Soul alternatives produce 32 Boards, 12 Coils or three Motors.
Assembler alternatives produce one Board, Coil or Motor; their Bound Soul
variants produce sixteen Boards, six Coils or two Motors. Electric Coil uses
two Electrum Wires, Cognitive Alloy and Magnetic Coil. Processing Unit uses
Adamant, Large Redstone Slate, Electric Coil and Plastic Sheet.

Formulaic Assemblicator, Oritech Assembler and Centrifuge use 5×5 Nether-table
recipes. Assembler consumes two Formulaic Assemblicators. Centrifuge uses three
Adamant Ingots across the bottom centre, four Deep Alloy Sheets on the lower
sides and six Copper Sheets above. Adamant crafts from four Diamonds and three
Platinum Ingots for two; Foundry combines Platinum and Diamond for one. Its
Create mixing and Metallurgic Infuser dust routes are disabled.

Ordinary raw metals enrich into two dust per raw item, or eighteen per raw
storage block. Purification produces three clumps per raw item, or twenty-seven
per raw storage block. Ore-block recipes retain their existing yields.

All Fluid Tank tiers use Steel Sheets top/bottom and Steel Rods left/right,
with their previous tier in the centre. Basic uses Create Fluid Tank in the
centre and Osmium Ingot corners. Chemical Tank upgrades use Osmium Sheets on
the four sides; Basic uses Steel Sheets and Basic Control Circuit corners.
Upgrade corners and stored-content transfer remain unchanged.

Pressurized Reaction Chamber uses the selected 5×5 Nether-table recipe with
one Advanced Chemical Tank, one Advanced Fluid Tank, two Motors and a central
Enrichment Chamber. Empty Capacitor is
made there from one Elite Chemical Tank, 5 B of ordinary Mana Potion and 2 B
Hydrogen over ten seconds. The potion fluid is Create's potion fluid carrying
Iron's Spellbooks Mana I; other potion contents do not satisfy the ingredient.

Purification Chamber and Electrolytic Separator use 5×5 Nether recipes. The
Separator contains a Motor rather than a tank. Pulveriser uses two Grindstones,
two Mechanical Saws and two Motors. Powered Furnace uses Industrial Glass and
three Magnetic Coils. Charger uses Glass, Copper Rods, Magnetoelectric Core and
Deep Alloy Sheets in an ordinary 3×3 craft. Its top row is Deep Alloy Sheet,
Glass, Deep Alloy Sheet.

Primitive Machine Core uses eight Andesite Casings around Bound Soul Ingot.
Basic Machine Core uses eight ordinary Steel Sheets around the Primitive Core.
All five basic Mekanism transmitters craft sixteen from six Steel Sheets and
three of their existing unique ingredient. Assembler alternatives use two Steel
Rods, one Steel Sheet and one unique ingredient for sixteen. Upgrades retain
their recipes. Copper Nuggets have one shaped ingot craft; the merged Small
Dust nugget-to-dust crafts are disabled for Copper, Gold, Iron, Nickel and Platinum.

Industrial Glass crafts one per batch. Foundry combines one full plating block
from `#oritech:plating` with one Glass for one Industrial Glass. Electrum Foundry
alloying uses Brass and Silver for two; crafting uses three Brass, three Redstone
and three Silver for four. Clibano combines Brass and Silver for one. Create
mixing is disabled; the separate Gold/Redstone infusion route remains available.

Basic, Lava and Bio Generators use 5×5 Nether recipes. Basic consumes one filled
Capacitor and one Machine Casing. Lava and Bio use five Copper Sheets across
the top, three Electrum Rods down each side and five Deep Alloy Sheets across
the bottom. Lava retains one Capacitor, Machine Casing and six Coils; Bio retains
Biopolymer, one Coil and one Basic Generator. Steam Engine uses Steel, Copper
and Deep Alloy Sheets, four filled Capacitors, three Coils, one Motor and one
Basic Generator. Steam Boiler Addon uses five Steel Sheets, Coal Dust, two
Adamant Ingots and any full plating block.

## Plastic processing

Polymer Resin's two fluid Centrifuge routes and manual craft use AnvilCraft
Resin instead of Sand. Smelting/blasting Polymer Resin produces one Raw
Biopolymer; Centrifuge processing produces two with Water or four with Mineral
Slurry. PRC's Water/Hydrogen Substrate route consumes two Raw Biopolymer and
retains its other amounts and outputs.

Two vertically stacked HDPE Pellets craft one HDPE Rod. Three vertically stacked
Rods craft one PlaStick; Lathe converts one Rod to one PlaStick. Four PlaSticks
in a shaped 2×2 craft produce one Oritech Plastic Sheet. Stamper converts one
PlaStick to one Plastic Sheet, or one Resin to one Hardened Resin; both retain
one matching output item as a reusable mould. Existing direct Raw Biopolymer
Centrifuge routes and Mekanism HDPE Sheet production remain available.

Ice & Fire Trolls cannot enter a server level, including previously saved mobs.
Their items are retired except Troll Banner Pattern, which crafts shapelessly
from Paper and Ancient Forgemastery Ravager Horn. Retained Troll Hide ingredients
and chest rewards use Quark Ravager Hide. Other Ice & Fire mobs remain available.

Charge Mix smelts 1:1 into Silicon in Powered Furnace and Energized Smelter,
including its Smelting Factories. The blasting route remains, and ordinary
vanilla Furnace processing remains unavailable. Altar of Amethyst also converts
one Enrichment Chamber into one Altar with a base duration of 72,000 ticks,
subject to the existing environmental speed rules.

Basic Control Circuits use Zinc infusion; the higher circuit recipes retain
their infusion materials. Zinc Ingot or Amethyst Shard supplies 10 chemical
units. Enriching one produces an enriched item supplying 80. Zinc uses the
pack's canonical Magitech ingot. Amethyst uses the selected edited item sprite.
The two infusion chemicals have animated icons for tanks and machine displays.
Infused Alloy uses Haze n Stuff's Rose Gold Ingot instead of Copper.

Obsidian blocks no longer crush into Obsidian Dust. Obsidiansteel Ingots produce
one dust in the Mekanism Crusher, Create Crushing Wheels, Oritech Pulverizer or
Fragment Forge. Amethyst infusion replaces Diamond in Refined Obsidian Dust.
The Obsidian Dust and Deepslate Combiner recipe is disabled. Other ingot-crushing
routes are disabled, including Oritech's corresponding Fragment Forge recipes.

Piezoelectric Crystal uses Copper Sheets, Glass, Quartz Blocks, Magnet Ingots
and Amethyst Blocks on the Nether table. Heater uses Terracotta, any log, Iron
Sheets, a Magnetoelectric Core and Deep Alloy Sheets. Charge Collector uses
Magnet Ingots and a bottom row of Deep Alloy Sheet / Large Copper Slate / Deep
Alloy Sheet. Transmission Pole uses a 3×5 Mechanical Crafter wall with a core,
Copper Sheets and Rods, Deep Alloy Sheets and a Deep Alloy Block.

Power Converters form a one-output upgrade chain: Small uses Magnetoelectric
Core over Copper Block; Medium and Large each add their predecessor between
those ingredients. Legacy combining and splitting recipes are disabled.
Mechanical Drill retains its ingredients and produces two.

Brilliant Obelisk infusion consumes eight Refined Brilliance instead of two Raw
Brilliance. Runewood Obelisk uses Hephaestus Forge II: one Brilliant Obelisk,
four Hallowed Gold Ingots, two Totemic Gold Ingots, Gold Horseshoes and Sugar;
15 Souls, 4000 Blood and 400 experience essence. Its old Spirit Altar route is
disabled. Overseer uses the selected 3×5 Mechanical Crafter recipe with Crying
Obsidian, Eyes of Ender, five Deep Alloy Sheets and a Brilliant Obelisk.

## Utility machine progression

Electric smelting starts with Oritech's Powered Furnace. Energized Smelter is
retained as the base for Mekanism and Mekanism Extras Smelting Factories, preserving
late-game bulk throughput. Its high-tier entry cost remains to be designed; the
native Smelter recipe is available meanwhile, and Basic Smelting Factory again
consumes the Smelter.

Ordinary Mekanism Heat Generator, Bio-Generator and Advanced Solar Generator are
retired. Oritech supplies the starter solid-fuel and biological generators.
Stellaris Solar Panel is retired.

Pumps, breakers and placers retain their recipes from before the proposed
machine-predecessor changes. Oritech -> Mekanism -> Extras pumps and the
four-stage breaker/placer paths remain deferred design ideas. The selected
Fluid/Chemical Tank recipes are described above.

Ordinary power, fluid and item transport uses Mekanism. Oritech's corresponding
pipes, framed/duct variants and Pipe Booster are retired, alongside its Portable
Tank and portable/large energy storage. Retained recipes use Mekanism equivalents;
Schrodinger's Safe uses a Basic Induction Cell. Oritech Superconductors and
Schrodinger's Safe remain available; Item Filter and Packed Wheat are withdrawn.

Refined Storage and Cable Tiers remain outside the proposed chains. The Devourer's
special terrain-drop reduction tag is empty, so it uses ordinary block loot without that
extra chance to discard drops. Its Dragon Head and Netherite Ingot costs remain.

Null Blaze Cube uses four Solar Cores in its former Magma Cream positions.
Nether Dust sifting yields Netherite Scrap at 0.1% and Redstone Dust at 8%; its
Nether Dust return and Tungsten Nugget chances retain their existing values.

Ore-processing machines and intermediates remain available. Their ore routes
remain, apart from the selected ingot-crushing and Obsidian Dust changes above.
Solar Flux Reborn panels and the large addon Heat Generator remain
available. Dedicated heaters supply evaporation heat; retained recipes that used
Advanced Solar Generator now use Oritech Big Solar Panel.
