# Compact spaces and late storage

Compact Machines uses one `new_machine` item with a room-template component and
colour. Farming is withdrawn by component, while the other templates remain.
Soaryn keeps its native 45³ room; its recipe uses a 9³ structure. Saved template
identities remain registered. Stock Wall/machine crafts are filtered on recipe
reload so a built-in pack's saved priority cannot restore the old acquisition.

## Wall acquisition

The Giant Anvil matches a 5³ Polished Deepslate shell (98 blocks), with an inner
3³ Levitation Powder Block shell (26) and one Heavy Iron Block at the centre.
Cover it with 5×5 Crafting Tables and drop a Giant Anvil. Ordinary conversion
places one Wall in the centre. A Space Overcompressor in the cap's centre uses
the native item-crafting form instead. Both consume the same structure.

Miniaturization converts a hollow 3³ Polished Deepslate shell (26 blocks), with
one dropped Levitation Powder item, into four Walls. The centre stays Air.

## Machine acquisition

Each Miniaturization structure is a hollow Wall cube with one centre block and
one dropped item. Every other interior position is Air. Outputs retain the
native template component and colour; no new machine item IDs are registered.

| Room | Cube | Walls | Centre block | Dropped item |
| --- | --- | ---: | --- | --- |
| 3³ | 3³ | 26 | Heavy Iron | Steel Ingot |
| 5³ | 3³ | 26 | Mana Charged Fluorite | Arcane Ingot |
| 7³ | 5³ | 98 | Hallowed Gold | Bound Soul Ingot |
| 9³ | 5³ | 98 | Cognitive Crystal | Astute Assembly |
| 11³ | 7³ | 218 | Overheated Ember Metal | Arcane Infused Netherite Ingot |
| 13³ | 7³ | 218 | Witherite | Blaze Cube |
| 45³ | 9³ | 386 | Avaritia Neutronium | Charged Gravis Ingot |

Native Field Projector sizes already support 3³/5³/7³/9³. Recipes use the native
200-tick default. Owned EMI views list exact material counts, centre and catalyst
instructions and retain each output's components.

Device uses Sculk Table; Quantum Core and Field Projector use End Table; both
Atom Modules use Nether Table. Match/Rescan Proxies use ordinary crafting with a
Comparator/Observer above three Walls. Old Projector Dish/Base are withdrawn.

## Storage parts

All three part families use two alloys in the top corners, three finished
processors at top/bottom corners, a centre Control Circuit, and three preceding
parts at left/right/bottom. T1 instead uses its family material at centre/top
corners, Silicon at top/bottom corners and Glass at left/right/bottom. Family
materials are Quartz Enriched Iron, Quartz Enriched Copper and Osmium for Item,
Fluid and Chemical respectively. Exact frozen inputs are authored in
`texture-work/storage_part_layouts.json`.

Raw Improved/Advanced/Withering replace Silicon with the preceding raw processor;
other ingredients, stations and raw-to-finished routes stay unchanged. Housing
and part-to-disk recipes retain their previous curation.

AnvilCraft base/charged Neutronium items display Gravis/Charged Gravis. Untreated
Reinforced Carbon Sheet retains its item ID and uses twelve Diamonds in place of
Refined Obsidian; the 3 B Naphtha refining route remains.

All tiers of Powah Furnator, Magmator, Thermo Generator, Reactor, Energy Hopper,
Energy Discharger and Ender Cell are withdrawn. Other Powah content remains.
