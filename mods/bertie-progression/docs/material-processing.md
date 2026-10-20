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
python -m unittest discover -s mods/bertie-progression/texture-work -p test_recipe_cleanup.py
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
Gems, Blaze Rods, Glowstone and Ancient Debris. Stratine Gems use Solar Core in the centre.
Creative Blaze Cake uses a Tier-2 Hephaestus Forge: one Blaze Cake, four Blaze Cubes,
two Ignitium Ingots and two Fire Infused Netherite Ingots; 15000 Blood, 50 Souls and 1350 Ink.

Malignant Pewter includes two Tungsten Ingots as its second secondary ingredient and keeps
Onyx Dust last. It uses 20 Earthen and 16 Eldritch Spirits. The Seashelf of Aquatic Filtration
uses Large Prismarine Slates in place of prismarine blocks.

Lava Bottle and Bone Serpent Tooth brew three-minute Lava Vision. Redstone extends it to
eight minutes, and gunpowder makes either duration splashable.

Scale Ingot is a visual placeholder in the Progression creative tab, with no recipe or use.
