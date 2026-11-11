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

Cable production uses only the 5×5 Mechanical Crafting wall, yielding twelve
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

## Quests

The post-Forge III quest retains its identity and position but now requests
Machine Casing. Controller is to its right and requires Casing and Advanced
Processor; Cable is below Casing and requires Casing.

Basic, Improved and Advanced Processor form an upward column, with a linked
64-item objective above-right of each. Construction and Destruction branch
left and above-left of Basic Processor; each has a linked 64-Core objective
above it. The stack objectives are side milestones, not prerequisites for
the next processor tier or Controller. All acquisition tasks are non-consuming.

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
