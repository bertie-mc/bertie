# Refined Storage progression

## Forge III and Machine Casing

Machine Casing (`refinedstorage:machine_casing`) is a Forge III ritual with
Primordial Assembly as its main input and twelve pedestal items: eight
Quartz-Enriched Iron, two Royal Steel Ingots and two Cognitive Alloy.
It consumes 3000 Aureal and 1000 Ink and produces one Casing. The existing
Forge III structure already provides the twelve required pedestal positions.
The original ordinary Casing recipe is disabled.

This Casing is a different block from Mekanism's Steel Casing.

Quartz-Enriched Iron and Copper use the ordinary 3×3 pattern below, with M
as the matching metal ingot and Q as Quartz. Five Ingots and four Quartz
produce one enriched Ingot. Their Assembler alternatives retain the native
three Ingots plus one Quartz → four enriched Ingots ratio, in 80 ticks.

```text
M Q M
Q M Q
M Q M
```

## Processors and cores

Raw Processor production uses unheated Create Mixing or Oritech Assembler.
Both retain Processor Binding, Silicon and one-item raw outputs. Mixing takes
100 ticks and the Assembler 80 ticks. Ordinary raw-to-finished smelting remains.

| Processor | Metal | Control Circuit |
|---|---|---|
| Basic | Steel | Basic |
| Improved | Electrum | Advanced |
| Advanced | Platinum | Elite |

Construction Core is Assembler-only: one Basic Processor, two Quartz and one
Diamond → one Core. Destruction Core is Assembler-only: one Basic Processor,
two Blazing Quartz and one Fluxite → one Core. Both take 80 ticks.

## Controller and Cable

Controller has one 5×5 Nether Crafting Table recipe, yielding one Controller.
Its former Forge III ritual is removed. Q is Quartz-Enriched Iron, R Reinforced
Alloy, P Advanced Processor, S Bound Soul Ingot, F Carbon Fibre Strands,
K Machine Casing and U Ultimate Control Circuit.

```text
Q R R R Q
R P S P R
R F K F R
R P U P R
Q R R R Q
```

Cable production uses only the 5×5 Mechanical Crafting wall, yielding sixteen
Cables. C is Quartz-Enriched Copper, A Andesite Alloy, G Moonstone Glass and
B Basic Control Circuit. The original ordinary production recipe is replaced.
Controller and Cable recoloring continue to consume existing devices.

```text
C C C C C
C A A A C
G G B G G
C A A A C
C C C C C
```

## Grid and peripheral recipes

Grid, Constructor, Destructor, External Storage, Exporter, Importer,
Autocrafter Manager and Autocrafting Monitor use Mechanical Crafting and
produce one device each. Their old ordinary production recipes are replaced;
recoloring continues to require existing devices.

Grid uses six Quartz-Enriched Iron, seven Royal Steel, three Onyx Glass,
three Cognitive Crystals, two Advanced Processors and one each of Construction
Core, Destruction Core, Processing Unit and Machine Casing. Manager retains
that outer layout, with two Autocrafters and a Circuit Board replacing the
cores and Processing Unit. Monitor instead uses two Improved Processors,
two Patterns and a Circuit Board in the corresponding slots.

Crafting Grid keeps its shapeless Grid/Advanced Processor upgrade, with one
Compressed Crafting Table replacing the generic crafting table. Pattern keeps
its ordinary shaped recipe, with Arcane Crystal Dust in all three former
Redstone positions.

Disk Housing keeps its ordinary shaped craft with Arcane Crystal Dust in all
three former Redstone positions. Storage Monitor also keeps its ordinary craft,
using Onyx Glass in the three right-column positions.

Disk Drive, Disk Interface, Network Transmitter and Network Receiver use full
5×5 Mechanical Crafting layouts and produce one device. Drive uses eleven
Quartz-Enriched Iron, eight Housings, three Advanced Processors, two Processing
Units and a Casing. Interface uses eleven Quartz-Enriched Iron, six Housings,
two Processing Units, two Onyx Glass, a Flux Gate, both Core types and a Casing.
Each network device uses twelve Quartz-Enriched Iron, four Adamant, two Flux
Gates, two Warp Flux, a Teleportation Core, an Absolute Control Circuit, both
Core types and a Casing. Receiver swaps the Transmitter's second and fourth
rows, retaining Construction Core left and Destruction Core right of Casing.

Portable Grid uses the Sculk Crafting Table: four corner Quartz-Enriched Iron,
Grid above Controller, Space Overcompressor below it and an Ultimate Control
Circuit on either side. Wrench uses a 4×4 Mechanical Crafting layout, consuming
six Quartz-Enriched Iron and three Advanced Processors for one Wrench.

The Nesting Shulker chain consumes two identical inputs at every tier: two
Shulker Boxes → Nesting, two Nesting → Over Nesting, two Over Nesting →
Supercritical. One Supercritical box therefore costs eight ordinary boxes.

## Quests

The post-Forge III quest retains its identity and position but now requests
Machine Casing. Controller is to its right and depends on Casing; Cable is
below Casing and also depends on Casing. Grid is above Controller, leading
to Crafting Grid and to Pattern → Pattern Grid.

External Storage sits below Cable. Importer → Exporter branches to its left,
and Constructor → Destructor to its right, with the first quest of each branch
depending on Cable. Disk Drive is below Controller and leads to Disk Interface.

Basic, Improved and Advanced Processor form an upward column, with a linked
64-item objective above-right of each. Construction and Destruction branch
left and above-left of Basic Processor; each has a linked 64-Core objective
above it. The stack objectives are side milestones, not prerequisites for
the next processor tier or Controller. All acquisition tasks are non-consuming.

Autocrafter follows Controller. Autocrafter Manager is below Pattern Grid and
depends on Autocrafter; Autocrafting Monitor is to its right and depends on it.

Elite → Ultra → Mega Autocrafters extend the base Autocrafter through Cable
Tiers, with all colour variants accepted per tier. Wrench branches from the
base Autocrafter. Disk Housing follows Disk Drive and leads to three parallel
part-capacity lines: items, fluids and chemicals. They include Extra Disks and
Mekanism Integration and end at Infinite. Advanced Storage Housing is a side
quest from Housing.

## Paired Dragonsteel collisions

The three additional Anvil Collision recipes require T1 eligibility and
32 m/t. Each consumes a matching Dragonsteel storage block and its immediately
underlying metal block, retaining the anvil and producing eight Dragonsteel
Ingots. Fire uses Ember Metal, Ice uses Frost Metal and Lightning uses
Lightning-Infused Netherite. Nearby blocks are not substituted for the support
or consumed by a transformation explosion.

These literal recipes spend a nine-Ingot Dragonsteel block to return eight
Ingots of the same element, plus the base block. They are not a multiplication
route. Existing Dragonforge production remains available.
